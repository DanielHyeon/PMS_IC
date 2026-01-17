package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.BacklogItemDto;
import com.insuretech.pms.project.dto.CreateBacklogItemRequest;
import com.insuretech.pms.project.dto.UpdateBacklogItemRequest;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.service.BacklogItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Backlog Item management
 *
 * Handles:
 * - Backlog item CRUD operations
 * - State transitions (BACKLOG -> SELECTED -> SPRINT -> COMPLETED)
 * - Story point updates and synchronization
 * - Item deletion with state validation
 */
@RestController
@RequestMapping("/api/projects/{projectId}/backlogs/{backlogId}/items")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BacklogItemController {

    private final BacklogItemService backlogItemService;

    /**
     * Create backlog item from requirement
     */
    @PostMapping("/from-requirement")
    public ResponseEntity<ApiResponse<BacklogItemDto>> createFromRequirement(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @RequestParam String requirementId) {

        BacklogItem item = backlogItemService.createBacklogItemFromRequirement(backlogId, requirementId);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Create manual backlog item
     */
    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<BacklogItemDto>> createManual(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @RequestBody CreateBacklogItemRequest request) {

        BacklogItem item = backlogItemService.createManualBacklogItem(
                backlogId,
                request.getTitle(),
                request.getStoryPoints());

        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Get backlog item by ID
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<BacklogItemDto>> getBacklogItem(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        BacklogItem item = backlogItemService.getBacklogItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Update story points
     */
    @PutMapping("/{itemId}/story-points")
    public ResponseEntity<ApiResponse<BacklogItemDto>> updateStoryPoints(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId,
            @RequestParam Integer storyPoints) {

        BacklogItem item = backlogItemService.updateStoryPoints(itemId, storyPoints);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Update estimated effort hours
     */
    @PutMapping("/{itemId}/estimated-effort")
    public ResponseEntity<ApiResponse<BacklogItemDto>> updateEstimatedEffort(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId,
            @RequestParam Integer effortHours) {

        BacklogItem item = backlogItemService.updateEstimatedEffort(itemId, effortHours);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Update backlog item details
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<BacklogItemDto>> updateBacklogItem(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId,
            @RequestBody UpdateBacklogItemRequest request) {

        BacklogItem item = backlogItemService.getBacklogItem(itemId);

        if (request.getStoryPoints() != null) {
            item = backlogItemService.updateStoryPoints(itemId, request.getStoryPoints());
        }
        if (request.getEstimatedEffortHours() != null) {
            item = backlogItemService.updateEstimatedEffort(itemId, request.getEstimatedEffortHours());
        }
        if (request.getAcceptanceCriteria() != null) {
            item.setAcceptanceCriteria(request.getAcceptanceCriteria());
        }

        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Select item for sprint planning
     * Validates that requirement-linked items have story points
     */
    @PostMapping("/{itemId}/select-for-sprint")
    public ResponseEntity<ApiResponse<BacklogItemDto>> selectForSprintPlanning(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        BacklogItem item = backlogItemService.selectForSprintPlanning(itemId);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Move item to sprint (from SELECTED state)
     */
    @PostMapping("/{itemId}/move-to-sprint")
    public ResponseEntity<ApiResponse<BacklogItemDto>> moveToSprint(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        BacklogItem item = backlogItemService.moveToSprint(itemId);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Mark item as completed
     */
    @PostMapping("/{itemId}/complete")
    public ResponseEntity<ApiResponse<BacklogItemDto>> completeBacklogItem(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        BacklogItem item = backlogItemService.completeBacklogItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Move item back to backlog (deselect)
     */
    @PostMapping("/{itemId}/move-back-to-backlog")
    public ResponseEntity<ApiResponse<BacklogItemDto>> moveBackToBacklog(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        BacklogItem item = backlogItemService.moveBackToBacklog(itemId);
        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Delete backlog item
     * Cannot delete items in SPRINT or COMPLETED state
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteBacklogItem(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        backlogItemService.deleteBacklogItem(itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Synchronize story points from linked requirement
     */
    @PostMapping("/{itemId}/sync-story-points")
    public ResponseEntity<ApiResponse<String>> syncStoryPointsFromRequirement(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @PathVariable String itemId) {

        backlogItemService.syncStoryPointsFromRequirement(itemId);
        return ResponseEntity.ok(ApiResponse.success("Story points synchronized"));
    }
}
