package com.insuretech.pms.project.service;

import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.repository.BacklogItemRepository;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for Sprint Planning workflow
 *
 * Handles:
 * - Moving selected backlog items to sprint
 * - Capacity validation
 * - Sprint creation and management
 * - Progress tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SprintPlanningService {

    private final BacklogItemRepository backlogItemRepository;
    private final SprintRepository sprintRepository;

    /**
     * Create sprint from selected backlog items
     *
     * @param projectId the project ID
     * @param sprintName sprint name
     * @param sprintGoal sprint goal
     * @param startDate sprint start date
     * @param endDate sprint end date
     * @param backlogId the backlog ID
     * @return created Sprint entity
     */
    public Sprint createSprintFromSelectedItems(String projectId, String sprintName, String sprintGoal,
                                                 LocalDate startDate, LocalDate endDate, String backlogId) {
        log.info("Creating sprint '{}' for project '{}'", sprintName, projectId);

        List<BacklogItem> selectedItems = backlogItemRepository.findSelectedItemsForSprintPlanning(backlogId);

        if (selectedItems.isEmpty()) {
            log.warn("No selected items found in backlog '{}' for sprint planning", backlogId);
            throw new IllegalStateException("Cannot create sprint without selected items");
        }

        Sprint sprint = Sprint.builder()
                .projectId(projectId)
                .name(sprintName)
                .goal(sprintGoal)
                .startDate(startDate)
                .endDate(endDate)
                .status(Sprint.SprintStatus.PLANNED)
                .build();

        Sprint savedSprint = sprintRepository.save(sprint);
        log.info("Sprint '{}' created with ID '{}'", sprintName, savedSprint.getId());

        moveItemsToSprint(backlogId, savedSprint.getId());

        return savedSprint;
    }

    /**
     * Move selected items from backlog to sprint
     *
     * @param backlogId the backlog ID
     * @param sprintId the target sprint ID
     * @return list of moved BacklogItems
     */
    public List<BacklogItem> moveItemsToSprint(String backlogId, String sprintId) {
        log.info("Moving selected items from backlog '{}' to sprint '{}'", backlogId, sprintId);

        List<BacklogItem> selectedItems = backlogItemRepository.findSelectedItemsForSprintPlanning(backlogId);

        validateSprintCapacity(selectedItems);

        for (BacklogItem item : selectedItems) {
            item.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
            item.setSprintId(sprintId);
        }

        List<BacklogItem> movedItems = backlogItemRepository.saveAll(selectedItems);
        log.info("Moved {} items to sprint '{}'", movedItems.size(), sprintId);

        return movedItems;
    }

    /**
     * Validate sprint capacity constraints
     *
     * @param items items to be added to sprint
     * @throws IllegalStateException if capacity constraints are violated
     */
    private void validateSprintCapacity(List<BacklogItem> items) {
        Integer totalStoryPoints = items.stream()
                .map(BacklogItem::getStoryPoints)
                .filter(sp -> sp != null)
                .reduce(0, Integer::sum);

        Integer totalEffortHours = items.stream()
                .map(BacklogItem::getEstimatedEffortHours)
                .filter(eh -> eh != null)
                .reduce(0, Integer::sum);

        if (totalStoryPoints == null || totalStoryPoints == 0) {
            log.warn("Sprint capacity validation: No story points assigned to items");
            throw new IllegalStateException("Sprint items must have story points assigned");
        }

        log.info("Sprint capacity validation passed: {} story points, {} effort hours",
                totalStoryPoints, totalEffortHours);
    }

    /**
     * Get items in a sprint
     *
     * @param sprintId the sprint ID
     * @return list of BacklogItems in the sprint
     */
    @Transactional(readOnly = true)
    public List<BacklogItem> getSprintItems(String sprintId) {
        return backlogItemRepository.findBySprintId(sprintId);
    }

    /**
     * Remove item from sprint (move back to SELECTED status)
     *
     * @param itemId the backlog item ID
     */
    public BacklogItem removeItemFromSprint(String itemId) {
        log.info("Removing item '{}' from sprint", itemId);

        BacklogItem item = backlogItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Backlog item not found: " + itemId));

        if (!item.getStatus().equals(BacklogItem.BacklogItemStatus.SPRINT)) {
            throw new IllegalStateException("Can only remove items in SPRINT status");
        }

        item.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
        item.setSprintId(null);

        BacklogItem updated = backlogItemRepository.save(item);
        log.info("Item '{}' removed from sprint and set to SELECTED status", itemId);

        return updated;
    }

    /**
     * Complete sprint and transition items
     *
     * @param sprintId the sprint ID
     */
    public Sprint completeSprint(String sprintId) {
        log.info("Completing sprint '{}'", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + sprintId));

        sprint.setStatus(Sprint.SprintStatus.COMPLETED);

        Sprint completedSprint = sprintRepository.save(sprint);
        log.info("Sprint '{}' marked as COMPLETED", sprintId);

        return completedSprint;
    }

    /**
     * Get sprint by ID
     *
     * @param sprintId the sprint ID
     * @return Sprint entity
     */
    @Transactional(readOnly = true)
    public Sprint getSprint(String sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + sprintId));
    }

    /**
     * Get active sprint for project
     *
     * @param projectId the project ID
     * @return Optional containing active Sprint if found
     */
    @Transactional(readOnly = true)
    public Optional<Sprint> getActiveSprint(String projectId) {
        return sprintRepository.findByProjectIdAndStatus(projectId, Sprint.SprintStatus.ACTIVE);
    }

    /**
     * Get all sprints for project
     *
     * @param projectId the project ID
     * @return list of Sprints for the project
     */
    @Transactional(readOnly = true)
    public List<Sprint> getProjectSprints(String projectId) {
        return sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
    }

    /**
     * Get sprint capacity metrics
     *
     * @param sprintId the sprint ID
     * @return map of capacity metrics (total story points, effort hours, item count)
     */
    @Transactional(readOnly = true)
    public SprintCapacityMetrics getSprintCapacity(String sprintId) {
        List<BacklogItem> items = backlogItemRepository.findBySprintId(sprintId);

        int totalStoryPoints = items.stream()
                .map(BacklogItem::getStoryPoints)
                .filter(sp -> sp != null)
                .reduce(0, Integer::sum);

        int totalEffortHours = items.stream()
                .map(BacklogItem::getEstimatedEffortHours)
                .filter(eh -> eh != null)
                .reduce(0, Integer::sum);

        long completedCount = items.stream()
                .filter(item -> item.getStatus().equals(BacklogItem.BacklogItemStatus.COMPLETED))
                .count();

        return new SprintCapacityMetrics(
                sprintId,
                items.size(),
                totalStoryPoints,
                totalEffortHours,
                completedCount,
                items.size() - completedCount
        );
    }

    /**
     * Capacity metrics for a sprint
     */
    public static class SprintCapacityMetrics {
        private final String sprintId;
        private final int totalItems;
        private final int totalStoryPoints;
        private final int totalEffortHours;
        private final long completedItems;
        private final long remainingItems;

        public SprintCapacityMetrics(String sprintId, int totalItems, int totalStoryPoints,
                                     int totalEffortHours, long completedItems, long remainingItems) {
            this.sprintId = sprintId;
            this.totalItems = totalItems;
            this.totalStoryPoints = totalStoryPoints;
            this.totalEffortHours = totalEffortHours;
            this.completedItems = completedItems;
            this.remainingItems = remainingItems;
        }

        public String getSprintId() { return sprintId; }
        public int getTotalItems() { return totalItems; }
        public int getTotalStoryPoints() { return totalStoryPoints; }
        public int getTotalEffortHours() { return totalEffortHours; }
        public long getCompletedItems() { return completedItems; }
        public long getRemainingItems() { return remainingItems; }
    }
}
