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

import java.util.List;

/**
 * Service for Backlog Item management
 *
 * Handles:
 * - BacklogItem CRUD operations
 * - State transitions (BACKLOG -> SELECTED -> SPRINT -> COMPLETED)
 * - Story point synchronization with linked requirements
 * - Origin type validation (REQUIREMENT vs MANUAL)
 */
@Service
@RequiredArgsConstructor
public class BacklogItemService {

    private final BacklogItemRepository backlogItemRepository;
    private final BacklogRepository backlogRepository;
    private final RequirementRepository requirementRepository;

    /**
     * Create a new backlog item from requirement
     */
    @Transactional
    public BacklogItem createBacklogItemFromRequirement(String backlogId, String requirementId) {
        Backlog backlog = backlogRepository.findById(backlogId)
                .orElseThrow(() -> CustomException.notFound("Backlog not found"));

        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> CustomException.notFound("Requirement not found"));

        BacklogItem item = new BacklogItem();
        item.setBacklog(backlog);
        item.setRequirement(requirement);
        item.setOriginType(BacklogItem.BacklogItemOrigin.REQUIREMENT);
        item.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);

        Integer maxPriority = backlogItemRepository.findMaxPriorityOrderByBacklogId(backlogId);
        item.setPriorityOrder((maxPriority != null ? maxPriority : 0) + 1);

        if (requirement.getStoryPoints() != null) {
            item.setStoryPoints(requirement.getStoryPoints());
        }

        if (requirement.getAcceptanceCriteria() != null) {
            item.setAcceptanceCriteria(requirement.getAcceptanceCriteria());
        }

        return backlogItemRepository.save(item);
    }

    /**
     * Create a manual backlog item
     */
    @Transactional
    public BacklogItem createManualBacklogItem(String backlogId, String title, Integer storyPoints) {
        Backlog backlog = backlogRepository.findById(backlogId)
                .orElseThrow(() -> CustomException.notFound("Backlog not found"));

        BacklogItem item = new BacklogItem();
        item.setBacklog(backlog);
        item.setOriginType(BacklogItem.BacklogItemOrigin.MANUAL);
        item.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        item.setStoryPoints(storyPoints);

        Integer maxPriority = backlogItemRepository.findMaxPriorityOrderByBacklogId(backlogId);
        item.setPriorityOrder((maxPriority != null ? maxPriority : 0) + 1);

        return backlogItemRepository.save(item);
    }

    /**
     * Get backlog item by ID
     */
    @Transactional(readOnly = true)
    public BacklogItem getBacklogItem(String itemId) {
        return backlogItemRepository.findById(itemId)
                .orElseThrow(() -> CustomException.notFound("Backlog item not found"));
    }

    /**
     * Update backlog item story points
     */
    @Transactional
    public BacklogItem updateStoryPoints(String itemId, Integer storyPoints) {
        BacklogItem item = getBacklogItem(itemId);

        if (storyPoints != null && storyPoints < 0) {
            throw CustomException.badRequest("Story points must be non-negative");
        }

        item.setStoryPoints(storyPoints);
        return backlogItemRepository.save(item);
    }

    /**
     * Update estimated effort hours
     */
    @Transactional
    public BacklogItem updateEstimatedEffort(String itemId, Integer effortHours) {
        BacklogItem item = getBacklogItem(itemId);

        if (effortHours != null && effortHours < 0) {
            throw CustomException.badRequest("Effort hours must be non-negative");
        }

        item.setEstimatedEffortHours(effortHours);
        return backlogItemRepository.save(item);
    }

    /**
     * Transition backlog item to SELECTED state
     */
    @Transactional
    public BacklogItem selectForSprintPlanning(String itemId) {
        BacklogItem item = getBacklogItem(itemId);

        if (item.getOriginType() == BacklogItem.BacklogItemOrigin.REQUIREMENT
                && item.getStoryPoints() == null) {
            throw CustomException.badRequest(
                    "Cannot select requirement-linked item without story points");
        }

        item.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
        return backlogItemRepository.save(item);
    }

    /**
     * Transition backlog item to SPRINT state
     */
    @Transactional
    public BacklogItem moveToSprint(String itemId) {
        BacklogItem item = getBacklogItem(itemId);

        if (item.getStatus() != BacklogItem.BacklogItemStatus.SELECTED) {
            throw CustomException.badRequest(
                    "Only SELECTED items can be moved to sprint");
        }

        item.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
        return backlogItemRepository.save(item);
    }

    /**
     * Mark backlog item as completed
     */
    @Transactional
    public BacklogItem completeBacklogItem(String itemId) {
        BacklogItem item = getBacklogItem(itemId);

        if (item.getStatus() != BacklogItem.BacklogItemStatus.SPRINT) {
            throw CustomException.badRequest(
                    "Only items in SPRINT can be marked as completed");
        }

        item.setStatus(BacklogItem.BacklogItemStatus.COMPLETED);
        return backlogItemRepository.save(item);
    }

    /**
     * Move item back to backlog (deselect)
     */
    @Transactional
    public BacklogItem moveBackToBacklog(String itemId) {
        BacklogItem item = getBacklogItem(itemId);

        if (item.getStatus() == BacklogItem.BacklogItemStatus.BACKLOG) {
            throw CustomException.badRequest("Item is already in backlog");
        }

        if (item.getStatus() == BacklogItem.BacklogItemStatus.COMPLETED) {
            throw CustomException.badRequest("Cannot move completed items back to backlog");
        }

        item.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        return backlogItemRepository.save(item);
    }

    /**
     * Delete backlog item
     */
    @Transactional
    public void deleteBacklogItem(String itemId) {
        BacklogItem item = getBacklogItem(itemId);

        if (item.getStatus() == BacklogItem.BacklogItemStatus.SPRINT
                || item.getStatus() == BacklogItem.BacklogItemStatus.COMPLETED) {
            throw CustomException.badRequest(
                    "Cannot delete items in SPRINT or COMPLETED status");
        }

        backlogItemRepository.deleteById(itemId);
    }

    /**
     * Get backlog items by requirement ID
     */
    @Transactional(readOnly = true)
    public BacklogItem findByRequirementId(String requirementId) {
        return backlogItemRepository.findByRequirementId(requirementId)
                .orElseThrow(() -> CustomException.notFound(
                        "No backlog item found for requirement: " + requirementId));
    }

    /**
     * Synchronize story points between requirement and backlog item
     */
    @Transactional
    public void syncStoryPointsFromRequirement(String itemId) {
        BacklogItem item = getBacklogItem(itemId);

        if (item.getRequirement() == null) {
            throw CustomException.badRequest("Item is not linked to a requirement");
        }

        Requirement requirement = item.getRequirement();
        if (requirement.getStoryPoints() != null) {
            item.setStoryPoints(requirement.getStoryPoints());
            backlogItemRepository.save(item);
        }
    }

    /**
     * Get all items for a backlog
     */
    @Transactional(readOnly = true)
    public List<BacklogItem> getItemsByBacklogId(String backlogId) {
        return backlogItemRepository.findByBacklogIdOrderByPriorityOrderAsc(backlogId);
    }

    /**
     * Count items by status in backlog
     */
    @Transactional(readOnly = true)
    public long countByStatus(String backlogId, String status) {
        return backlogItemRepository.countByBacklogIdAndStatus(backlogId, status);
    }
}
