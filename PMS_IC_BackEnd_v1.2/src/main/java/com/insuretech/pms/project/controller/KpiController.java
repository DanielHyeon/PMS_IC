package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.KpiDto;
import com.insuretech.pms.project.service.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "KPIs", description = "핵심 성과 지표 관리 API")
@RestController
@RequestMapping("/api/phases/{phaseId}/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @Operation(summary = "단계별 KPI 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<KpiDto>>> getKpis(@PathVariable String phaseId) {
        return ResponseEntity.ok(ApiResponse.success(kpiService.getKpisByPhase(phaseId)));
    }

    @Operation(summary = "KPI 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @PostMapping
    public ResponseEntity<ApiResponse<KpiDto>> createKpi(
            @PathVariable String phaseId,
            @RequestBody KpiDto dto
    ) {
        KpiDto created = kpiService.createKpi(phaseId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("KPI가 생성되었습니다", created));
    }

    @Operation(summary = "KPI 수정")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @PutMapping("/{kpiId}")
    public ResponseEntity<ApiResponse<KpiDto>> updateKpi(
            @PathVariable String phaseId,
            @PathVariable String kpiId,
            @RequestBody KpiDto dto
    ) {
        KpiDto updated = kpiService.updateKpi(phaseId, kpiId, dto);
        return ResponseEntity.ok(ApiResponse.success("KPI가 수정되었습니다", updated));
    }

    @Operation(summary = "KPI 삭제")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @DeleteMapping("/{kpiId}")
    public ResponseEntity<ApiResponse<Void>> deleteKpi(
            @PathVariable String phaseId,
            @PathVariable String kpiId
    ) {
        kpiService.deleteKpi(phaseId, kpiId);
        return ResponseEntity.ok(ApiResponse.success("KPI가 삭제되었습니다", null));
    }
}
