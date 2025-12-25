package com.insuretech.pms.task.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tasks", description = "태스크 관리 API")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    @Operation(summary = "태스크 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Object>>> getAllTasks() {
        // TODO: Implement task service
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @Operation(summary = "태스크 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createTask(@RequestBody Object dto) {
        // TODO: Implement
        return ResponseEntity.ok(ApiResponse.success("태스크가 생성되었습니다", dto));
    }
}