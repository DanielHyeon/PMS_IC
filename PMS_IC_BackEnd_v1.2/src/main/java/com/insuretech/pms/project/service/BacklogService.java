package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.entity.Backlog;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.repository.BacklogRepository;
import com.insuretech.pms.project.repository.BacklogItemRepository;
import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for Product Backlog management
 *
 * Handles:
 * - Backlog CRUD operations
 * - Backlog item priority management
 * - Progress calculation triggers
 * - Story point synchronization
 */
@Service
@RequiredArgsConstructor
public class BacklogService {

    private static final Logger logger = Logger.getLogger(BacklogService.class.getName());

    private final BacklogRepository backlogRepository;
    private final BacklogItemRepository backlogItemRepository;
    private final RequirementRepository requirementRepository;

    /**
     * Get or create active backlog for a project
     */
    @Transactional
    public Backlog getOrCreateBacklog(String projectId) {
        var optional = backlogRepository.findActiveBacklogByProjectId(projectId);
        if (optional.isPresent()) {
            return optional.get();
        }

        Backlog backlog = new Backlog();
        backlog.setProjectId(projectId);
        backlog.setName("Product Backlog");
        backlog.setDescription("Default product backlog");
        backlog.setStatus(Backlog.BacklogStatus.ACTIVE);
        return backlogRepository.save(backlog);
    }

    /**
     * Get all backlogs for project
     */
    @Transactional(readOnly = true)
    public List<Backlog> getBacklogsByProjectId(String projectId) {
        return backlogRepository.findByProjectId(projectId);
    }

    /**
     * Get active backlog
     */
    @Transactional(readOnly = true)
    public Backlog getActiveBacklog(String projectId) {
        return backlogRepository.findActiveBacklogByProjectId(projectId)
                .orElseThrow(() -> CustomException.notFound("Backlog not found"));
    }

    /**
     * Get all backlog items sorted by priority
     */
    @Transactional(readOnly = true)
    public List<BacklogItem> getBacklogItems(String backlogId) {
        return backlogItemRepository.findByBacklogIdOrderByPriorityOrderAsc(backlogId);
    }

    /**
     * Get backlog items by status
     */
    @Transactional(readOnly = true)
    public List<BacklogItem> getBacklogItemsByStatus(String backlogId, String status) {
        return backlogItemRepository.findByBacklogIdAndStatus(backlogId, status);
    }

    /**
     * Get selected items for sprint planning
     */
    @Transactional(readOnly = true)
    public List<BacklogItem> getSelectedItemsForSprintPlanning(String backlogId) {
        return backlogItemRepository.findSelectedItemsForSprintPlanning(backlogId);
    }

    /**
     * Reorder backlog items by priority
     */
    @Transactional
    public void reorderBacklogItems(String backlogId, List<String> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            String itemId = itemIds.get(i);
            BacklogItem item = backlogItemRepository.findById(itemId)
                    .orElseThrow(() -> CustomException.notFound("Item not found"));
            item.setPriorityOrder(i);
            backlogItemRepository.save(item);
        }
    }

    /**
     * Get total story points for selected items
     */
    @Transactional(readOnly = true)
    public Integer getTotalStoryPointsForSelectedItems(String backlogId) {
        Integer total = backlogItemRepository.sumStoryPointsForSelectedItems(backlogId);
        return total != null ? total : 0;
    }

    /**
     * Count items by status
     */
    @Transactional(readOnly = true)
    public long countItemsByStatus(String backlogId, String status) {
        return backlogItemRepository.countByBacklogIdAndStatus(backlogId, status);
    }

    /**
     * Calculate total effort for selected items
     */
    @Transactional(readOnly = true)
    public Integer calculateTotalEffortForSelectedItems(String backlogId) {
        List<BacklogItem> selectedItems = getSelectedItemsForSprintPlanning(backlogId);
        int total = 0;
        for (BacklogItem item : selectedItems) {
            if (item.getEstimatedEffortHours() != null) {
                total += item.getEstimatedEffortHours();
            }
        }
        return total;
    }

    /**
     * Trigger progress calculation for requirement
     */
    @Transactional
    public void triggerProgressCalculation(String requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> CustomException.notFound("Requirement not found"));

        Integer newProgress = calculateProgress(requirement);

        if (!newProgress.equals(requirement.getProgressPercentage())) {
            requirement.setProgressPercentage(newProgress);
            requirement.setLastProgressUpdate(LocalDateTime.now());
            requirementRepository.save(requirement);
        }
    }

    /**
     * Calculate progress based on method
     */
    private Integer calculateProgress(Requirement requirement) {
        var method = requirement.getProgressCalcMethod();
        if (method == null) {
            method = Requirement.ProgressCalculationMethod.STORY_POINT;
        }

        return switch (method) {
            case STORY_POINT -> calculateProgressByStoryPoints(requirement);
            case TASK_COUNT -> calculateProgressByTaskCount(requirement);
            case TIME_BASED -> calculateProgressByTime(requirement);
        };
    }

    /**
     * Calculate by story points
     */
    private Integer calculateProgressByStoryPoints(Requirement requirement) {
        if (requirement.getStoryPoints() == null || requirement.getStoryPoints() <= 0) {
            return calculateProgressByTaskCount(requirement);
        }
        return requirement.getProgressPercentage() != null ? requirement.getProgressPercentage() : 0;
    }

    /**
     * Calculate by task count
     */
    private Integer calculateProgressByTaskCount(Requirement requirement) {
        if (requirement.getLinkedTaskIds() == null || requirement.getLinkedTaskIds().isEmpty()) {
            return 0;
        }
        return requirement.getProgressPercentage() != null ? requirement.getProgressPercentage() : 0;
    }

    /**
     * Calculate by time
     */
    private Integer calculateProgressByTime(Requirement requirement) {
        if (requirement.getEstimatedEffortHours() == null || requirement.getEstimatedEffortHours() <= 0) {
            return requirement.getProgressPercentage() != null ? requirement.getProgressPercentage() : 0;
        }

        if (requirement.getActualEffortHours() == null) {
            return 0;
        }

        int progress = (int) ((requirement.getActualEffortHours() * 100L) / requirement.getEstimatedEffortHours());
        return Math.min(progress, 100);
    }
}
