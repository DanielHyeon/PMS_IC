package com.insuretech.pms.task.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.task.dto.WipValidationResult;
import com.insuretech.pms.task.service.WipValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "WIP Validation", description = "WIP 제한 검증 API")
@RestController
@RequestMapping("/api/wip")
@RequiredArgsConstructor
public class WipValidationController {

    private final WipValidationService wipValidationService;

    @Operation(summary = "칼럼 WIP 제한 검증",
               description = "지정된 칼럼의 WIP 제한(soft/hard)을 검증합니다")
    @GetMapping("/validate/column/{columnId}")
    public ResponseEntity<ApiResponse<WipValidationResult>> validateColumnWipLimit(
            @Parameter(description = "칼럼 ID")
            @PathVariable String columnId,
            @Parameter(description = "소프트 리밋 초과 허용 여부")
            @RequestParam(defaultValue = "false") boolean allowSoftLimitExceeding) {

        WipValidationResult result = wipValidationService.validateColumnWipLimit(columnId, allowSoftLimitExceeding);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "스프린트 CONWIP 검증",
               description = "스프린트의 CONWIP(Constant WIP) 제한을 검증합니다")
    @GetMapping("/validate/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<WipValidationResult>> validateSprintConwip(
            @Parameter(description = "스프린트 ID")
            @PathVariable String sprintId) {

        WipValidationResult result = wipValidationService.validateSprintConwip(sprintId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "개인 WIP 제한 검증",
               description = "지정된 사용자의 개인 WIP 제한을 검증합니다")
    @GetMapping("/validate/personal/{assigneeId}")
    public ResponseEntity<ApiResponse<WipValidationResult>> validatePersonalWipLimit(
            @Parameter(description = "담당자 ID")
            @PathVariable String assigneeId,
            @Parameter(description = "최대 개인 WIP 수량")
            @RequestParam(defaultValue = "5") int maxPersonalWip) {

        WipValidationResult result = wipValidationService.validatePersonalWipLimit(assigneeId, maxPersonalWip);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "프로젝트 WIP 상태 조회",
               description = "프로젝트의 전체 WIP 상태를 조회합니다")
    @GetMapping("/status/project/{projectId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectWipStatus(
            @Parameter(description = "프로젝트 ID")
            @PathVariable String projectId) {

        Map<String, Object> wipStatus = wipValidationService.getProjectWipStatus(projectId);
        return ResponseEntity.ok(ApiResponse.success(wipStatus));
    }

    @Operation(summary = "칼럼 WIP 현황 조회",
               description = "칼럼의 상세 WIP 현황을 조회합니다")
    @GetMapping("/status/column/{columnId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getColumnWipStatus(
            @Parameter(description = "칼럼 ID")
            @PathVariable String columnId) {

        Map<String, Object> columnStatus = wipValidationService.getColumnWipStatus(columnId);
        return ResponseEntity.ok(ApiResponse.success(columnStatus));
    }

    @Operation(summary = "스프린트 WIP 현황 조회",
               description = "스프린트의 상세 WIP 현황을 조회합니다")
    @GetMapping("/status/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSprintWipStatus(
            @Parameter(description = "스프린트 ID")
            @PathVariable String sprintId) {

        Map<String, Object> sprintStatus = wipValidationService.getSprintWipStatus(sprintId);
        return ResponseEntity.ok(ApiResponse.success(sprintStatus));
    }
}
