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
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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
    private final ProjectDataService projectDataService;

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        // Correlation ID 설정
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Processing chat message [correlationId: {}, userId: {}]", correlationId, currentUser.getId());

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

        // 기존 대화 맥락 조회 (스마트 컨텍스트 선택)
        List<ChatMessage> contextMessages = getContextMessages(session.getId(), 10);
        log.debug("Retrieved {} context messages for session {}", contextMessages.size(), session.getId());

        // 프로젝트 관련 질문인지 확인하고 데이터 조회
        String projectData = null;
        if (projectDataService.isProjectRelatedQuery(request.getMessage())) {
            log.info("Project-related query detected, retrieving project data [correlationId: {}]", correlationId);
            
            // 프로젝트 ID가 명시되어 있으면 해당 프로젝트만, 없으면 전체 프로젝트
            String projectId = projectDataService.extractProjectId(request.getMessage());
            if (projectId != null) {
                projectData = projectDataService.getProjectSummary(projectId);
                log.info("Retrieved specific project data: {}", projectId);
            } else {
                projectData = projectDataService.getAllProjectsSummary();
                log.info("Retrieved all projects data");
            }
        }

        // 세션 제목 자동 생성/업데이트 (첫 메시지인 경우)
        if (contextMessages.size() <= 2) { // 사용자 메시지 + AI 응답만 있는 경우
            updateSessionTitle(session, request.getMessage());
        }

        // AI 서비스 호출 (프로젝트 데이터 포함)
        ChatResponse aiResponse = aiChatClient.chat(
            currentUser.getId(), 
            request.getMessage(), 
            contextMessages,
            projectData
        );

        // AI 응답 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.ASSISTANT)
                .content(aiResponse.getReply())
                .build();
        chatMessageRepository.save(assistantMessage);

        // Redis에 캐싱 (1시간) - 임시 비활성화 (LocalDateTime 직렬화 문제)
        // cacheMessage(redisKey, userMessage);
        // cacheMessage(redisKey, assistantMessage);

            aiResponse.setSessionId(session.getId());
            log.info("Chat message processed successfully [correlationId: {}, sessionId: {}]", 
                    correlationId, session.getId());
            return aiResponse;
        } catch (Exception e) {
            log.error("Error processing chat message [correlationId: {}]: {}", correlationId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * 세션의 최근 메시지 조회 (최적화)
     * 컨텍스트를 위해 최근 N개 메시지를 가져옴
     */
    private List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        // 전체 메시지 개수 확인
        long totalCount = chatMessageRepository.countBySessionId(sessionId);
        
        if (totalCount == 0) {
            return List.of();
        }
        
        // 최근 limit개만 필요하면 마지막 limit개만 조회
        if (totalCount <= limit) {
            return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        }
        
        // 전체 메시지 중 마지막 limit개만 가져오기
        List<ChatMessage> allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        int startIndex = (int) (totalCount - limit);
        return allMessages.subList(startIndex, allMessages.size());
    }
    
    /**
     * 컨텍스트를 위한 메시지 조회 (스마트 선택)
     * 최근 메시지와 함께 초기 대화 맥락도 포함
     */
    private List<ChatMessage> getContextMessages(String sessionId, int recentLimit) {
        List<ChatMessage> allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        if (allMessages.isEmpty()) {
            return List.of();
        }
        
        // 메시지가 적으면 모두 반환
        if (allMessages.size() <= recentLimit) {
            return allMessages;
        }
        
        // 최근 메시지 + 초기 대화 맥락 (처음 3개)
        List<ChatMessage> contextMessages = new java.util.ArrayList<>();
        
        // 초기 대화 맥락 (처음 3개)
        int initialContextSize = Math.min(3, allMessages.size());
        contextMessages.addAll(allMessages.subList(0, initialContextSize));
        
        // 최근 메시지 (마지막 recentLimit개)
        int startIndex = Math.max(initialContextSize, allMessages.size() - recentLimit);
        List<ChatMessage> recentMessages = allMessages.subList(startIndex, allMessages.size());
        
        // 중복 제거 및 병합
        contextMessages.addAll(recentMessages);
        return contextMessages.stream()
                .distinct()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // Redis 캐싱은 현재 비활성화되어 있음 (LocalDateTime 직렬화 문제)
    // 향후 필요시 재활성화
    @SuppressWarnings("unused")
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

    /**
     * 사용자의 활성 세션 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatSession> getUserSessions(String userId) {
        return chatSessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    /**
     * 새로운 대화 세션 생성
     */
    @Transactional
    public ChatSession createNewSession(String userId, String title) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title(title != null ? title : "New Chat")
                .active(true)
                .build();
        return chatSessionRepository.save(session);
    }

    /**
     * 세션 제목 자동 생성/업데이트
     */
    private void updateSessionTitle(ChatSession session, String firstMessage) {
        if (session.getTitle() == null || session.getTitle().equals("New Chat")) {
            // 첫 메시지의 앞부분을 제목으로 사용 (최대 30자)
            String title = firstMessage.length() > 30 
                    ? firstMessage.substring(0, 30) + "..." 
                    : firstMessage;
            session.setTitle(title);
            chatSessionRepository.save(session);
            log.debug("Updated session title: {}", title);
        }
    }

    /**
     * 세션 제목 수동 업데이트
     */
    @Transactional
    public ChatSession updateSessionTitle(String sessionId, String title) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> CustomException.notFound("채팅 세션을 찾을 수 없습니다"));
        
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }
}