package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.*;
import com.insuretech.pms.project.entity.Backlog;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.service.BacklogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Product Backlog management
 *
 * Handles:
 * - Backlog CRUD operations
 * - Backlog item management (via BacklogItemController)
 * - Item priority reordering
 * - Sprint planning capacity queries
 * - Progress tracking queries
 */
@RestController
@RequestMapping("/api/projects/{projectId}/backlogs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BacklogController {

    private final BacklogService backlogService;

    /**
     * Get or create active backlog for project
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<BacklogDto>> getOrCreateActiveBacklog(
            @PathVariable String projectId) {
        Backlog backlog = backlogService.getOrCreateBacklog(projectId);
        return ResponseEntity.ok(ApiResponse.success(BacklogDto.fromEntity(backlog)));
    }

    /**
     * Get all backlogs for project
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BacklogDto>>> getBacklogs(
            @PathVariable String projectId) {
        List<BacklogDto> backlogs = backlogService.getBacklogsByProjectId(projectId)
                .stream()
                .map(BacklogDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(backlogs));
    }

    /**
     * Get active backlog (explicit fetch)
     */
    @GetMapping("/active/detailed")
    public ResponseEntity<ApiResponse<BacklogDto>> getActiveBacklog(
            @PathVariable String projectId) {
        Backlog backlog = backlogService.getActiveBacklog(projectId);
        return ResponseEntity.ok(ApiResponse.success(BacklogDto.fromEntity(backlog)));
    }

    /**
     * Get all backlog items sorted by priority
     */
    @GetMapping("/{backlogId}/items")
    public ResponseEntity<ApiResponse<List<BacklogItemDto>>> getBacklogItems(
            @PathVariable String projectId,
            @PathVariable String backlogId) {
        List<BacklogItemDto> items = backlogService.getBacklogItems(backlogId)
                .stream()
                .map(BacklogItemDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Get backlog items by status
     * Query parameter: status (BACKLOG, SELECTED, SPRINT, COMPLETED)
     */
    @GetMapping("/{backlogId}/items/by-status")
    public ResponseEntity<ApiResponse<List<BacklogItemDto>>> getBacklogItemsByStatus(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @RequestParam String status) {
        List<BacklogItemDto> items = backlogService.getBacklogItemsByStatus(backlogId, status)
                .stream()
                .map(BacklogItemDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Get sprint planning capacity summary
     * Includes: selected items count, total story points, total effort
     */
    @GetMapping("/{backlogId}/sprint-planning-capacity")
    public ResponseEntity<ApiResponse<SprintPlanningCapacityDto>> getSprintPlanningCapacity(
            @PathVariable String projectId,
            @PathVariable String backlogId) {

        List<BacklogItem> selectedItems = backlogService.getSelectedItemsForSprintPlanning(backlogId);

        SprintPlanningCapacityDto capacity = new SprintPlanningCapacityDto();
        capacity.setBacklogItemCount(backlogService.countItemsByStatus(backlogId, "BACKLOG"));
        capacity.setSelectedItemCount(backlogService.countItemsByStatus(backlogId, "SELECTED"));
        capacity.setSprintItemCount(backlogService.countItemsByStatus(backlogId, "SPRINT"));
        capacity.setCompletedItemCount(backlogService.countItemsByStatus(backlogId, "COMPLETED"));
        capacity.setTotalStoryPointsForSelected(backlogService.getTotalStoryPointsForSelectedItems(backlogId));
        capacity.setTotalEstimatedEffortForSelected(backlogService.calculateTotalEffortForSelectedItems(backlogId));
        capacity.setSelectedItems(selectedItems.stream()
                .map(BacklogItemDto::fromEntity)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(ApiResponse.success(capacity));
    }

    /**
     * Reorder backlog items by priority (drag-and-drop)
     * Request body contains list of item IDs in desired priority order
     */
    @PutMapping("/{backlogId}/items/reorder")
    public ResponseEntity<ApiResponse<String>> reorderBacklogItems(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @RequestBody ReorderBacklogItemsRequest request) {

        backlogService.reorderBacklogItems(backlogId, request.getItemIds());
        return ResponseEntity.ok(ApiResponse.success("Items reordered successfully"));
    }

    /**
     * Get total story points for selected items
     */
    @GetMapping("/{backlogId}/selected-story-points")
    public ResponseEntity<ApiResponse<Integer>> getTotalStoryPointsForSelected(
            @PathVariable String projectId,
            @PathVariable String backlogId) {
        Integer total = backlogService.getTotalStoryPointsForSelectedItems(backlogId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    /**
     * Get total estimated effort for selected items
     */
    @GetMapping("/{backlogId}/selected-effort")
    public ResponseEntity<ApiResponse<Integer>> getTotalEffortForSelected(
            @PathVariable String projectId,
            @PathVariable String backlogId) {
        Integer total = backlogService.calculateTotalEffortForSelectedItems(backlogId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    /**
     * Count items by status
     */
    @GetMapping("/{backlogId}/item-count-by-status")
    public ResponseEntity<ApiResponse<Long>> countItemsByStatus(
            @PathVariable String projectId,
            @PathVariable String backlogId,
            @RequestParam String status) {
        long count = backlogService.countItemsByStatus(backlogId, status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
