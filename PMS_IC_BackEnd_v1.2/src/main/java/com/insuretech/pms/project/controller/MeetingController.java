package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.MeetingDto;
import com.insuretech.pms.project.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Meetings", description = "회의 관리 API")
@RestController
@RequestMapping("/api/projects/{projectId}/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "프로젝트별 회의 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingDto>>> getMeetings(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(meetingService.getMeetingsByProject(projectId)));
    }

    @Operation(summary = "회의 상세 조회")
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingDto>> getMeeting(
            @PathVariable String projectId,
            @PathVariable String meetingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingService.getMeetingById(meetingId)));
    }

    @Operation(summary = "회의 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingDto>> createMeeting(
            @PathVariable String projectId,
            @RequestBody MeetingDto dto
    ) {
        MeetingDto created = meetingService.createMeeting(projectId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회의가 생성되었습니다", created));
    }

    @Operation(summary = "회의 수정")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @PutMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingDto>> updateMeeting(
            @PathVariable String projectId,
            @PathVariable String meetingId,
            @RequestBody MeetingDto dto
    ) {
        MeetingDto updated = meetingService.updateMeeting(projectId, meetingId, dto);
        return ResponseEntity.ok(ApiResponse.success("회의가 수정되었습니다", updated));
    }

    @Operation(summary = "회의 삭제")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @PathVariable String projectId,
            @PathVariable String meetingId
    ) {
        meetingService.deleteMeeting(projectId, meetingId);
        return ResponseEntity.ok(ApiResponse.success("회의가 삭제되었습니다", null));
    }
}
