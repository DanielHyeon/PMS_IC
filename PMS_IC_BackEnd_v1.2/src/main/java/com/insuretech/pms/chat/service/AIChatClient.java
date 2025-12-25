package com.insuretech.pms.chat.service;

import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.mock-url}")
    private String aiMockUrl;

    @Value("${ai.service.model:llama3}")
    private String aiModel;

    public ChatResponse chat(String userId, String message, List<ChatMessage> context) {
        try {
            return callOllama(message, context);
        } catch (Exception e) {
            log.warn("Primary AI service call failed, falling back to mock: {}", e.getMessage());
            try {
                return callMock(userId, message, context);
            } catch (Exception mockError) {
                log.error("Mock AI service call failed: {}", mockError.getMessage());
                return ChatResponse.builder()
                        .reply("죄송합니다. 현재 AI 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요.")
                        .confidence(0.0)
                        .build();
            }
        }
    }

    private ChatResponse callOllama(String message, List<ChatMessage> context) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage msg : context) {
            messages.add(Map.of(
                    "role", msg.getRole().name().toLowerCase(),
                    "content", msg.getContent()
            ));
        }
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> request = new HashMap<>();
        request.put("model", aiModel);
        request.put("messages", messages);
        request.put("stream", false);

        WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

        Map<String, Object> response = webClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("AI service returned null response");
        }

        String reply = extractOllamaReply(response);
        return ChatResponse.builder()
                .reply(reply)
                .confidence(0.9)
                .suggestions(List.of())
                .build();
    }

    private String extractOllamaReply(Map<String, Object> response) {
        Object messageObj = response.get("message");
        if (messageObj instanceof Map) {
            Object contentObj = ((Map<?, ?>) messageObj).get("content");
            if (contentObj instanceof String && !((String) contentObj).isBlank()) {
                return (String) contentObj;
            }
        }

        Object responseText = response.get("response");
        if (responseText instanceof String && !((String) responseText).isBlank()) {
            return (String) responseText;
        }

        throw new IllegalStateException("AI response missing message content");
    }

    private ChatResponse callMock(String userId, String message, List<ChatMessage> context) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("message", message);
        request.put("context", context.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole().name().toLowerCase(),
                        "content", msg.getContent()
                ))
                .toList());

        WebClient webClient = webClientBuilder.baseUrl(aiMockUrl).build();

        Map<String, Object> response = webClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Mock AI service returned null response");
        }

        return ChatResponse.builder()
                .reply((String) response.get("reply"))
                .confidence((Double) response.getOrDefault("confidence", 0.9))
                .suggestions((List<String>) response.getOrDefault("suggestions", List.of()))
                .build();
    }
}
