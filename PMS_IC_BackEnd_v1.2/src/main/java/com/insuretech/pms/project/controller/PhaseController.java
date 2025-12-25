package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.PhaseDto;
import com.insuretech.pms.project.service.PhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Phases", description = "단계별 관리 API")
@RestController
@RequestMapping("/api/phases")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    @Operation(summary = "단계 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PhaseDto>>> getPhases(
            @RequestParam(value = "projectId", required = false) String projectId) {
        return ResponseEntity.ok(ApiResponse.success(phaseService.getPhases(projectId)));
    }

    @Operation(summary = "단계 상세 조회")
    @GetMapping("/{phaseId}")
    public ResponseEntity<ApiResponse<PhaseDto>> getPhase(@PathVariable String phaseId) {
        return ResponseEntity.ok(ApiResponse.success(phaseService.getPhaseById(phaseId)));
    }
}
