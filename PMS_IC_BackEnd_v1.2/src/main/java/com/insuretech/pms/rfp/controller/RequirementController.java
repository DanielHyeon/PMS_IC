package com.insuretech.pms.rfp.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.rfp.dto.*;
import com.insuretech.pms.rfp.service.RequirementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/requirements")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RequirementController {

    private final RequirementService requirementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RequirementDto>>> getRequirements(
            @PathVariable String projectId) {
        List<RequirementDto> requirements = requirementService.getRequirementsByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(requirements));
    }

    @GetMapping("/{requirementId}")
    public ResponseEntity<ApiResponse<RequirementDto>> getRequirement(
            @PathVariable String projectId,
            @PathVariable String requirementId) {
        RequirementDto requirement = requirementService.getRequirementById(projectId, requirementId);
        return ResponseEntity.ok(ApiResponse.success(requirement));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RequirementDto>> createRequirement(
            @PathVariable String projectId,
            @RequestBody CreateRequirementRequest request) {
        RequirementDto requirement = requirementService.createRequirement(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(requirement));
    }

    @PutMapping("/{requirementId}")
    public ResponseEntity<ApiResponse<RequirementDto>> updateRequirement(
            @PathVariable String projectId,
            @PathVariable String requirementId,
            @RequestBody UpdateRequirementRequest request) {
        RequirementDto requirement = requirementService.updateRequirement(projectId, requirementId, request);
        return ResponseEntity.ok(ApiResponse.success(requirement));
    }

    @DeleteMapping("/{requirementId}")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable String projectId,
            @PathVariable String requirementId) {
        requirementService.deleteRequirement(projectId, requirementId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requirementId}/link-task")
    public ResponseEntity<ApiResponse<RequirementDto>> linkTask(
            @PathVariable String projectId,
            @PathVariable String requirementId,
            @RequestBody LinkTaskRequest request) {
        RequirementDto requirement = requirementService.linkTask(projectId, requirementId, request.getTaskId());
        return ResponseEntity.ok(ApiResponse.success(requirement));
    }

    @DeleteMapping("/{requirementId}/unlink-task/{taskId}")
    public ResponseEntity<Void> unlinkTask(
            @PathVariable String projectId,
            @PathVariable String requirementId,
            @PathVariable String taskId) {
        requirementService.unlinkTask(projectId, requirementId, taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RequirementDto>>> searchRequirements(
            @PathVariable String projectId,
            @RequestParam String keyword) {
        List<RequirementDto> requirements = requirementService.searchRequirements(projectId, keyword);
        return ResponseEntity.ok(ApiResponse.success(requirements));
    }
}
