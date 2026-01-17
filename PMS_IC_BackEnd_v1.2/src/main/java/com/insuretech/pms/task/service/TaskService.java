package com.insuretech.pms.task.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.task.dto.TaskRequest;
import com.insuretech.pms.task.dto.TaskResponse;
import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.event.TaskStatusChangedEvent;
import com.insuretech.pms.task.repository.KanbanColumnRepository;
import com.insuretech.pms.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        KanbanColumn column = null;
        if (request.getColumnId() != null && !request.getColumnId().isBlank()) {
            column = kanbanColumnRepository.findById(request.getColumnId())
                    .orElseThrow(() -> CustomException.notFound("칸반 컬럼을 찾을 수 없습니다: " + request.getColumnId()));
        }

        Task task = Task.builder()
                .column(column)
                .phaseId(request.getPhaseId())
                .title(request.getTitle())
                .description(request.getDescription())
                .assigneeId(request.getAssigneeId())
                .priority(parsePriority(request.getPriority()))
                .status(parseStatus(request.getStatus()))
                .dueDate(request.getDueDate())
                .orderNum(request.getOrderNum())
                .tags(joinTags(request.getTags()))
                .trackType(parseTrackType(request.getTrackType()))
                .build();

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .columnId(task.getColumn() != null ? task.getColumn().getId() : null)
                .phaseId(task.getPhaseId())
                .title(task.getTitle())
                .description(task.getDescription())
                .assigneeId(task.getAssigneeId())
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .dueDate(task.getDueDate())
                .orderNum(task.getOrderNum())
                .tags(parseTags(task.getTags()))
                .trackType(task.getTrackType() != null ? task.getTrackType().name() : null)
                .build();
    }

    private Task.Priority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return Task.Priority.MEDIUM;
        }
        try {
            return Task.Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Task.Priority.MEDIUM;
        }
    }

    private Task.TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Task.TaskStatus.TODO;
        }
        try {
            return Task.TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Task.TaskStatus.TODO;
        }
    }

    private Task.TrackType parseTrackType(String trackType) {
        if (trackType == null || trackType.isBlank()) {
            return Task.TrackType.COMMON;
        }
        try {
            return Task.TrackType.valueOf(trackType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Task.TrackType.COMMON;
        }
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }

    /**
     * Update task status and publish TaskStatusChangedEvent
     * Event will be picked up by TaskProgressCalculator to update requirement progress
     */
    @Transactional
    public TaskResponse updateTaskStatus(String taskId, String newStatus, String requirementId, String changedBy) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw CustomException.notFound("작업을 찾을 수 없습니다: " + taskId);
        }

        Task task = taskOpt.get();
        Task.TaskStatus oldStatus = task.getStatus();
        Task.TaskStatus statusEnum = parseStatus(newStatus);

        // Only publish event if status actually changed
        if (oldStatus != statusEnum) {
            task.setStatus(statusEnum);
            Task savedTask = taskRepository.save(task);

            // Publish event for progress calculation
            if (requirementId != null) {
                TaskStatusChangedEvent event = new TaskStatusChangedEvent(
                    taskId,
                    requirementId,
                    oldStatus,
                    statusEnum,
                    changedBy
                );
                applicationEventPublisher.publishEvent(event);
                log.info("Published TaskStatusChangedEvent for task {} requirement {}: {} -> {}",
                    taskId, requirementId, oldStatus, statusEnum);
            }

            return toResponse(savedTask);
        }

        return toResponse(task);
    }
}
