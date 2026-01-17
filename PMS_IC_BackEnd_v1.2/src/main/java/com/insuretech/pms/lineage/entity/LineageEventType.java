package com.insuretech.pms.lineage.entity;

/**
 * Types of lineage events for requirement traceability.
 * These events drive Neo4j and OpenMetadata synchronization.
 */
public enum LineageEventType {

    // ===== Requirement Events =====
    /**
     * New requirement created from RFP extraction
     */
    REQUIREMENT_CREATED,

    /**
     * Requirement details updated
     */
    REQUIREMENT_UPDATED,

    /**
     * Requirement deleted or archived
     */
    REQUIREMENT_DELETED,

    /**
     * Requirement status changed (IDENTIFIED -> ANALYZED -> APPROVED, etc.)
     */
    REQUIREMENT_STATUS_CHANGED,

    // ===== UserStory Events =====
    /**
     * New user story created
     */
    STORY_CREATED,

    /**
     * User story updated
     */
    STORY_UPDATED,

    /**
     * User story deleted
     */
    STORY_DELETED,

    /**
     * User story assigned to sprint
     */
    STORY_SPRINT_ASSIGNED,

    // ===== Task Events =====
    /**
     * New task created
     */
    TASK_CREATED,

    /**
     * Task updated
     */
    TASK_UPDATED,

    /**
     * Task deleted
     */
    TASK_DELETED,

    /**
     * Task status changed (TODO -> IN_PROGRESS -> DONE)
     */
    TASK_STATUS_CHANGED,

    // ===== Lineage Relationship Events =====
    /**
     * Requirement linked to UserStory (DERIVES relationship)
     */
    REQUIREMENT_STORY_LINKED,

    /**
     * Requirement unlinked from UserStory
     */
    REQUIREMENT_STORY_UNLINKED,

    /**
     * UserStory linked to Task (BREAKS_DOWN_TO relationship)
     */
    STORY_TASK_LINKED,

    /**
     * UserStory unlinked from Task
     */
    STORY_TASK_UNLINKED,

    /**
     * Requirement directly linked to Task (legacy/shortcut)
     */
    REQUIREMENT_TASK_LINKED,

    /**
     * Requirement unlinked from Task
     */
    REQUIREMENT_TASK_UNLINKED,

    // ===== Sprint Events =====
    /**
     * Sprint created
     */
    SPRINT_CREATED,

    /**
     * Sprint started
     */
    SPRINT_STARTED,

    /**
     * Sprint completed
     */
    SPRINT_COMPLETED;

    /**
     * Returns the aggregate type for this event (for grouping/routing)
     */
    public String getAggregateType() {
        if (name().startsWith("REQUIREMENT")) {
            return "REQUIREMENT";
        } else if (name().startsWith("STORY")) {
            return "USER_STORY";
        } else if (name().startsWith("TASK")) {
            return "TASK";
        } else if (name().startsWith("SPRINT")) {
            return "SPRINT";
        }
        return "UNKNOWN";
    }

    /**
     * Check if this event creates a lineage edge
     */
    public boolean isLineageEvent() {
        return name().contains("LINKED") || name().contains("UNLINKED");
    }

    /**
     * Check if this is a creation event
     */
    public boolean isCreationEvent() {
        return name().endsWith("_CREATED");
    }

    /**
     * Check if this is a deletion event
     */
    public boolean isDeletionEvent() {
        return name().endsWith("_DELETED");
    }
}
