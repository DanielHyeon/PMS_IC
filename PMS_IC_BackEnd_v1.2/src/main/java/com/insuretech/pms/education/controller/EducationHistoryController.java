package com.insuretech.pms.education.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.education.dto.EducationHistoryDto;
import com.insuretech.pms.education.service.EducationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Education History", description = "교육 수강 이력 관리 API")
@RestController
@RequestMapping("/api/education-histories")
@RequiredArgsConstructor
public class EducationHistoryController {

    private final EducationHistoryService historyService;

    @Operation(summary = "세션별 수강 이력 조회")
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<EducationHistoryDto>>> getHistoriesBySession(
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(historyService.getHistoriesBySession(sessionId)));
    }

    @Operation(summary = "참여자별 수강 이력 조회")
    @GetMapping("/participant/{participantId}")
    public ResponseEntity<ApiResponse<List<EducationHistoryDto>>> getHistoriesByParticipant(
            @PathVariable String participantId
    ) {
        return ResponseEntity.ok(ApiResponse.success(historyService.getHistoriesByParticipant(participantId)));
    }

    @Operation(summary = "교육 세션 참가 등록")
    @PostMapping("/session/{sessionId}/register")
    public ResponseEntity<ApiResponse<EducationHistoryDto>> registerParticipant(
            @PathVariable String sessionId,
            @RequestBody EducationHistoryDto dto
    ) {
        EducationHistoryDto created = historyService.registerParticipant(sessionId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("교육 참가가 등록되었습니다", created));
    }

    @Operation(summary = "수강 이력 수정 (완료 상태, 점수, 피드백)")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PutMapping("/{historyId}")
    public ResponseEntity<ApiResponse<EducationHistoryDto>> updateHistory(
            @PathVariable String historyId,
            @RequestBody EducationHistoryDto dto
    ) {
        EducationHistoryDto updated = historyService.updateHistory(historyId, dto);
        return ResponseEntity.ok(ApiResponse.success("수강 이력이 수정되었습니다", updated));
    }

    @Operation(summary = "교육 참가 취소")
    @DeleteMapping("/{historyId}")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(@PathVariable String historyId) {
        historyService.cancelRegistration(historyId);
        return ResponseEntity.ok(ApiResponse.success("교육 참가가 취소되었습니다", null));
    }
}
