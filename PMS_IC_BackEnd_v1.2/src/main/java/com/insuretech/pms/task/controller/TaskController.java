package com.insuretech.pms.task.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.task.dto.TaskRequest;
import com.insuretech.pms.task.dto.TaskResponse;
import com.insuretech.pms.task.service.TaskService;
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

    private final TaskService taskService;

    @Operation(summary = "태스크 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks() {
        List<TaskResponse> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @Operation(summary = "태스크 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@RequestBody TaskRequest request) {
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success("태스크가 생성되었습니다", task));
    }
}