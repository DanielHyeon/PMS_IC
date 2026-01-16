package com.insuretech.pms.education.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.education.dto.EducationDto;
import com.insuretech.pms.education.dto.EducationRoadmapDto;
import com.insuretech.pms.education.dto.EducationSessionDto;
import com.insuretech.pms.education.service.EducationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Education", description = "교육 관리 API")
@RestController
@RequestMapping("/api/educations")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    // ========== Education CRUD ==========

    @Operation(summary = "모든 교육 과정 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EducationDto>>> getAllEducations() {
        return ResponseEntity.ok(ApiResponse.success(educationService.getAllEducations()));
    }

    @Operation(summary = "교육 과정 상세 조회")
    @GetMapping("/{educationId}")
    public ResponseEntity<ApiResponse<EducationDto>> getEducation(@PathVariable String educationId) {
        return ResponseEntity.ok(ApiResponse.success(educationService.getEducationById(educationId)));
    }

    @Operation(summary = "교육 과정 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EducationDto>> createEducation(@RequestBody EducationDto dto) {
        EducationDto created = educationService.createEducation(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("교육 과정이 생성되었습니다", created));
    }

    @Operation(summary = "교육 과정 수정")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PutMapping("/{educationId}")
    public ResponseEntity<ApiResponse<EducationDto>> updateEducation(
            @PathVariable String educationId,
            @RequestBody EducationDto dto
    ) {
        EducationDto updated = educationService.updateEducation(educationId, dto);
        return ResponseEntity.ok(ApiResponse.success("교육 과정이 수정되었습니다", updated));
    }

    @Operation(summary = "교육 과정 삭제 (비활성화)")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @DeleteMapping("/{educationId}")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(@PathVariable String educationId) {
        educationService.deleteEducation(educationId);
        return ResponseEntity.ok(ApiResponse.success("교육 과정이 삭제되었습니다", null));
    }

    // ========== Session CRUD ==========

    @Operation(summary = "교육 과정별 세션 조회")
    @GetMapping("/{educationId}/sessions")
    public ResponseEntity<ApiResponse<List<EducationSessionDto>>> getSessions(@PathVariable String educationId) {
        return ResponseEntity.ok(ApiResponse.success(educationService.getSessionsByEducation(educationId)));
    }

    @Operation(summary = "교육 세션 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PostMapping("/{educationId}/sessions")
    public ResponseEntity<ApiResponse<EducationSessionDto>> createSession(
            @PathVariable String educationId,
            @RequestBody EducationSessionDto dto
    ) {
        EducationSessionDto created = educationService.createSession(educationId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("교육 세션이 생성되었습니다", created));
    }

    @Operation(summary = "교육 세션 수정")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PutMapping("/{educationId}/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<EducationSessionDto>> updateSession(
            @PathVariable String educationId,
            @PathVariable String sessionId,
            @RequestBody EducationSessionDto dto
    ) {
        EducationSessionDto updated = educationService.updateSession(educationId, sessionId, dto);
        return ResponseEntity.ok(ApiResponse.success("교육 세션이 수정되었습니다", updated));
    }

    @Operation(summary = "교육 세션 삭제")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @DeleteMapping("/{educationId}/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable String educationId,
            @PathVariable String sessionId
    ) {
        educationService.deleteSession(educationId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("교육 세션이 삭제되었습니다", null));
    }

    // ========== Roadmap ==========

    @Operation(summary = "모든 교육 로드맵 조회")
    @GetMapping("/roadmaps")
    public ResponseEntity<ApiResponse<List<EducationRoadmapDto>>> getAllRoadmaps() {
        return ResponseEntity.ok(ApiResponse.success(educationService.getAllRoadmaps()));
    }

    @Operation(summary = "역할별 교육 로드맵 조회")
    @GetMapping("/roadmaps/role/{role}")
    public ResponseEntity<ApiResponse<List<EducationRoadmapDto>>> getRoadmapsByRole(@PathVariable String role) {
        return ResponseEntity.ok(ApiResponse.success(educationService.getRoadmapsByRole(role)));
    }

    @Operation(summary = "교육 로드맵 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PostMapping("/roadmaps")
    public ResponseEntity<ApiResponse<EducationRoadmapDto>> createRoadmap(@RequestBody EducationRoadmapDto dto) {
        EducationRoadmapDto created = educationService.createRoadmap(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("교육 로드맵이 생성되었습니다", created));
    }

    @Operation(summary = "교육 로드맵 수정")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @PutMapping("/roadmaps/{roadmapId}")
    public ResponseEntity<ApiResponse<EducationRoadmapDto>> updateRoadmap(
            @PathVariable String roadmapId,
            @RequestBody EducationRoadmapDto dto
    ) {
        EducationRoadmapDto updated = educationService.updateRoadmap(roadmapId, dto);
        return ResponseEntity.ok(ApiResponse.success("교육 로드맵이 수정되었습니다", updated));
    }

    @Operation(summary = "교육 로드맵 삭제")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'ADMIN')")
    @DeleteMapping("/roadmaps/{roadmapId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoadmap(@PathVariable String roadmapId) {
        educationService.deleteRoadmap(roadmapId);
        return ResponseEntity.ok(ApiResponse.success("교육 로드맵이 삭제되었습니다", null));
    }
}
