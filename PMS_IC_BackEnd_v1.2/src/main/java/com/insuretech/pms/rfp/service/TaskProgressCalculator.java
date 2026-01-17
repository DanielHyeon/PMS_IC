package com.insuretech.pms.rfp.service;

import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.repository.RequirementRepository;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.event.TaskStatusChangedEvent;
import com.insuretech.pms.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskProgressCalculator {

    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;

    /**
     * Listen for task status changes and update requirement progress
     */
    @EventListener
    @Transactional
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        if (event.getRequirementId() == null) {
            return;
        }

        Optional<Requirement> reqOpt = requirementRepository.findById(event.getRequirementId());
        if (reqOpt.isEmpty()) {
            return;
        }

        Requirement requirement = reqOpt.get();

        // Recalculate progress based on linked tasks
        calculateAndUpdateProgress(requirement);

        // Save updated requirement
        requirementRepository.save(requirement);
    }

    /**
     * Calculate requirement progress based on its linked tasks
     */
    @Transactional
    public void calculateAndUpdateProgress(Requirement requirement) {
        Set<String> linkedTaskIds = requirement.getLinkedTaskIds();

        if (linkedTaskIds == null || linkedTaskIds.isEmpty()) {
            // No linked tasks, keep current progress
            return;
        }

        // Fetch all linked tasks
        int totalLinkedTasks = linkedTaskIds.size();
        int completedTasks = 0;

        for (String taskId : linkedTaskIds) {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent() && taskOpt.get().getStatus() == Task.TaskStatus.DONE) {
                completedTasks++;
            }
        }

        // Calculate progress based on selected method
        int newProgress = calculateProgressByMethod(requirement, completedTasks, totalLinkedTasks);

        // Update requirement
        requirement.setProgressPercentage(newProgress);
        requirement.setLastProgressUpdate(LocalDateTime.now());

        // Update progress stage
        updateProgressStage(requirement);
    }

    /**
     * Calculate progress based on selected calculation method
     */
    private int calculateProgressByMethod(Requirement requirement, int completedTasks, int totalLinkedTasks) {
        switch (requirement.getProgressCalcMethod()) {
            case TASK_COUNT:
                return calculateTaskCountProgress(completedTasks, totalLinkedTasks);

            case TIME_BASED:
                return calculateTimeBasedProgress(requirement);

            case STORY_POINT:
            default:
                return calculateStoryPointProgress(requirement, completedTasks, totalLinkedTasks);
        }
    }

    /**
     * Calculate progress using story points
     */
    private int calculateStoryPointProgress(Requirement requirement, int completedTasks, int totalLinkedTasks) {
        if (requirement.getStoryPoints() == null || requirement.getStoryPoints() == 0) {
            // Fallback to task count if no story points
            return calculateTaskCountProgress(completedTasks, totalLinkedTasks);
        }

        // Simplified: assume each completed task represents progress
        // In real implementation, would track SP per task
        if (totalLinkedTasks == 0) {
            return 0;
        }
        return (completedTasks * 100) / totalLinkedTasks;
    }

    /**
     * Calculate progress using task count
     */
    private int calculateTaskCountProgress(int completedTasks, int totalLinkedTasks) {
        if (totalLinkedTasks == 0) {
            return 0;
        }
        return (completedTasks * 100) / totalLinkedTasks;
    }

    /**
     * Calculate progress using time-based method
     */
    private int calculateTimeBasedProgress(Requirement requirement) {
        if (requirement.getEstimatedEffortHours() == null || requirement.getEstimatedEffortHours() == 0) {
            return 0;
        }

        Integer actualEffort = requirement.getActualEffortHours() != null ? requirement.getActualEffortHours() : 0;
        return Math.min((actualEffort * 100) / requirement.getEstimatedEffortHours(), 100);
    }

    /**
     * Update progress stage based on current progress and due date
     */
    private void updateProgressStage(Requirement requirement) {
        if (requirement.getDueDate() != null && java.time.LocalDate.now().isAfter(requirement.getDueDate())
                && requirement.getProgressPercentage() < 100) {
            requirement.setProgressStage(Requirement.ProgressStage.DELAYED);
        } else if (requirement.getProgressPercentage() == 0) {
            requirement.setProgressStage(Requirement.ProgressStage.NOT_STARTED);
        } else if (requirement.getProgressPercentage() == 100) {
            requirement.setProgressStage(Requirement.ProgressStage.COMPLETED);
        } else {
            requirement.setProgressStage(Requirement.ProgressStage.IN_PROGRESS);
        }
    }

    /**
     * Batch recalculate progress for all requirements in a project
     */
    @Transactional
    public void recalculateProjectProgress(String projectId) {
        // In real implementation, would fetch all requirements for project
        // and recalculate their progress
    }
}
