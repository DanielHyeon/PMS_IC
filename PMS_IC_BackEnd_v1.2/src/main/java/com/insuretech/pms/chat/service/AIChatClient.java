package com.insuretech.pms.chat.service;

import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public ChatResponse chat(String userId, String message, List<ChatMessage> context) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("message", message);
            request.put("context", context.stream()
                    .map(msg -> Map.of(
                            "role", msg.getRole().name().toLowerCase(),
                            "content", msg.getContent()
                    ))
                    .collect(Collectors.toList()));

            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return ChatResponse.builder()
                    .reply((String) response.get("reply"))
                    .confidence((Double) response.getOrDefault("confidence", 0.9))
                    .suggestions((List<String>) response.getOrDefault("suggestions", List.of()))
                    .build();

        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage());
            // Fallback response
            return ChatResponse.builder()
                    .reply("죄송합니다. 현재 AI 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요.")
                    .confidence(0.0)
                    .build();
        }
    }
}