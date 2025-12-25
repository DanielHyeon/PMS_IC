package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.DeliverableDto;
import com.insuretech.pms.project.service.DeliverableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Deliverables", description = "산출물 관리 API")
@RestController
@RequestMapping("/api/phases/{phaseId}/deliverables")
@RequiredArgsConstructor
public class DeliverableController {

    private final DeliverableService deliverableService;

    @Operation(summary = "단계별 산출물 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliverableDto>>> getDeliverables(@PathVariable String phaseId) {
        return ResponseEntity.ok(ApiResponse.success(deliverableService.getDeliverablesByPhase(phaseId)));
    }

    @Operation(summary = "산출물 업로드 (신규)")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")
    @PostMapping
    public ResponseEntity<ApiResponse<DeliverableDto>> uploadDeliverable(
            @PathVariable String phaseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "type", defaultValue = "DOCUMENT") String type,
            Authentication authentication
    ) {
        DeliverableDto dto = deliverableService.uploadDeliverable(
                phaseId,
                null,
                file,
                name,
                description,
                type,
                authentication != null ? authentication.getName() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("산출물이 업로드되었습니다", dto));
    }

    @Operation(summary = "산출물 파일 업로드/갱신")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")
    @PostMapping("/{deliverableId}/upload")
    public ResponseEntity<ApiResponse<DeliverableDto>> uploadExistingDeliverable(
            @PathVariable String phaseId,
            @PathVariable String deliverableId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "type", defaultValue = "DOCUMENT") String type,
            Authentication authentication
    ) {
        DeliverableDto dto = deliverableService.uploadDeliverable(
                phaseId,
                deliverableId,
                file,
                name,
                description,
                type,
                authentication != null ? authentication.getName() : null
        );
        return ResponseEntity.ok(ApiResponse.success("산출물이 업로드되었습니다", dto));
    }
}
