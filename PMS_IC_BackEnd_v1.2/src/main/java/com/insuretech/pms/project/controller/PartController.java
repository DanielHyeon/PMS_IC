package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.*;
import com.insuretech.pms.project.service.PartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Parts", description = "Sub-project (Part) management API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PartController {

    private final PartService partService;

    @Operation(summary = "Get all parts for a project")
    @GetMapping("/api/projects/{projectId}/parts")
    public ResponseEntity<ApiResponse<List<PartDto>>> getPartsByProject(@PathVariable String projectId) {
        List<PartDto> parts = partService.getPartsByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(parts));
    }

    @Operation(summary = "Get part by ID")
    @GetMapping("/api/parts/{partId}")
    public ResponseEntity<ApiResponse<PartDto>> getPartById(@PathVariable String partId) {
        PartDto part = partService.getPartById(partId);
        return ResponseEntity.ok(ApiResponse.success(part));
    }

    @Operation(summary = "Create a new part")
    @PostMapping("/api/projects/{projectId}/parts")
    public ResponseEntity<ApiResponse<PartDto>> createPart(
            @PathVariable String projectId,
            @Valid @RequestBody CreatePartRequest request) {
        PartDto part = partService.createPart(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Part created successfully", part));
    }

    @Operation(summary = "Update a part")
    @PutMapping("/api/parts/{partId}")
    public ResponseEntity<ApiResponse<PartDto>> updatePart(
            @PathVariable String partId,
            @RequestBody UpdatePartRequest request) {
        PartDto part = partService.updatePart(partId, request);
        return ResponseEntity.ok(ApiResponse.success("Part updated successfully", part));
    }

    @Operation(summary = "Delete a part")
    @DeleteMapping("/api/parts/{partId}")
    public ResponseEntity<ApiResponse<Void>> deletePart(@PathVariable String partId) {
        partService.deletePart(partId);
        return ResponseEntity.ok(ApiResponse.success("Part deleted successfully", null));
    }

    @Operation(summary = "Assign leader to a part")
    @PutMapping("/api/parts/{partId}/leader")
    public ResponseEntity<ApiResponse<PartDto>> assignLeader(
            @PathVariable String partId,
            @Valid @RequestBody AssignLeaderRequest request) {
        PartDto part = partService.assignLeader(partId, request);
        return ResponseEntity.ok(ApiResponse.success("Leader assigned successfully", part));
    }

    @Operation(summary = "Get part members")
    @GetMapping("/api/parts/{partId}/members")
    public ResponseEntity<ApiResponse<Set<String>>> getPartMembers(@PathVariable String partId) {
        Set<String> members = partService.getPartMembers(partId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @Operation(summary = "Add member to a part")
    @PostMapping("/api/parts/{partId}/members")
    public ResponseEntity<ApiResponse<PartDto>> addMember(
            @PathVariable String partId,
            @Valid @RequestBody PartMemberRequest request) {
        PartDto part = partService.addMember(partId, request);
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", part));
    }

    @Operation(summary = "Remove member from a part")
    @DeleteMapping("/api/parts/{partId}/members/{memberId}")
    public ResponseEntity<ApiResponse<PartDto>> removeMember(
            @PathVariable String partId,
            @PathVariable String memberId) {
        PartDto part = partService.removeMember(partId, memberId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", part));
    }
}
