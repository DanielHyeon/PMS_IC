package com.insuretech.pms.chat.controller;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.service.AuthService;
import com.insuretech.pms.chat.dto.*;
import com.insuretech.pms.chat.entity.ChatMessage;
import com.insuretech.pms.chat.entity.ChatSession;
import com.insuretech.pms.chat.service.ChatService;
import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Chat", description = "AI 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthService authService;

    @Operation(summary = "메시지 전송", description = "AI 챗봇에게 메시지를 전송합니다")
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
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

    @Operation(summary = "세션 목록 조회", description = "현재 사용자의 활성 세션 목록을 조회합니다")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionDto>>> getSessions() {
        User currentUser = authService.getCurrentUser();
        List<ChatSession> sessions = chatService.getUserSessions(currentUser.getId());
        List<ChatSessionDto> sessionDtos = sessions.stream()
                .map(ChatSessionDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(sessionDtos));
    }

    @Operation(summary = "새로운 대화 시작", description = "새로운 채팅 세션을 생성합니다")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSessionDto>> createSession(@Valid @RequestBody CreateSessionRequest request) {
        User currentUser = authService.getCurrentUser();
        ChatSession session = chatService.createNewSession(
                currentUser.getId(),
                request.getTitle()
        );
        return ResponseEntity.ok(ApiResponse.success(ChatSessionDto.from(session)));
    }

    @Operation(summary = "세션 제목 수정", description = "채팅 세션의 제목을 수정합니다")
    @PutMapping("/session/{sessionId}/title")
    public ResponseEntity<ApiResponse<ChatSessionDto>> updateSessionTitle(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateSessionTitleRequest request) {
        ChatSession session = chatService.updateSessionTitle(sessionId, request.getTitle());
        return ResponseEntity.ok(ApiResponse.success(ChatSessionDto.from(session)));
    }
}