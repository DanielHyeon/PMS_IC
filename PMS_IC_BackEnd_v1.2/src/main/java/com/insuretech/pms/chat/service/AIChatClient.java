package com.insuretech.pms.chat.service;

import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import com.insuretech.pms.rag.service.RAGSearchService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatClient {

    @Qualifier("aiServiceWebClient")
    private final WebClient aiServiceWebClient;
    private final WebClient.Builder webClientBuilder;
    private final RAGSearchService ragSearchService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.mock-url}")
    private String aiMockUrl;

    @Value("${ai.service.model:llama3}")
    private String aiModel;

    @CircuitBreaker(name = "llm-service", fallbackMethod = "chatFallback")
    @Retry(name = "llm-service")
    @TimeLimiter(name = "llm-service")
    public CompletableFuture<ChatResponse> chatAsync(String userId, String message, List<ChatMessage> context) {
        return chatAsync(userId, message, context, null);
    }

    @CircuitBreaker(name = "llm-service", fallbackMethod = "chatFallback")
    @Retry(name = "llm-service")
    @TimeLimiter(name = "llm-service")
    public CompletableFuture<ChatResponse> chatAsync(String userId, String message, List<ChatMessage> context, String projectData) {
        String correlationId = getOrCreateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            log.info("Processing chat request [correlationId: {}]", correlationId);
            return CompletableFuture.completedFuture(callFlaskLLM(message, context, projectData));
        } catch (Exception e) {
            log.warn("Flask LLM service call failed [correlationId: {}], falling back to mock: {}", 
                    correlationId, e.getMessage());
            return CompletableFuture.completedFuture(chatFallback(userId, message, context, e));
        } finally {
            MDC.clear();
        }
    }

    public ChatResponse chat(String userId, String message, List<ChatMessage> context) {
        return chat(userId, message, context, null);
    }

    public ChatResponse chat(String userId, String message, List<ChatMessage> context, String projectData) {
        try {
            return chatAsync(userId, message, context, projectData).get();
        } catch (Exception e) {
            log.error("Chat request failed: {}", e.getMessage(), e);
            return chatFallback(userId, message, context, e);
        }
    }

    /**
     * Circuit Breaker Fallback 메서드
     */
    private ChatResponse chatFallback(String userId, String message, List<ChatMessage> context, Throwable throwable) {
        log.warn("Circuit breaker fallback triggered [userId: {}]: {}", userId, throwable != null ? throwable.getMessage() : "Unknown error");
        
        try {
            return callMock(userId, message, context);
        } catch (Exception mockError) {
            log.error("Mock AI service call failed: {}", mockError.getMessage(), mockError);
            // Mock 서비스도 실패한 경우 기본 응답 반환
            return createDefaultResponse(message);
        }
    }
    
    /**
     * 기본 응답 생성 (모든 서비스 실패 시)
     */
    private ChatResponse createDefaultResponse(String message) {
        // 간단한 키워드 기반 응답
        String lowerMessage = message.toLowerCase();
        String reply;
        
        if (lowerMessage.contains("안녕") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            reply = "안녕하세요! PMS AI 어시스턴트입니다. 현재 서비스 연결에 문제가 있어 기본 응답을 드리고 있습니다. 잠시 후 다시 시도해주세요.";
        } else if (lowerMessage.contains("프로젝트") || lowerMessage.contains("project")) {
            reply = "프로젝트 관련 질문이시군요. 현재 AI 서비스 연결에 문제가 있어 정확한 답변을 드리기 어렵습니다. 잠시 후 다시 시도해주세요.";
        } else if (lowerMessage.contains("태스크") || lowerMessage.contains("task")) {
            reply = "태스크 관련 질문이시군요. 현재 AI 서비스 연결에 문제가 있어 정확한 답변을 드리기 어렵습니다. 잠시 후 다시 시도해주세요.";
        } else {
            reply = "죄송합니다. 현재 AI 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요.";
        }
        
        return ChatResponse.builder()
                .reply(reply)
                .confidence(0.0)
                .suggestions(List.of(
                    "프로젝트 진행률 확인",
                    "할당된 태스크 조회",
                    "이번 스프린트 목표 확인"
                ))
                .build();
    }

    // 오버로드된 메서드 - 프로젝트 데이터 없이 호출 시 사용
    // 현재는 직접 호출되지 않지만 향후 사용 가능성을 위해 유지

    /**
     * Flask LLM 서비스 호출 (프로젝트 데이터 포함)
     */
    private ChatResponse callFlaskLLM(String message, List<ChatMessage> context, String projectData) {
        // Flask LLM이 기대하는 형식으로 컨텍스트 변환
        List<Map<String, String>> contextList = new ArrayList<>();
        for (ChatMessage msg : context) {
            contextList.add(Map.of(
                    "role", msg.getRole().name().toLowerCase(),
                    "content", msg.getContent()
            ));
        }

        // RAG 검색 수행 (관련 문서 찾기)
        List<String> retrievedDocs = ragSearchService.searchRelevantDocuments(message, 3);
        log.info("Retrieved {} documents from RAG for message: {}", retrievedDocs.size(), message);

        Map<String, Object> request = new HashMap<>();
        request.put("message", message);
        request.put("context", contextList);
        request.put("retrieved_docs", retrievedDocs);  // RAG 검색 결과 추가
        
        // 프로젝트 데이터가 있으면 추가
        if (projectData != null && !projectData.isBlank()) {
            // 프로젝트 데이터를 retrieved_docs에 추가하여 LLM 컨텍스트에 포함
            List<String> allDocs = new ArrayList<>(retrievedDocs);
            allDocs.add(0, "=== 프로젝트 데이터 ===\n" + projectData);
            request.put("retrieved_docs", allDocs);
            log.info("Added project data to LLM context");
        }

        String correlationId = getOrCreateCorrelationId();
        
        Map<String, Object> response = aiServiceWebClient.post()
                .uri("/api/chat")
                .header("X-Correlation-ID", correlationId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null) {
            throw new IllegalStateException("Flask LLM service returned null response");
        }

        String reply = extractFlaskReply(response);
        Double confidence = extractConfidence(response);
        List<String> suggestions = extractSuggestions(response);

        return ChatResponse.builder()
                .reply(reply)
                .confidence(confidence)
                .suggestions(suggestions)
                .build();
    }

    private String extractFlaskReply(Map<String, Object> response) {
        Object replyObj = response.get("reply");
        if (replyObj instanceof String && !((String) replyObj).isBlank()) {
            return (String) replyObj;
        }
        throw new IllegalStateException("Flask LLM response missing reply field");
    }

    private Double extractConfidence(Map<String, Object> response) {
        Object confidenceObj = response.get("confidence");
        if (confidenceObj instanceof Number) {
            return ((Number) confidenceObj).doubleValue();
        }
        return 0.85; // 기본값
    }

    @SuppressWarnings("unchecked")
    private List<String> extractSuggestions(Map<String, Object> response) {
        Object suggestionsObj = response.get("suggestions");
        if (suggestionsObj instanceof List) {
            return (List<String>) suggestionsObj;
        }
        return List.of();
    }

    private ChatResponse callMock(String userId, String message, List<ChatMessage> context) {
        log.info("Calling mock AI service [userId: {}, message: {}]", userId, message);
        
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("message", message);
        request.put("context", context.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole().name().toLowerCase(),
                        "content", msg.getContent()
                ))
                .toList());

        WebClient webClient = webClientBuilder
                .baseUrl(aiMockUrl)
                .build();

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5)) // Mock 서비스는 빠르게 응답해야 함
                    .block();

            if (response == null) {
                log.warn("Mock AI service returned null response");
                throw new IllegalStateException("Mock AI service returned null response");
            }

            String reply = (String) response.get("reply");
            if (reply == null || reply.isBlank()) {
                log.warn("Mock AI service returned empty reply");
                throw new IllegalStateException("Mock AI service returned empty reply");
            }

            log.info("Mock AI service response received successfully");
            return ChatResponse.builder()
                    .reply(reply)
                    .confidence((Double) response.getOrDefault("confidence", 0.9))
                    .suggestions((List<String>) response.getOrDefault("suggestions", List.of()))
                    .build();
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            log.error("Mock AI service connection error: {}", e.getMessage());
            throw new IllegalStateException("Mock AI service connection error: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            // WebClient timeout은 RuntimeException으로 래핑됨
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.error("Mock AI service timeout: {}", e.getMessage());
                throw new IllegalStateException("Mock AI service timeout", e);
            }
            log.error("Mock AI service error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Mock AI service error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Correlation ID 가져오기 또는 생성
     */
    private String getOrCreateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }
}
