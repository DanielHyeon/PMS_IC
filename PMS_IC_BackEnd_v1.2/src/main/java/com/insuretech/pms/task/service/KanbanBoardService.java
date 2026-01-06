package com.insuretech.pms.task.service;

import com.insuretech.pms.task.dto.KanbanColumnResponse;
import com.insuretech.pms.task.dto.TaskSummaryResponse;
import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.repository.KanbanColumnRepository;
import com.insuretech.pms.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KanbanBoardService {

    private final KanbanColumnRepository kanbanColumnRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<KanbanColumnResponse> getColumns(String projectId) {
        List<KanbanColumn> columns = (projectId == null || projectId.isBlank())
                ? kanbanColumnRepository.findAllByOrderByOrderNumAsc()
                : kanbanColumnRepository.findByProjectIdOrderByOrderNumAsc(projectId);

        return columns.stream()
                .map(this::toColumnResponse)
                .collect(Collectors.toList());
    }

    private KanbanColumnResponse toColumnResponse(KanbanColumn column) {
        List<Task> tasks = taskRepository.findByColumnIdOrderByOrderNumAsc(column.getId());
        List<TaskSummaryResponse> taskResponses = tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());

        return KanbanColumnResponse.builder()
                .id(column.getId())
                .name(column.getName())
                .orderNum(column.getOrderNum())
                .wipLimit(column.getWipLimit())
                .color(column.getColor())
                .tasks(taskResponses)
                .build();
    }

    private TaskSummaryResponse toTaskResponse(Task task) {
        return TaskSummaryResponse.builder()
                .id(task.getId())
                .columnId(task.getColumn() != null ? task.getColumn().getId() : null)
                .title(task.getTitle())
                .assigneeId(task.getAssigneeId())
                .priority(task.getPriority() != null ? task.getPriority().name().toLowerCase() : null)
                .status(task.getStatus() != null ? task.getStatus().name().toLowerCase() : null)
                .dueDate(task.getDueDate())
                .orderNum(task.getOrderNum())
                .tags(parseTags(task.getTags()))
                .build();
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
}
