package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.IssueDto;
import com.insuretech.pms.project.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Issues", description = "이슈 관리 API")
@RestController
@RequestMapping("/api/projects/{projectId}/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @Operation(summary = "프로젝트별 이슈 조회")
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<IssueDto>>> getIssues(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(issueService.getIssuesByProject(projectId)));
    }

    @Operation(summary = "이슈 상세 조회")
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId)")
    @GetMapping("/{issueId}")
    public ResponseEntity<ApiResponse<IssueDto>> getIssue(
            @PathVariable String projectId,
            @PathVariable String issueId
    ) {
        return ResponseEntity.ok(ApiResponse.success(issueService.getIssueById(issueId)));
    }

    @Operation(summary = "이슈 생성")
    @PreAuthorize("@projectSecurity.canWorkOnIssues(#projectId)")
    @PostMapping
    public ResponseEntity<ApiResponse<IssueDto>> createIssue(
            @PathVariable String projectId,
            @RequestBody IssueDto dto
    ) {
        IssueDto created = issueService.createIssue(projectId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이슈가 생성되었습니다", created));
    }

    @Operation(summary = "이슈 수정")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectId, 'PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")
    @PutMapping("/{issueId}")
    public ResponseEntity<ApiResponse<IssueDto>> updateIssue(
            @PathVariable String projectId,
            @PathVariable String issueId,
            @RequestBody IssueDto dto
    ) {
        IssueDto updated = issueService.updateIssue(projectId, issueId, dto);
        return ResponseEntity.ok(ApiResponse.success("이슈가 수정되었습니다", updated));
    }

    @Operation(summary = "이슈 상태 변경")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectId, 'PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")
    @PatchMapping("/{issueId}/status")
    public ResponseEntity<ApiResponse<IssueDto>> updateIssueStatus(
            @PathVariable String projectId,
            @PathVariable String issueId,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        IssueDto updated = issueService.updateIssueStatus(projectId, issueId, status);
        return ResponseEntity.ok(ApiResponse.success("이슈 상태가 변경되었습니다", updated));
    }

    @Operation(summary = "이슈 삭제")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectId, 'PMO_HEAD', 'PM')")
    @DeleteMapping("/{issueId}")
    public ResponseEntity<ApiResponse<Void>> deleteIssue(
            @PathVariable String projectId,
            @PathVariable String issueId
    ) {
        issueService.deleteIssue(projectId, issueId);
        return ResponseEntity.ok(ApiResponse.success("이슈가 삭제되었습니다", null));
    }
}
