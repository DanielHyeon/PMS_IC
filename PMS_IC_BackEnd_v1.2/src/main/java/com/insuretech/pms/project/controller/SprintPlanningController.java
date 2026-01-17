package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.BacklogItemDto;
import com.insuretech.pms.project.dto.CreateSprintRequest;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.service.SprintPlanningService;
import com.insuretech.pms.task.dto.SprintDto;
import com.insuretech.pms.task.entity.Sprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Sprint Planning operations
 *
 * Handles:
 * - Creating sprints from selected backlog items
 * - Moving items between backlog and sprint
 * - Sprint capacity management
 * - Sprint status transitions
 */
@RestController
@RequestMapping("/api/projects/{projectId}/sprint-planning")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SprintPlanningController {

    private final SprintPlanningService sprintPlanningService;

    /**
     * Create sprint from selected backlog items
     */
    @PostMapping("/create-from-selected")
    public ResponseEntity<ApiResponse<SprintDto>> createSprintFromSelected(
            @PathVariable String projectId,
            @RequestParam String backlogId,
            @RequestBody CreateSprintRequest request) {

        Sprint sprint = sprintPlanningService.createSprintFromSelectedItems(
                projectId,
                request.getSprintName(),
                request.getSprintGoal(),
                request.getStartDate(),
                request.getEndDate(),
                backlogId
        );

        return ResponseEntity.ok(ApiResponse.success(SprintDto.fromEntity(sprint)));
    }

    /**
     * Move selected items to sprint manually
     * Items must already be in SELECTED status
     */
    @PostMapping("/{sprintId}/move-items")
    public ResponseEntity<ApiResponse<List<BacklogItemDto>>> moveItemsToSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId,
            @RequestParam String backlogId) {

        List<BacklogItem> movedItems = sprintPlanningService.moveItemsToSprint(backlogId, sprintId);

        List<BacklogItemDto> itemDtos = movedItems.stream()
                .map(BacklogItemDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(itemDtos));
    }

    /**
     * Get all items in a sprint
     */
    @GetMapping("/{sprintId}/items")
    public ResponseEntity<ApiResponse<List<BacklogItemDto>>> getSprintItems(
            @PathVariable String projectId,
            @PathVariable String sprintId) {

        List<BacklogItem> items = sprintPlanningService.getSprintItems(sprintId);

        List<BacklogItemDto> itemDtos = items.stream()
                .map(BacklogItemDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(itemDtos));
    }

    /**
     * Remove item from sprint (move back to SELECTED status)
     */
    @PostMapping("/items/{itemId}/remove-from-sprint")
    public ResponseEntity<ApiResponse<BacklogItemDto>> removeItemFromSprint(
            @PathVariable String projectId,
            @PathVariable String itemId) {

        BacklogItem item = sprintPlanningService.removeItemFromSprint(itemId);

        return ResponseEntity.ok(ApiResponse.success(BacklogItemDto.fromEntity(item)));
    }

    /**
     * Get sprint by ID
     */
    @GetMapping("/{sprintId}")
    public ResponseEntity<ApiResponse<SprintDto>> getSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId) {

        Sprint sprint = sprintPlanningService.getSprint(sprintId);

        return ResponseEntity.ok(ApiResponse.success(SprintDto.fromEntity(sprint)));
    }

    /**
     * Get active sprint for project
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SprintDto>> getActiveSprint(
            @PathVariable String projectId) {

        return sprintPlanningService.getActiveSprint(projectId)
                .map(sprint -> ResponseEntity.ok(ApiResponse.success(SprintDto.fromEntity(sprint))))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    /**
     * Get all sprints for project
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<SprintDto>>> getProjectSprints(
            @PathVariable String projectId) {

        List<Sprint> sprints = sprintPlanningService.getProjectSprints(projectId);

        List<SprintDto> sprintDtos = sprints.stream()
                .map(SprintDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(sprintDtos));
    }

    /**
     * Complete sprint (transition to COMPLETED status)
     */
    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<ApiResponse<SprintDto>> completeSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId) {

        Sprint sprint = sprintPlanningService.completeSprint(sprintId);

        return ResponseEntity.ok(ApiResponse.success(SprintDto.fromEntity(sprint)));
    }

    /**
     * Get sprint capacity metrics
     */
    @GetMapping("/{sprintId}/capacity")
    public ResponseEntity<ApiResponse<SprintCapacityResponse>> getSprintCapacity(
            @PathVariable String projectId,
            @PathVariable String sprintId) {

        SprintPlanningService.SprintCapacityMetrics metrics = sprintPlanningService.getSprintCapacity(sprintId);

        SprintCapacityResponse response = new SprintCapacityResponse(
                metrics.getSprintId(),
                metrics.getTotalItems(),
                metrics.getTotalStoryPoints(),
                metrics.getTotalEffortHours(),
                metrics.getCompletedItems(),
                metrics.getRemainingItems()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Response DTO for sprint capacity metrics
     */
    public static class SprintCapacityResponse {
        private final String sprintId;
        private final int totalItems;
        private final int totalStoryPoints;
        private final int totalEffortHours;
        private final long completedItems;
        private final long remainingItems;

        public SprintCapacityResponse(String sprintId, int totalItems, int totalStoryPoints,
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
