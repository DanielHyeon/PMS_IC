package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.*;
import com.insuretech.pms.project.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Project Members", description = "Project member management API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(summary = "Get all members of a project")
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectMemberDto>>> getProjectMembers(
            @PathVariable String projectId) {
        List<ProjectMemberDto> members = projectMemberService.getProjectMembers(projectId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @Operation(summary = "Get a specific project member")
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId)")
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<ProjectMemberDto>> getProjectMember(
            @PathVariable String projectId,
            @PathVariable String memberId) {
        ProjectMemberDto member = projectMemberService.getProjectMember(projectId, memberId);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @Operation(summary = "Add a member to the project")
    @PreAuthorize("@projectSecurity.canManageMembers(#projectId)")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectMemberDto>> addProjectMember(
            @PathVariable String projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        ProjectMemberDto member = projectMemberService.addProjectMember(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member added successfully", member));
    }

    @Operation(summary = "Update a project member's role")
    @PreAuthorize("@projectSecurity.canManageMembers(#projectId)")
    @PutMapping("/{memberId}")
    public ResponseEntity<ApiResponse<ProjectMemberDto>> updateProjectMember(
            @PathVariable String projectId,
            @PathVariable String memberId,
            @RequestBody UpdateProjectMemberRequest request) {
        ProjectMemberDto member = projectMemberService.updateProjectMember(projectId, memberId, request);
        return ResponseEntity.ok(ApiResponse.success("Member updated successfully", member));
    }

    @Operation(summary = "Remove a member from the project")
    @PreAuthorize("@projectSecurity.canManageMembers(#projectId)")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeProjectMember(
            @PathVariable String projectId,
            @PathVariable String memberId) {
        projectMemberService.removeProjectMember(projectId, memberId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }
}
