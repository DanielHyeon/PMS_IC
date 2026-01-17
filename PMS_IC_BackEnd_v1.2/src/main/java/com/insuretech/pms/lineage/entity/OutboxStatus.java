package com.insuretech.pms.lineage.entity;

/**
 * Status of an outbox event in the publishing lifecycle.
 */
public enum OutboxStatus {
    /**
     * Event is waiting to be published to Redis Streams
     */
    PENDING,

    /**
     * Event has been successfully published
     */
    PUBLISHED,

    /**
     * Event publishing failed after retries
     */
    FAILED
}
