package com.insuretech.pms.lineage.repository;

import com.insuretech.pms.lineage.entity.LineageEventType;
import com.insuretech.pms.lineage.entity.OutboxEvent;
import com.insuretech.pms.lineage.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find pending events ordered by creation time (FIFO)
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * Find pending events with limit for batch processing
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findPendingEventsWithLimit(
            @Param("status") OutboxStatus status,
            @Param("limit") int limit);

    /**
     * Find failed events that can be retried
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findRetryableEvents(@Param("maxRetries") int maxRetries);

    /**
     * Find events by aggregate for debugging/tracing
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
            String aggregateType, String aggregateId);

    /**
     * Find events by type for monitoring
     */
    List<OutboxEvent> findByEventTypeOrderByCreatedAtDesc(LineageEventType eventType);

    /**
     * Count pending events (for monitoring/alerting)
     */
    long countByStatus(OutboxStatus status);

    /**
     * Delete old published events for cleanup
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoff")
    int deletePublishedEventsBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Check if idempotency key exists (for duplicate prevention)
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
