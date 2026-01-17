package com.insuretech.pms.rfp.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.rfp.dto.*;
import com.insuretech.pms.rfp.service.RfpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/rfps")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RfpController {

    private final RfpService rfpService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RfpDto>>> getRfps(@PathVariable String projectId) {
        List<RfpDto> rfps = rfpService.getRfpsByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(rfps));
    }

    @GetMapping("/{rfpId}")
    public ResponseEntity<ApiResponse<RfpDto>> getRfp(
            @PathVariable String projectId,
            @PathVariable String rfpId) {
        RfpDto rfp = rfpService.getRfpById(projectId, rfpId);
        return ResponseEntity.ok(ApiResponse.success(rfp));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RfpDto>> createRfp(
            @PathVariable String projectId,
            @RequestBody CreateRfpRequest request) {
        RfpDto rfp = rfpService.createRfp(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(rfp));
    }

    @PutMapping("/{rfpId}")
    public ResponseEntity<ApiResponse<RfpDto>> updateRfp(
            @PathVariable String projectId,
            @PathVariable String rfpId,
            @RequestBody UpdateRfpRequest request) {
        RfpDto rfp = rfpService.updateRfp(projectId, rfpId, request);
        return ResponseEntity.ok(ApiResponse.success(rfp));
    }

    @DeleteMapping("/{rfpId}")
    public ResponseEntity<Void> deleteRfp(
            @PathVariable String projectId,
            @PathVariable String rfpId) {
        rfpService.deleteRfp(projectId, rfpId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RfpDto>> uploadRfp(
            @PathVariable String projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        try {
            RfpDto rfp = rfpService.uploadRfpFile(projectId, file, title);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(rfp));
        } catch (IOException e) {
            log.error("Failed to upload RFP file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping("/{rfpId}/extract")
    public ResponseEntity<ApiResponse<RfpDto>> extractRequirements(
            @PathVariable String projectId,
            @PathVariable String rfpId,
            @RequestBody(required = false) ExtractRequirementsRequest request) {
        RfpDto rfp = rfpService.startExtraction(projectId, rfpId);
        return ResponseEntity.ok(ApiResponse.success(rfp));
    }

    @GetMapping("/{rfpId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessingStatus(
            @PathVariable String projectId,
            @PathVariable String rfpId) {
        RfpDto rfp = rfpService.getRfpById(projectId, rfpId);
        Map<String, Object> status = Map.of(
                "rfpId", rfp.getId(),
                "status", rfp.getProcessingStatus().name(),
                "requirementCount", rfp.getRequirementCount(),
                "message", rfp.getProcessingMessage() != null ? rfp.getProcessingMessage() : ""
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RfpDto>>> searchRfps(
            @PathVariable String projectId,
            @RequestParam String keyword) {
        List<RfpDto> rfps = rfpService.searchRfps(projectId, keyword);
        return ResponseEntity.ok(ApiResponse.success(rfps));
    }
}
