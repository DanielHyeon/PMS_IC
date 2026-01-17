package com.insuretech.pms.lineage.service;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import com.insuretech.pms.lineage.dto.*;
import com.insuretech.pms.lineage.entity.LineageEventType;
import com.insuretech.pms.lineage.entity.OutboxEvent;
import com.insuretech.pms.lineage.repository.OutboxEventRepository;
import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.repository.RequirementRepository;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.entity.UserStory;
import com.insuretech.pms.task.repository.SprintRepository;
import com.insuretech.pms.task.repository.TaskRepository;
import com.insuretech.pms.task.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying lineage data from PostgreSQL and constructing graph visualizations.
 * Uses OutboxEvent for timeline and domain entities for graph construction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LineageQueryService {

    private final OutboxEventRepository outboxEventRepository;
    private final RequirementRepository requirementRepository;
    private final UserStoryRepository userStoryRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;

    /**
     * Get the complete lineage graph for a project.
     * Constructs nodes from Requirements, UserStories, Tasks, and Sprints.
     * Constructs edges from linkedTaskIds and sprint assignments.
     */
    public LineageGraphDto getProjectGraph(String projectId) {
        log.debug("Building lineage graph for project: {}", projectId);

        List<LineageNodeDto> nodes = new ArrayList<>();
        List<LineageEdgeDto> edges = new ArrayList<>();

        // Load all entities for the project
        List<Requirement> requirements = requirementRepository.findByProjectIdOrderByCodeAsc(projectId);
        List<UserStory> stories = userStoryRepository.findByProjectIdOrderByPriorityOrderAsc(projectId);
        List<Task> tasks = loadTasksForProject(projectId);
        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);

        // Build nodes
        requirements.forEach(req -> nodes.add(toNode(req)));
        stories.forEach(story -> nodes.add(toNode(story)));
        tasks.forEach(task -> nodes.add(toNode(task)));
        sprints.forEach(sprint -> nodes.add(toNode(sprint)));

        // Build edges from Requirement -> Task links
        for (Requirement req : requirements) {
            for (String taskId : req.getLinkedTaskIds()) {
                edges.add(LineageEdgeDto.builder()
                        .id(req.getId() + "-" + taskId)
                        .source(req.getId())
                        .target(taskId)
                        .relationship(LineageEdgeDto.LineageRelationship.IMPLEMENTED_BY)
                        .createdAt(req.getUpdatedAt())
                        .build());
            }
        }

        // Build edges from UserStory -> Sprint assignments
        for (UserStory story : stories) {
            if (story.getSprint() != null) {
                edges.add(LineageEdgeDto.builder()
                        .id(story.getId() + "-" + story.getSprint().getId())
                        .source(story.getId())
                        .target(story.getSprint().getId())
                        .relationship(LineageEdgeDto.LineageRelationship.BELONGS_TO_SPRINT)
                        .createdAt(story.getUpdatedAt())
                        .build());
            }
        }

        // Calculate statistics
        LineageGraphDto.LineageStatisticsDto stats = calculateStatistics(
                requirements, stories, tasks, sprints);

        return LineageGraphDto.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(stats)
                .build();
    }

    /**
     * Get activity timeline with pagination and filtering.
     */
    public Page<LineageEventDto> getTimeline(
            String projectId,
            String aggregateType,
            LocalDateTime since,
            LocalDateTime until,
            String userId,
            Pageable pageable) {

        log.debug("Fetching timeline for project: {}, type: {}, since: {}, until: {}",
                projectId, aggregateType, since, until);

        // Get all events and filter (for MVP; production should use custom query)
        List<OutboxEvent> allEvents = outboxEventRepository.findAll();

        List<OutboxEvent> filtered = allEvents.stream()
                .filter(e -> matchesProject(e, projectId))
                .filter(e -> aggregateType == null || e.getAggregateType().equals(aggregateType))
                .filter(e -> since == null || e.getCreatedAt().isAfter(since))
                .filter(e -> until == null || e.getCreatedAt().isBefore(until))
                .filter(e -> userId == null || matchesUser(e, userId))
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt).reversed())
                .collect(Collectors.toList());

        // Map users for actor names
        Map<String, String> userNames = loadUserNames();

        // Convert to DTOs with pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<LineageEventDto> pageContent = filtered.subList(start, end).stream()
                .map(e -> toEventDto(e, userNames))
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * Get history for a specific entity.
     */
    public List<LineageEventDto> getEntityHistory(String aggregateType, String aggregateId) {
        log.debug("Fetching history for {}: {}", aggregateType, aggregateId);

        List<OutboxEvent> events = outboxEventRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(aggregateType, aggregateId);

        Map<String, String> userNames = loadUserNames();

        return events.stream()
                .map(e -> toEventDto(e, userNames))
                .collect(Collectors.toList());
    }

    /**
     * Get upstream dependencies (what leads to this entity).
     */
    public LineageTreeDto getUpstream(String aggregateType, String aggregateId, int depth) {
        log.debug("Fetching upstream for {}: {} with depth {}", aggregateType, aggregateId, depth);

        List<LineageNodeDto> nodes = new ArrayList<>();
        List<LineageEdgeDto> edges = new ArrayList<>();

        // Find the root entity
        LineageNodeDto root = findNode(aggregateType, aggregateId);
        if (root == null) {
            return LineageTreeDto.builder()
                    .nodes(Collections.emptyList())
                    .edges(Collections.emptyList())
                    .maxDepth(0)
                    .totalNodes(0)
                    .build();
        }

        nodes.add(root);

        // For Task, find linked Requirements
        if ("TASK".equals(aggregateType)) {
            List<Requirement> linkedReqs = requirementRepository.findAll().stream()
                    .filter(r -> r.getLinkedTaskIds().contains(aggregateId))
                    .collect(Collectors.toList());

            for (Requirement req : linkedReqs) {
                nodes.add(toNode(req));
                edges.add(LineageEdgeDto.builder()
                        .id(req.getId() + "-" + aggregateId)
                        .source(req.getId())
                        .target(aggregateId)
                        .relationship(LineageEdgeDto.LineageRelationship.IMPLEMENTED_BY)
                        .build());
            }
        }

        return LineageTreeDto.builder()
                .root(root)
                .nodes(nodes)
                .edges(edges)
                .maxDepth(depth)
                .totalNodes(nodes.size())
                .build();
    }

    /**
     * Get downstream dependencies (what depends on this entity).
     */
    public LineageTreeDto getDownstream(String aggregateType, String aggregateId, int depth) {
        log.debug("Fetching downstream for {}: {} with depth {}", aggregateType, aggregateId, depth);

        List<LineageNodeDto> nodes = new ArrayList<>();
        List<LineageEdgeDto> edges = new ArrayList<>();

        LineageNodeDto root = findNode(aggregateType, aggregateId);
        if (root == null) {
            return LineageTreeDto.builder()
                    .nodes(Collections.emptyList())
                    .edges(Collections.emptyList())
                    .maxDepth(0)
                    .totalNodes(0)
                    .build();
        }

        nodes.add(root);

        // For Requirement, find linked Tasks
        if ("REQUIREMENT".equals(aggregateType)) {
            Requirement req = requirementRepository.findById(aggregateId).orElse(null);
            if (req != null) {
                for (String taskId : req.getLinkedTaskIds()) {
                    taskRepository.findById(taskId).ifPresent(task -> {
                        nodes.add(toNode(task));
                        edges.add(LineageEdgeDto.builder()
                                .id(aggregateId + "-" + taskId)
                                .source(aggregateId)
                                .target(taskId)
                                .relationship(LineageEdgeDto.LineageRelationship.IMPLEMENTED_BY)
                                .build());
                    });
                }
            }
        }

        return LineageTreeDto.builder()
                .root(root)
                .nodes(nodes)
                .edges(edges)
                .maxDepth(depth)
                .totalNodes(nodes.size())
                .build();
    }

    /**
     * Analyze impact of changing an entity.
     */
    public ImpactAnalysisDto analyzeImpact(String aggregateType, String aggregateId) {
        log.debug("Analyzing impact for {}: {}", aggregateType, aggregateId);

        LineageNodeDto source = findNode(aggregateType, aggregateId);
        if (source == null) {
            return ImpactAnalysisDto.builder()
                    .sourceId(aggregateId)
                    .sourceType(aggregateType)
                    .impactedStories(0)
                    .impactedTasks(0)
                    .impactedSprints(0)
                    .directImpacts(Collections.emptyList())
                    .indirectImpacts(Collections.emptyList())
                    .affectedSprintNames(Collections.emptyList())
                    .build();
        }

        List<ImpactAnalysisDto.ImpactedEntityDto> directImpacts = new ArrayList<>();
        List<ImpactAnalysisDto.ImpactedEntityDto> indirectImpacts = new ArrayList<>();
        Set<String> affectedSprintIds = new HashSet<>();

        if ("REQUIREMENT".equals(aggregateType)) {
            Requirement req = requirementRepository.findById(aggregateId).orElse(null);
            if (req != null) {
                // Direct impacts: linked tasks
                for (String taskId : req.getLinkedTaskIds()) {
                    taskRepository.findById(taskId).ifPresent(task -> {
                        directImpacts.add(ImpactAnalysisDto.ImpactedEntityDto.builder()
                                .id(task.getId())
                                .type("TASK")
                                .title(task.getTitle())
                                .status(task.getStatus().name())
                                .impactLevel(ImpactAnalysisDto.ImpactLevel.DIRECT)
                                .depth(1)
                                .build());
                    });
                }
            }
        }

        // Get affected sprint names
        List<String> sprintNames = affectedSprintIds.stream()
                .map(id -> sprintRepository.findById(id).map(Sprint::getName).orElse("Unknown"))
                .collect(Collectors.toList());

        return ImpactAnalysisDto.builder()
                .sourceId(aggregateId)
                .sourceType(aggregateType)
                .sourceTitle(source.getTitle())
                .impactedStories(0)
                .impactedTasks(directImpacts.size())
                .impactedSprints(affectedSprintIds.size())
                .directImpacts(directImpacts)
                .indirectImpacts(indirectImpacts)
                .affectedSprintNames(sprintNames)
                .build();
    }

    /**
     * Get lineage statistics for a project.
     */
    public LineageGraphDto.LineageStatisticsDto getStatistics(String projectId) {
        List<Requirement> requirements = requirementRepository.findByProjectIdOrderByCodeAsc(projectId);
        List<UserStory> stories = userStoryRepository.findByProjectIdOrderByPriorityOrderAsc(projectId);
        List<Task> tasks = loadTasksForProject(projectId);
        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);

        return calculateStatistics(requirements, stories, tasks, sprints);
    }

    // ===== Helper Methods =====

    private List<Task> loadTasksForProject(String projectId) {
        // Tasks are loaded via KanbanColumn which belongs to a project
        // For simplicity, load all tasks (production should use proper query)
        return taskRepository.findAll();
    }

    private LineageNodeDto toNode(Requirement req) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", req.getCategory() != null ? req.getCategory().name() : null);
        metadata.put("priority", req.getPriority() != null ? req.getPriority().name() : null);
        metadata.put("progress", req.getProgress());
        metadata.put("linkedTaskCount", req.getLinkedTaskIds().size());

        return LineageNodeDto.builder()
                .id(req.getId())
                .type(LineageNodeDto.LineageNodeType.REQUIREMENT)
                .code(req.getCode())
                .title(req.getTitle())
                .status(req.getStatus() != null ? req.getStatus().name() : null)
                .metadata(metadata)
                .build();
    }

    private LineageNodeDto toNode(UserStory story) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("storyPoints", story.getStoryPoints());
        metadata.put("priority", story.getPriority() != null ? story.getPriority().name() : null);
        metadata.put("epic", story.getEpic());
        metadata.put("sprintId", story.getSprint() != null ? story.getSprint().getId() : null);

        return LineageNodeDto.builder()
                .id(story.getId())
                .type(LineageNodeDto.LineageNodeType.USER_STORY)
                .title(story.getTitle())
                .status(story.getStatus() != null ? story.getStatus().name() : null)
                .metadata(metadata)
                .build();
    }

    private LineageNodeDto toNode(Task task) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("priority", task.getPriority() != null ? task.getPriority().name() : null);
        metadata.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : null);
        metadata.put("trackType", task.getTrackType() != null ? task.getTrackType().name() : null);

        return LineageNodeDto.builder()
                .id(task.getId())
                .type(LineageNodeDto.LineageNodeType.TASK)
                .title(task.getTitle())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .metadata(metadata)
                .build();
    }

    private LineageNodeDto toNode(Sprint sprint) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("goal", sprint.getGoal());
        metadata.put("startDate", sprint.getStartDate() != null ? sprint.getStartDate().toString() : null);
        metadata.put("endDate", sprint.getEndDate() != null ? sprint.getEndDate().toString() : null);

        return LineageNodeDto.builder()
                .id(sprint.getId())
                .type(LineageNodeDto.LineageNodeType.SPRINT)
                .title(sprint.getName())
                .status(sprint.getStatus() != null ? sprint.getStatus().name() : null)
                .metadata(metadata)
                .build();
    }

    private LineageNodeDto findNode(String aggregateType, String aggregateId) {
        return switch (aggregateType) {
            case "REQUIREMENT" -> requirementRepository.findById(aggregateId)
                    .map(this::toNode).orElse(null);
            case "USER_STORY" -> userStoryRepository.findById(aggregateId)
                    .map(this::toNode).orElse(null);
            case "TASK" -> taskRepository.findById(aggregateId)
                    .map(this::toNode).orElse(null);
            case "SPRINT" -> sprintRepository.findById(aggregateId)
                    .map(this::toNode).orElse(null);
            default -> null;
        };
    }

    private LineageGraphDto.LineageStatisticsDto calculateStatistics(
            List<Requirement> requirements,
            List<UserStory> stories,
            List<Task> tasks,
            List<Sprint> sprints) {

        int linkedReqs = (int) requirements.stream()
                .filter(r -> !r.getLinkedTaskIds().isEmpty())
                .count();

        double coverage = requirements.isEmpty() ? 0.0 :
                (double) linkedReqs / requirements.size() * 100;

        return LineageGraphDto.LineageStatisticsDto.builder()
                .requirements(requirements.size())
                .stories(stories.size())
                .tasks(tasks.size())
                .sprints(sprints.size())
                .linkedRequirements(linkedReqs)
                .unlinkedRequirements(requirements.size() - linkedReqs)
                .coverage(Math.round(coverage * 100.0) / 100.0)
                .build();
    }

    private boolean matchesProject(OutboxEvent event, String projectId) {
        if (projectId == null) {
            return true;
        }
        Map<String, Object> payload = event.getPayload();
        return payload != null && projectId.equals(payload.get("projectId"));
    }

    private boolean matchesUser(OutboxEvent event, String userId) {
        if (userId == null) {
            return true;
        }
        Map<String, Object> payload = event.getPayload();
        if (payload == null) {
            return false;
        }
        return userId.equals(payload.get("userId")) || userId.equals(payload.get("actorId"));
    }

    private Map<String, String> loadUserNames() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }

    private LineageEventDto toEventDto(OutboxEvent event, Map<String, String> userNames) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null) {
            payload = Collections.emptyMap();
        }

        String actorId = (String) payload.getOrDefault("userId",
                payload.getOrDefault("actorId", ""));
        String actorName = userNames.getOrDefault(actorId, "System");

        return LineageEventDto.builder()
                .id(event.getId().toString())
                .eventType(event.getEventType())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .entityCode((String) payload.get("code"))
                .entityTitle((String) payload.get("title"))
                .actorId(actorId)
                .actorName(actorName)
                .timestamp(event.getCreatedAt())
                .changes(payload)
                .description(buildDescription(event))
                .build();
    }

    private String buildDescription(OutboxEvent event) {
        LineageEventType type = event.getEventType();
        String target = event.getAggregateType().toLowerCase().replace("_", " ");

        return switch (type) {
            case REQUIREMENT_CREATED -> "Created requirement";
            case REQUIREMENT_UPDATED -> "Updated requirement";
            case REQUIREMENT_DELETED -> "Deleted requirement";
            case REQUIREMENT_STATUS_CHANGED -> "Changed requirement status";
            case STORY_CREATED -> "Created user story";
            case STORY_UPDATED -> "Updated user story";
            case STORY_DELETED -> "Deleted user story";
            case STORY_SPRINT_ASSIGNED -> "Assigned story to sprint";
            case TASK_CREATED -> "Created task";
            case TASK_UPDATED -> "Updated task";
            case TASK_DELETED -> "Deleted task";
            case TASK_STATUS_CHANGED -> "Changed task status";
            case REQUIREMENT_STORY_LINKED -> "Linked requirement to story";
            case REQUIREMENT_STORY_UNLINKED -> "Unlinked requirement from story";
            case STORY_TASK_LINKED -> "Linked story to task";
            case STORY_TASK_UNLINKED -> "Unlinked story from task";
            case REQUIREMENT_TASK_LINKED -> "Linked requirement to task";
            case REQUIREMENT_TASK_UNLINKED -> "Unlinked requirement from task";
            case SPRINT_CREATED -> "Created sprint";
            case SPRINT_STARTED -> "Started sprint";
            case SPRINT_COMPLETED -> "Completed sprint";
        };
    }
}
