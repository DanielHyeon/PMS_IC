-- Outbox Pattern Implementation for Event-Driven Lineage Tracking
-- Ensures atomic transaction between business logic and event publishing

-- ============================================
-- 1. Outbox Events Table
-- ============================================

CREATE TABLE IF NOT EXISTS project.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Event identification
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,

    -- Event payload (JSON)
    payload JSONB NOT NULL,

    -- Processing status
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,

    -- Retry tracking
    retry_count INTEGER DEFAULT 0,
    last_error TEXT,

    -- Idempotency key for consumers
    idempotency_key VARCHAR(100) UNIQUE
);

-- Indexes for efficient polling
CREATE INDEX IF NOT EXISTS idx_outbox_status_created
    ON project.outbox_events(status, created_at)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON project.outbox_events(aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_outbox_event_type
    ON project.outbox_events(event_type);

-- ============================================
-- 2. Comments for documentation
-- ============================================

COMMENT ON TABLE project.outbox_events IS
'Transactional Outbox for event-driven lineage synchronization
@pattern: Outbox Pattern - ensures at-least-once delivery
@consumers: Neo4j sync, OpenMetadata sync
@polling: OutboxPoller reads PENDING events and publishes to Redis Streams';

COMMENT ON COLUMN project.outbox_events.event_type IS
'Event type: REQUIREMENT_CREATED, REQUIREMENT_STORY_LINKED, STORY_TASK_LINKED, etc.';

COMMENT ON COLUMN project.outbox_events.aggregate_type IS
'Entity type: REQUIREMENT, USER_STORY, TASK, SPRINT';

COMMENT ON COLUMN project.outbox_events.aggregate_id IS
'Entity ID that triggered the event';

COMMENT ON COLUMN project.outbox_events.payload IS
'JSON payload containing event data for consumers';

COMMENT ON COLUMN project.outbox_events.idempotency_key IS
'Unique key for idempotent processing by consumers';


-- ============================================
-- 3. Function to generate idempotency key
-- ============================================

CREATE OR REPLACE FUNCTION project.generate_idempotency_key()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.idempotency_key IS NULL THEN
        NEW.idempotency_key := NEW.event_type || ':' || NEW.aggregate_id || ':' ||
                               EXTRACT(EPOCH FROM NEW.created_at)::TEXT;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_outbox_idempotency_key
    BEFORE INSERT ON project.outbox_events
    FOR EACH ROW
    EXECUTE FUNCTION project.generate_idempotency_key();


-- ============================================
-- 4. Cleanup function for old published events
-- ============================================

CREATE OR REPLACE FUNCTION project.cleanup_old_outbox_events(days_to_keep INTEGER DEFAULT 7)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM project.outbox_events
    WHERE status = 'PUBLISHED'
      AND published_at < NOW() - (days_to_keep || ' days')::INTERVAL;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION project.cleanup_old_outbox_events IS
'Removes published events older than specified days. Default: 7 days.
Usage: SELECT project.cleanup_old_outbox_events(14);';
