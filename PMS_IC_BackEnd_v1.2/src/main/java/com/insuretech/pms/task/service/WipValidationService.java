package com.insuretech.pms.task.service;

import com.insuretech.pms.task.dto.WipValidationResult;
import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.repository.KanbanColumnRepository;
import com.insuretech.pms.task.repository.TaskRepository;
import com.insuretech.pms.task.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WipValidationService {

    private final KanbanColumnRepository kanbanColumnRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;

    /**
     * Validate WIP limit for a specific kanban column before moving a task
     * @param columnId Target column ID
     * @param allowSoftLimitExceeding Allow soft limit to be exceeded (warning only)
     * @return WipValidationResult with validation status and suggestions
     */
    @Transactional(readOnly = true)
    public WipValidationResult validateColumnWipLimit(String columnId, boolean allowSoftLimitExceeding) {
        Optional<KanbanColumn> columnOpt = kanbanColumnRepository.findById(columnId);
        if (columnOpt.isEmpty()) {
            return WipValidationResult.invalid(
                    WipValidationResult.WipViolationType.NONE,
                    columnId,
                    0,
                    0,
                    "Column not found"
            );
        }

        KanbanColumn column = columnOpt.get();
        int currentWipCount = getColumnWipCount(columnId);

        // Check hard limit (strict enforcement)
        if (column.getWipLimitHard() != null && currentWipCount >= column.getWipLimitHard()) {
            return WipValidationResult.invalid(
                    WipValidationResult.WipViolationType.COLUMN_HARD_LIMIT,
                    column.getName(),
                    currentWipCount,
                    column.getWipLimitHard(),
                    String.format("Hard WIP limit (%d) reached for column '%s'. Current WIP: %d",
                            column.getWipLimitHard(), column.getName(), currentWipCount)
            );
        }

        // Check soft limit (warning only)
        if (column.getWipLimitSoft() != null && currentWipCount >= column.getWipLimitSoft()) {
            if (!allowSoftLimitExceeding) {
                WipValidationResult result = WipValidationResult.invalid(
                        WipValidationResult.WipViolationType.COLUMN_SOFT_LIMIT,
                        column.getName(),
                        currentWipCount,
                        column.getWipLimitSoft(),
                        String.format("Soft WIP limit (%d) exceeded for column '%s'. Current WIP: %d. This is a warning.",
                                column.getWipLimitSoft(), column.getName(), currentWipCount)
                );
                result.setSuggestions(getSoftLimitSuggestions(column, currentWipCount));
                return result;
            }
        }

        return WipValidationResult.valid();
    }

    /**
     * Validate CONWIP (Constant Work In Progress) for an entire sprint
     * @param sprintId Sprint ID
     * @return WipValidationResult with validation status
     */
    @Transactional(readOnly = true)
    public WipValidationResult validateSprintConwip(String sprintId) {
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        if (sprintOpt.isEmpty()) {
            return WipValidationResult.invalid(
                    WipValidationResult.WipViolationType.NONE,
                    sprintId,
                    0,
                    0,
                    "Sprint not found"
            );
        }

        Sprint sprint = sprintOpt.get();

        // CONWIP validation only applies if enabled and limit is set
        if (!sprint.getEnableWipValidation() || sprint.getConwipLimit() == null) {
            return WipValidationResult.valid();
        }

        int currentSprintWip = getSprintWipCount(sprintId);

        if (currentSprintWip >= sprint.getConwipLimit()) {
            WipValidationResult result = WipValidationResult.invalid(
                    WipValidationResult.WipViolationType.SPRINT_CONWIP_LIMIT,
                    "Sprint: " + sprint.getName(),
                    currentSprintWip,
                    sprint.getConwipLimit(),
                    String.format("CONWIP limit (%d) reached for sprint '%s'. Current total WIP: %d",
                            sprint.getConwipLimit(), sprint.getName(), currentSprintWip)
            );
            result.setSuggestions(getConwipSuggestions(sprint, currentSprintWip));
            return result;
        }

        return WipValidationResult.valid();
    }

    /**
     * Validate personal WIP limit for a specific assignee
     * @param assigneeId User ID of the assignee
     * @param maxPersonalWip Maximum tasks allowed per person (configurable)
     * @return WipValidationResult with validation status
     */
    @Transactional(readOnly = true)
    public WipValidationResult validatePersonalWipLimit(String assigneeId, int maxPersonalWip) {
        if (maxPersonalWip <= 0) {
            return WipValidationResult.valid();
        }

        List<Task> assigneeTasks = taskRepository.findByAssigneeIdAndStatusNot(
                assigneeId,
                Task.TaskStatus.DONE
        );

        int personalWipCount = assigneeTasks.size();

        if (personalWipCount >= maxPersonalWip) {
            WipValidationResult result = WipValidationResult.invalid(
                    WipValidationResult.WipViolationType.PERSONAL_WIP_LIMIT,
                    "Assignee: " + assigneeId,
                    personalWipCount,
                    maxPersonalWip,
                    String.format("Personal WIP limit (%d) reached for assignee. Current tasks: %d",
                            maxPersonalWip, personalWipCount)
            );
            result.setSuggestions(getPersonalWipSuggestions(assigneeId, assigneeTasks));
            return result;
        }

        return WipValidationResult.valid();
    }

    /**
     * Get overall WIP status for a project
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectWipStatus(String projectId) {
        List<KanbanColumn> columns = kanbanColumnRepository.findByProjectIdOrderByOrderNumAsc(projectId);

        Map<String, Object> wipStatus = new HashMap<>();
        List<Map<String, Object>> columnStatuses = new ArrayList<>();
        int totalWip = 0;
        int bottleneckCount = 0;

        for (KanbanColumn column : columns) {
            int columnWip = getColumnWipCount(column.getId());
            totalWip += columnWip;

            Map<String, Object> columnStatus = new HashMap<>();
            columnStatus.put("columnId", column.getId());
            columnStatus.put("columnName", column.getName());
            columnStatus.put("wipCount", columnWip);
            columnStatus.put("wipLimitSoft", column.getWipLimitSoft());
            columnStatus.put("wipLimitHard", column.getWipLimitHard());
            columnStatus.put("isBottleneck", column.getIsBottleneckColumn());

            // Calculate health status
            String health = calculateColumnHealth(column, columnWip);
            columnStatus.put("health", health);

            if (column.getIsBottleneckColumn()) {
                bottleneckCount++;
            }

            columnStatuses.add(columnStatus);
        }

        wipStatus.put("projectId", projectId);
        wipStatus.put("totalWip", totalWip);
        wipStatus.put("columnStatuses", columnStatuses);
        wipStatus.put("bottleneckCount", bottleneckCount);

        return wipStatus;
    }

    /**
     * Get WIP count for a specific column (tasks in not-done status)
     */
    private int getColumnWipCount(String columnId) {
        return taskRepository.countByColumnIdAndStatusNot(
                columnId,
                Task.TaskStatus.DONE
        );
    }

    /**
     * Get total WIP count for a sprint (all non-done tasks assigned to sprint)
     */
    private int getSprintWipCount(String sprintId) {
        return taskRepository.countBySprintIdAndStatusNot(
                sprintId,
                Task.TaskStatus.DONE
        );
    }

    /**
     * Generate suggestions for soft limit exceeded scenario
     */
    private List<String> getSoftLimitSuggestions(KanbanColumn column, int currentWip) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Consider completing some in-progress tasks before adding more");
        suggestions.add("Check for blockers that might be preventing task completion");

        if (column.getIsBottleneckColumn()) {
            suggestions.add("This column is marked as a bottleneck - prioritize completing tasks here");
        }

        Integer softLimit = column.getWipLimitSoft();
        if (softLimit != null) {
            int excess = currentWip - softLimit;
            if (excess > 2) {
                suggestions.add(String.format("Consider increasing team capacity or reassigning tasks (excess: %d)", excess));
            }
        }

        return suggestions;
    }

    /**
     * Generate suggestions for CONWIP limit reached scenario
     */
    private List<String> getConwipSuggestions(Sprint sprint, int currentWip) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Sprint is at full capacity - complete some tasks before starting new ones");
        suggestions.add("Review task priorities and dependencies");
        suggestions.add("Consider removing low-priority tasks from sprint backlog");

        if (sprint.getConwipLimit() != null && currentWip > sprint.getConwipLimit() * 1.2) {
            suggestions.add("Consider increasing sprint capacity or CONWIP limit for future sprints");
        }

        return suggestions;
    }

    /**
     * Generate suggestions for personal WIP limit reached scenario
     */
    private List<String> getPersonalWipSuggestions(String assigneeId, List<Task> tasks) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("This assignee has reached their personal WIP limit");
        suggestions.add("Consider redistributing tasks to other team members");

        // Find tasks that might be blocked
        long blockedCount = tasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                .count();

        if (blockedCount > 0) {
            suggestions.add(String.format("There are %d in-progress tasks - check for blockers", blockedCount));
        }

        return suggestions;
    }

    /**
     * Calculate health status of a column based on WIP levels
     */
    private String calculateColumnHealth(KanbanColumn column, int currentWip) {
        if (column.getWipLimitHard() != null && currentWip >= column.getWipLimitHard()) {
            return "RED"; // At hard limit
        }
        if (column.getWipLimitSoft() != null && currentWip >= column.getWipLimitSoft()) {
            return "YELLOW"; // At soft limit
        }
        return "GREEN"; // Under soft limit
    }

    /**
     * Get detailed WIP status for a specific column
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getColumnWipStatus(String columnId) {
        Optional<KanbanColumn> columnOpt = kanbanColumnRepository.findById(columnId);
        if (columnOpt.isEmpty()) {
            return Map.of("error", "Column not found");
        }

        KanbanColumn column = columnOpt.get();
        int currentWip = getColumnWipCount(columnId);

        Map<String, Object> status = new HashMap<>();
        status.put("columnId", column.getId());
        status.put("columnName", column.getName());
        status.put("currentWip", currentWip);
        status.put("wipLimitSoft", column.getWipLimitSoft());
        status.put("wipLimitHard", column.getWipLimitHard());
        status.put("isBottleneck", column.getIsBottleneckColumn());
        status.put("health", calculateColumnHealth(column, currentWip));

        // Calculate percentages
        if (column.getWipLimitHard() != null) {
            status.put("hardLimitPercentage", (currentWip * 100) / column.getWipLimitHard());
        }
        if (column.getWipLimitSoft() != null) {
            status.put("softLimitPercentage", (currentWip * 100) / column.getWipLimitSoft());
        }

        return status;
    }

    /**
     * Get detailed WIP status for a specific sprint
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSprintWipStatus(String sprintId) {
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        if (sprintOpt.isEmpty()) {
            return Map.of("error", "Sprint not found");
        }

        Sprint sprint = sprintOpt.get();
        int currentWip = getSprintWipCount(sprintId);

        Map<String, Object> status = new HashMap<>();
        status.put("sprintId", sprint.getId());
        status.put("sprintName", sprint.getName());
        status.put("currentWip", currentWip);
        status.put("conwipLimit", sprint.getConwipLimit());
        status.put("wipValidationEnabled", sprint.getEnableWipValidation());

        // Calculate percentages if limit is set
        if (sprint.getConwipLimit() != null && sprint.getConwipLimit() > 0) {
            status.put("conwipPercentage", (currentWip * 100) / sprint.getConwipLimit());
            status.put("health", currentWip >= sprint.getConwipLimit() ? "RED" : "GREEN");
        } else {
            status.put("health", "UNKNOWN");
        }

        return status;
    }
}
