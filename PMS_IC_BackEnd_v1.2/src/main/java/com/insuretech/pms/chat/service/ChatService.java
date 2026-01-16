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
        User user = resolveCurrentUser();
        String userId = user != null ? user.getId() : "guest";
        
        ChatSession session = getOrCreateSession(request.getSessionId(), userId);
        ChatMessage userMessage = saveUserMessage(session, request.getMessage());

        List<ChatMessage> recentMessages = getRecentMessages(session.getId(), 10);
        ChatResponse aiResponse = callAIService(userId, request.getMessage(), recentMessages);

        ChatMessage assistantMessage = saveAssistantMessage(session, aiResponse.getReply());
        cacheMessages(session.getId(), userMessage, assistantMessage);

        aiResponse.setSessionId(session.getId());
        return aiResponse;
    }

    private User resolveCurrentUser() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            log.info("Processing chat request for unauthenticated user (guest)");
            return null;
        }
    }

    private ChatSession getOrCreateSession(String sessionId, String userId) {
        if (sessionId != null) {
            return chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> CustomException.notFound("채팅 세션을 찾을 수 없습니다"));
        }
        
        ChatSession newSession = ChatSession.builder()
                .userId(userId)
                .title("New Chat")
                .active(true)
                .build();
        return chatSessionRepository.save(newSession);
    }

    private ChatMessage saveUserMessage(ChatSession session, String message) {
        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.USER)
                .content(message)
                .build();
        return chatMessageRepository.save(userMessage);
    }

    private ChatResponse callAIService(String userId, String message, List<ChatMessage> recentMessages) {
        return aiChatClient.chat(userId, message, recentMessages);
    }

    private ChatMessage saveAssistantMessage(ChatSession session, String reply) {
        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.ASSISTANT)
                .content(reply)
                .build();
        return chatMessageRepository.save(assistantMessage);
    }

    private void cacheMessages(String sessionId, ChatMessage userMessage, ChatMessage assistantMessage) {
        String redisKey = "chat:session:" + sessionId;
        cacheMessage(redisKey, userMessage);
        cacheMessage(redisKey, assistantMessage);
    }

    private List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        int startIndex = Math.max(0, allMessages.size() - limit);
        return allMessages.subList(startIndex, allMessages.size());
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