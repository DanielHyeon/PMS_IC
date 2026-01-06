package com.insuretech.pms.chat.controller;

import com.insuretech.pms.chat.dto.ModelChangeRequest;
import com.insuretech.pms.chat.dto.ModelInfoResponse;
import com.insuretech.pms.chat.service.LlmService;
import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "LLM", description = "LLM 모델 관리 API")
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @Operation(summary = "현재 모델 조회", description = "현재 사용 중인 LLM 모델 정보를 조회합니다")
    @GetMapping("/model")
    public ResponseEntity<ApiResponse<ModelInfoResponse>> getCurrentModel() {
        ModelInfoResponse modelInfo = llmService.getCurrentModel();
        return ResponseEntity.ok(ApiResponse.success(modelInfo));
    }

    @Operation(summary = "모델 변경", description = "LLM 모델을 변경합니다 (관리자 전용)")
    @PutMapping("/model")
    public ResponseEntity<ApiResponse<ModelInfoResponse>> changeModel(@RequestBody ModelChangeRequest request) {
        ModelInfoResponse modelInfo = llmService.changeModel(request.getModelPath());
        return ResponseEntity.ok(ApiResponse.success("모델이 성공적으로 변경되었습니다", modelInfo));
    }

    @Operation(summary = "LLM 서비스 상태 확인", description = "LLM 서비스의 헬스 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> checkHealth() {
        Object healthStatus = llmService.checkHealth();
        return ResponseEntity.ok(ApiResponse.success(healthStatus));
    }
}
