package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.ProjectDto;
import com.insuretech.pms.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Projects", description = "프로젝트 관리 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getAllProjects() {
        List<ProjectDto> projects = projectService.getAllProjects();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @Operation(summary = "프로젝트 상세 조회")
    @PreAuthorize("@projectSecurity.isProjectMember(#id) or @projectSecurity.hasSystemRole('ADMIN') or @projectSecurity.hasSystemRole('AUDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProjectById(@PathVariable String id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @Operation(summary = "프로젝트 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(@RequestBody ProjectDto dto) {
        ProjectDto created = projectService.createProject(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로젝트가 생성되었습니다", created));
    }

    @Operation(summary = "프로젝트 수정")
    @PreAuthorize("@projectSecurity.hasAnyRole(#id, 'PMO_HEAD', 'PM', 'SPONSOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @PathVariable String id,
            @RequestBody ProjectDto dto) {
        ProjectDto updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 수정되었습니다", updated));
    }

    @Operation(summary = "프로젝트 삭제")
    @PreAuthorize("@projectSecurity.hasRole(#id, 'PMO_HEAD') or @projectSecurity.hasSystemRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 삭제되었습니다", null));
    }
}