package com.insuretech.pms.lineage.config;

import com.insuretech.pms.lineage.service.OutboxPoller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes Redis Streams and consumer groups on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LineageStreamInitializer {

    private final OutboxPoller outboxPoller;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Initializing lineage event stream...");
        outboxPoller.initializeStream();
        log.info("Lineage event stream initialization complete");
    }
}
