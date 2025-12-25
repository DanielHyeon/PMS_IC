package com.insuretech.pms.chat.service;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.service.AuthService;
import com.insuretech.pms.chat.dto.ChatRequest;
import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import com.insuretech.pms.chat.entity.ChatSession;
import com.insuretech.pms.chat.repository.ChatMessageRepository;
import com.insuretech.pms.chat.repository.ChatSessionRepository;
import com.insuretech.pms.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AIChatClient aiChatClient;
    private final AuthService authService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        User currentUser = authService.getCurrentUser();

        // 세션 조회 또는 생성
        ChatSession session;
        if (request.getSessionId() != null) {
            session = chatSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> CustomException.notFound("채팅 세션을 찾을 수 없습니다"));
        } else {
            session = ChatSession.builder()
                    .userId(currentUser.getId())
                    .title("New Chat")
                    .active(true)
                    .build();
            session = chatSessionRepository.save(session);
        }

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMessage);

        // Redis에서 최근 대화 조회
        String redisKey = "chat:session:" + session.getId();
        List<ChatMessage> recentMessages = getRecentMessages(session.getId(), 10);

        // AI 서비스 호출
        ChatResponse aiResponse = aiChatClient.chat(currentUser.getId(), request.getMessage(), recentMessages);

        // AI 응답 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.ASSISTANT)
                .content(aiResponse.getReply())
                .build();
        chatMessageRepository.save(assistantMessage);

        // Redis에 캐싱 (1시간)
        cacheMessage(redisKey, userMessage);
        cacheMessage(redisKey, assistantMessage);

        aiResponse.setSessionId(session.getId());
        return aiResponse;
    }

    private List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .skip(Math.max(0, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).size() - limit))
                .collect(Collectors.toList());
    }

    private void cacheMessage(String redisKey, ChatMessage message) {
        redisTemplate.opsForList().rightPush(redisKey, message);
        redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void deleteSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> CustomException.notFound("채팅 세션을 찾을 수 없습니다"));

        session.setActive(false);
        chatSessionRepository.save(session);

        // Redis에서도 삭제
        String redisKey = "chat:session:" + sessionId;
        redisTemplate.delete(redisKey);
    }
}