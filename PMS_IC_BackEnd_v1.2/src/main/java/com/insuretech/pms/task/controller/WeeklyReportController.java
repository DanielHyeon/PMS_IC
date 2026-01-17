package com.insuretech.pms.task.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.task.dto.WeeklyReportDto;
import com.insuretech.pms.task.service.WeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Weekly Reports", description = "주간 보고서 관리 API")
@RestController
@RequestMapping("/api/weekly-reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(summary = "프로젝트 주간 보고서 생성",
               description = "지정된 프로젝트와 주간에 대한 주간 보고서를 생성합니다")
    @PostMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> generateProjectWeeklyReport(
            @Parameter(description = "프로젝트 ID")
            @PathVariable String projectId,
            @Parameter(description = "주간 시작 날짜 (yyyy-MM-dd)")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate,
            @Parameter(description = "사용자 ID (감사용)")
            @RequestParam String userId) {

        WeeklyReportDto report = weeklyReportService.generateWeeklyReport(projectId, weekStartDate, userId);
        return ResponseEntity.ok(ApiResponse.success("주간 보고서가 생성되었습니다", report));
    }

    @Operation(summary = "스프린트 주간 보고서 생성",
               description = "지정된 스프린트에 대한 주간 보고서를 생성합니다")
    @PostMapping("/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> generateSprintWeeklyReport(
            @Parameter(description = "스프린트 ID")
            @PathVariable String sprintId,
            @Parameter(description = "사용자 ID (감사용)")
            @RequestParam String userId) {

        WeeklyReportDto report = weeklyReportService.generateSprintWeeklyReport(sprintId, userId);
        return ResponseEntity.ok(ApiResponse.success("스프린트 주간 보고서가 생성되었습니다", report));
    }

    @Operation(summary = "프로젝트 최근 주간 보고서 조회",
               description = "프로젝트의 최근 주간 보고서 목록을 조회합니다")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<WeeklyReportDto>>> getProjectReports(
            @Parameter(description = "프로젝트 ID")
            @PathVariable String projectId,
            @Parameter(description = "조회할 보고서 개수")
            @RequestParam(defaultValue = "10") int limit) {

        List<WeeklyReportDto> reports = weeklyReportService.getProjectReports(projectId, limit);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "스프린트 최근 주간 보고서 조회",
               description = "스프린트의 최근 주간 보고서 목록을 조회합니다")
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<List<WeeklyReportDto>>> getSprintReports(
            @Parameter(description = "스프린트 ID")
            @PathVariable String sprintId,
            @Parameter(description = "조회할 보고서 개수")
            @RequestParam(defaultValue = "10") int limit) {

        List<WeeklyReportDto> reports = weeklyReportService.getSprintReports(sprintId, limit);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "주간 보고서 상세 조회",
               description = "ID로 특정 주간 보고서의 상세 정보를 조회합니다")
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> getReportById(
            @Parameter(description = "보고서 ID")
            @PathVariable String reportId) {

        WeeklyReportDto report = weeklyReportService.getReportById(reportId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @Operation(summary = "주간 보고서 삭제",
               description = "지정된 주간 보고서를 삭제합니다")
    @DeleteMapping("/{reportId}")
    public ResponseEntity<ApiResponse<String>> deleteReport(
            @Parameter(description = "보고서 ID")
            @PathVariable String reportId) {

        weeklyReportService.deleteReport(reportId);
        return ResponseEntity.ok(ApiResponse.success("주간 보고서가 삭제되었습니다"));
    }
}
