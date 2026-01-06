package com.insuretech.pms.chat.controller;

import com.insuretech.pms.chat.dto.ChatRequest;
import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import com.insuretech.pms.chat.service.ChatService;
import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "AI 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "메시지 전송", description = "AI 챗봇에게 메시지를 전송합니다")
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대화 히스토리 조회", description = "세션의 대화 내역을 조회합니다")
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getHistory(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatService.getHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @Operation(summary = "세션 삭제", description = "채팅 세션을 삭제합니다")
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("세션이 삭제되었습니다", null));
    }
}