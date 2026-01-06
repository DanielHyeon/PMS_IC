package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.DeliverableApprovalRequest;
import com.insuretech.pms.project.dto.DeliverableDto;
import com.insuretech.pms.project.entity.Deliverable;
import com.insuretech.pms.project.service.DeliverableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Deliverable Actions", description = "산출물 승인/다운로드 API")
@RestController
@RequestMapping("/api/deliverables")
@RequiredArgsConstructor
public class DeliverableActionController {

    private final DeliverableService deliverableService;

    @Operation(summary = "산출물 승인/반려")
    @PreAuthorize("hasAnyRole('SPONSOR', 'PMO_HEAD', 'PM')")
    @PostMapping("/{deliverableId}/approval")
    public ResponseEntity<ApiResponse<DeliverableDto>> approveDeliverable(
            @PathVariable String deliverableId,
            @RequestBody DeliverableApprovalRequest request,
            Authentication authentication
    ) {
        DeliverableDto dto = deliverableService.approveDeliverable(
                deliverableId,
                request.isApproved(),
                authentication != null ? authentication.getName() : null
        );
        return ResponseEntity.ok(ApiResponse.success("산출물 상태가 변경되었습니다", dto));
    }

    @Operation(summary = "산출물 다운로드")
    @GetMapping("/{deliverableId}/download")
    public ResponseEntity<Resource> downloadDeliverable(@PathVariable String deliverableId) {
        Deliverable deliverable = deliverableService.getDeliverable(deliverableId);
        Resource resource = deliverableService.loadDeliverableFile(deliverable);
        String fileName = deliverable.getFileName() != null ? deliverable.getFileName() : "deliverable";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
