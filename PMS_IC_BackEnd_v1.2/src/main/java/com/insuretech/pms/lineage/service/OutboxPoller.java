package com.insuretech.pms.lineage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insuretech.pms.lineage.entity.OutboxEvent;
import com.insuretech.pms.lineage.entity.OutboxStatus;
import com.insuretech.pms.lineage.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Polls the outbox table and publishes events to Redis Streams.
 * This component bridges the transactional outbox pattern with Redis Streams
 * for event-driven lineage synchronization.
 *
 * <p>Stream structure:</p>
 * <ul>
 *   <li>lineage:events - Main stream for all lineage events</li>
 *   <li>Consumer groups: neo4j-sync, openmetadata-sync</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${lineage.outbox.batch-size:100}")
    private int batchSize;

    @Value("${lineage.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${lineage.stream.name:lineage:events}")
    private String streamName;

    /**
     * Poll pending events and publish to Redis Streams.
     * Runs every 5 seconds by default.
     */
    @Scheduled(fixedDelayString = "${lineage.outbox.poll-interval:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findPendingEventsWithLimit(OutboxStatus.PENDING, batchSize);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                publishToRedisStream(event);
                event.markPublished();
                outboxEventRepository.save(event);
                log.debug("Published event {} to stream", event.getId());
            } catch (Exception e) {
                handlePublishError(event, e);
            }
        }
    }

    /**
     * Retry failed events that haven't exceeded max retries.
     * Runs every minute.
     */
    @Scheduled(fixedDelayString = "${lineage.outbox.retry-interval:60000}")
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findRetryableEvents(maxRetries);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed outbox events", failedEvents.size());

        for (OutboxEvent event : failedEvents) {
            event.resetForRetry();
            outboxEventRepository.save(event);
        }
    }

    /**
     * Cleanup old published events.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "${lineage.outbox.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = outboxEventRepository.deletePublishedEventsBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old published outbox events", deleted);
        }
    }

    private void publishToRedisStream(OutboxEvent event) throws JsonProcessingException {
        Map<String, String> streamMessage = new HashMap<>();
        streamMessage.put("id", event.getId().toString());
        streamMessage.put("eventType", event.getEventType().name());
        streamMessage.put("aggregateType", event.getAggregateType());
        streamMessage.put("aggregateId", event.getAggregateId());
        streamMessage.put("payload", objectMapper.writeValueAsString(event.getPayload()));
        streamMessage.put("idempotencyKey", event.getIdempotencyKey());
        streamMessage.put("createdAt", event.getCreatedAt().toString());

        ObjectRecord<String, Map<String, String>> record = StreamRecords
                .newRecord()
                .in(streamName)
                .ofObject(streamMessage);

        redisTemplate.opsForStream().add(record);
    }

    private void handlePublishError(OutboxEvent event, Exception e) {
        log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
        event.markFailed(e.getMessage());
        outboxEventRepository.save(event);

        if (event.getRetryCount() >= maxRetries) {
            log.error("Event {} exceeded max retries ({}), marking as permanently failed",
                    event.getId(), maxRetries);
        }
    }

    /**
     * Initialize Redis Stream and consumer groups on startup.
     * Called by LineageStreamInitializer.
     */
    public void initializeStream() {
        try {
            // Create stream if not exists by adding a dummy message and trimming
            Boolean exists = redisTemplate.hasKey(streamName);
            if (Boolean.FALSE.equals(exists)) {
                Map<String, String> initMessage = new HashMap<>();
                initMessage.put("init", "true");

                ObjectRecord<String, Map<String, String>> record = StreamRecords
                        .newRecord()
                        .in(streamName)
                        .ofObject(initMessage);

                redisTemplate.opsForStream().add(record);
                log.info("Created Redis Stream: {}", streamName);
            }

            // Create consumer groups
            createConsumerGroupIfNotExists("neo4j-sync");
            createConsumerGroupIfNotExists("openmetadata-sync");

        } catch (Exception e) {
            log.warn("Stream initialization warning: {}", e.getMessage());
        }
    }

    private void createConsumerGroupIfNotExists(String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(streamName, groupName);
            log.info("Created consumer group: {}", groupName);
        } catch (Exception e) {
            // Group already exists - this is expected on restart
            log.debug("Consumer group {} already exists", groupName);
        }
    }
}
