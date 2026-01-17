package com.insuretech.pms.lineage.service;

import com.insuretech.pms.lineage.entity.LineageEventType;
import com.insuretech.pms.lineage.entity.OutboxEvent;
import com.insuretech.pms.lineage.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing lineage events to the outbox table.
 * Events are stored in the same transaction as business logic,
 * ensuring atomic operation between data changes and event creation.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * @Transactional
 * public void linkRequirementToStory(String reqId, String storyId) {
 *     // Business logic
 *     requirement.linkStory(storyId);
 *     requirementRepo.save(requirement);
 *
 *     // Event in same transaction
 *     lineageEventProducer.publishRequirementStoryLinked(reqId, storyId, projectId);
 * }
 * }
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineageEventProducer {

    private final OutboxEventRepository outboxEventRepository;

    // ===== Requirement Events =====

    public void publishRequirementCreated(String requirementId, String projectId, String code, String title) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", requirementId);
        payload.put("projectId", projectId);
        payload.put("code", code);
        payload.put("title", title);

        saveEvent(LineageEventType.REQUIREMENT_CREATED, "REQUIREMENT", requirementId, payload);
    }

    public void publishRequirementUpdated(String requirementId, String projectId, Map<String, Object> changes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", requirementId);
        payload.put("projectId", projectId);
        payload.put("changes", changes);

        saveEvent(LineageEventType.REQUIREMENT_UPDATED, "REQUIREMENT", requirementId, payload);
    }

    public void publishRequirementDeleted(String requirementId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", requirementId);
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.REQUIREMENT_DELETED, "REQUIREMENT", requirementId, payload);
    }

    public void publishRequirementStatusChanged(String requirementId, String projectId,
                                                 String oldStatus, String newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", requirementId);
        payload.put("projectId", projectId);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);

        saveEvent(LineageEventType.REQUIREMENT_STATUS_CHANGED, "REQUIREMENT", requirementId, payload);
    }

    // ===== UserStory Events =====

    public void publishStoryCreated(String storyId, String projectId, String title, Integer storyPoints) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("storyId", storyId);
        payload.put("projectId", projectId);
        payload.put("title", title);
        payload.put("storyPoints", storyPoints);

        saveEvent(LineageEventType.STORY_CREATED, "USER_STORY", storyId, payload);
    }

    public void publishStoryUpdated(String storyId, String projectId, Map<String, Object> changes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("storyId", storyId);
        payload.put("projectId", projectId);
        payload.put("changes", changes);

        saveEvent(LineageEventType.STORY_UPDATED, "USER_STORY", storyId, payload);
    }

    public void publishStoryDeleted(String storyId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("storyId", storyId);
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.STORY_DELETED, "USER_STORY", storyId, payload);
    }

    public void publishStorySprintAssigned(String storyId, String sprintId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("storyId", storyId);
        payload.put("sprintId", sprintId);
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.STORY_SPRINT_ASSIGNED, "USER_STORY", storyId, payload);
    }

    // ===== Task Events =====

    public void publishTaskCreated(String taskId, String projectId, String title) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("projectId", projectId);
        payload.put("title", title);

        saveEvent(LineageEventType.TASK_CREATED, "TASK", taskId, payload);
    }

    public void publishTaskUpdated(String taskId, String projectId, Map<String, Object> changes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("projectId", projectId);
        payload.put("changes", changes);

        saveEvent(LineageEventType.TASK_UPDATED, "TASK", taskId, payload);
    }

    public void publishTaskDeleted(String taskId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.TASK_DELETED, "TASK", taskId, payload);
    }

    public void publishTaskStatusChanged(String taskId, String projectId,
                                          String oldStatus, String newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("projectId", projectId);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);

        saveEvent(LineageEventType.TASK_STATUS_CHANGED, "TASK", taskId, payload);
    }

    // ===== Lineage Relationship Events =====

    /**
     * Publish event when a Requirement is linked to a UserStory.
     * Creates DERIVES relationship in Neo4j: (Requirement)-[:DERIVES]->(UserStory)
     */
    public void publishRequirementStoryLinked(String requirementId, String storyId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", requirementId);
        payload.put("sourceType", "REQUIREMENT");
        payload.put("targetId", storyId);
        payload.put("targetType", "USER_STORY");
        payload.put("relationshipType", "DERIVES");
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.REQUIREMENT_STORY_LINKED, "REQUIREMENT", requirementId, payload);
    }

    public void publishRequirementStoryUnlinked(String requirementId, String storyId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", requirementId);
        payload.put("sourceType", "REQUIREMENT");
        payload.put("targetId", storyId);
        payload.put("targetType", "USER_STORY");
        payload.put("relationshipType", "DERIVES");
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.REQUIREMENT_STORY_UNLINKED, "REQUIREMENT", requirementId, payload);
    }

    /**
     * Publish event when a UserStory is linked to a Task.
     * Creates BREAKS_DOWN_TO relationship: (UserStory)-[:BREAKS_DOWN_TO]->(Task)
     */
    public void publishStoryTaskLinked(String storyId, String taskId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", storyId);
        payload.put("sourceType", "USER_STORY");
        payload.put("targetId", taskId);
        payload.put("targetType", "TASK");
        payload.put("relationshipType", "BREAKS_DOWN_TO");
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.STORY_TASK_LINKED, "USER_STORY", storyId, payload);
    }

    public void publishStoryTaskUnlinked(String storyId, String taskId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", storyId);
        payload.put("sourceType", "USER_STORY");
        payload.put("targetId", taskId);
        payload.put("targetType", "TASK");
        payload.put("relationshipType", "BREAKS_DOWN_TO");
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.STORY_TASK_UNLINKED, "USER_STORY", storyId, payload);
    }

    /**
     * Publish event when a Requirement is directly linked to a Task (legacy/shortcut).
     */
    public void publishRequirementTaskLinked(String requirementId, String taskId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", requirementId);
        payload.put("sourceType", "REQUIREMENT");
        payload.put("targetId", taskId);
        payload.put("targetType", "TASK");
        payload.put("relationshipType", "IMPLEMENTED_BY");
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.REQUIREMENT_TASK_LINKED, "REQUIREMENT", requirementId, payload);
    }

    public void publishRequirementTaskUnlinked(String requirementId, String taskId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", requirementId);
        payload.put("sourceType", "REQUIREMENT");
        payload.put("targetId", taskId);
        payload.put("targetType", "TASK");
        payload.put("relationshipType", "IMPLEMENTED_BY");
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.REQUIREMENT_TASK_UNLINKED, "REQUIREMENT", requirementId, payload);
    }

    // ===== Sprint Events =====

    public void publishSprintCreated(String sprintId, String projectId, String name) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sprintId", sprintId);
        payload.put("projectId", projectId);
        payload.put("name", name);

        saveEvent(LineageEventType.SPRINT_CREATED, "SPRINT", sprintId, payload);
    }

    public void publishSprintStarted(String sprintId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sprintId", sprintId);
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.SPRINT_STARTED, "SPRINT", sprintId, payload);
    }

    public void publishSprintCompleted(String sprintId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sprintId", sprintId);
        payload.put("projectId", projectId);

        saveEvent(LineageEventType.SPRINT_COMPLETED, "SPRINT", sprintId, payload);
    }

    // ===== Internal Helper =====

    private void saveEvent(LineageEventType eventType, String aggregateType,
                           String aggregateId, Map<String, Object> payload) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .build();

        outboxEventRepository.save(event);
        log.debug("Outbox event saved: {} for {}:{}", eventType, aggregateType, aggregateId);
    }
}
