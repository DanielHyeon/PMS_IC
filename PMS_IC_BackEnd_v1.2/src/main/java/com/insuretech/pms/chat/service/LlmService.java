package com.insuretech.pms.chat.service;

import com.insuretech.pms.chat.dto.ModelInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.service.url}")
    private String aiServiceUrl;
    private static final String DEFAULT_MODEL_DIR = "/app/models";

    /**
     * 현재 사용 중인 모델 정보 조회
     */
    public ModelInfoResponse getCurrentModel() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> response = webClient.get()
                    .uri("/api/model/current")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return ModelInfoResponse.builder()
                        .currentModel(normalizeModelName((String) response.get("currentModel")))
                        .status((String) response.getOrDefault("status", "active"))
                        .timestamp(System.currentTimeMillis())
                        .build();
            }

            throw new IllegalStateException("Failed to get current model info");
        } catch (Exception e) {
            log.error("Error getting current model: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get current model information", e);
        }
    }

    /**
     * LLM 모델 변경
     */
    public ModelInfoResponse changeModel(String modelPath) {
        try {
            String resolvedModelPath = resolveModelPath(modelPath);
            log.info("Changing LLM model to: {} (resolved: {})", modelPath, resolvedModelPath);

            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> request = Map.of("modelPath", resolvedModelPath);

            try {
                Map<String, Object> response = webClient.put()
                        .uri("/api/model/change")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (response != null && "success".equals(response.get("status"))) {
                    log.info("Successfully changed model to: {}", resolvedModelPath);
                    return ModelInfoResponse.builder()
                            .currentModel(normalizeModelName((String) response.get("currentModel")))
                            .status((String) response.get("status"))
                            .timestamp(System.currentTimeMillis())
                            .build();
                }

                String errorMessage = response != null ? 
                        (String) response.getOrDefault("message", response.getOrDefault("error", "Unknown error")) :
                        "No response from LLM service";
                log.error("Model change failed. Response: {}", response);
                throw new IllegalStateException("Model change failed: " + errorMessage);
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                log.error("WebClient error changing model to {}: Status={}, Body={}", 
                        modelPath, e.getStatusCode(), e.getResponseBodyAsString(), e);
                String errorBody = e.getResponseBodyAsString();
                String errorMessage = extractErrorMessage(errorBody);
                throw new RuntimeException("모델 변경 실패: " + errorMessage, e);
            } catch (org.springframework.web.reactive.function.client.WebClientException e) {
                // 연결 실패 등 네트워크 에러 처리
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Connection refused")) {
                    errorMsg = String.format("LLM 서비스에 연결할 수 없습니다 (%s). 서비스가 실행 중인지 확인하세요. " +
                            "Docker를 사용하는 경우 'docker-compose up llm-service' 명령으로 서비스를 시작하세요.", aiServiceUrl);
                } else if (errorMsg != null && errorMsg.contains("timeout")) {
                    errorMsg = String.format("LLM 서비스 응답 시간 초과 (%s). 서비스가 정상적으로 동작하는지 확인하세요.", aiServiceUrl);
                } else if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = String.format("LLM 서비스 연결 실패 (%s)", aiServiceUrl);
                }
                log.error("WebClient connection error: {}", errorMsg, e);
                throw new RuntimeException("모델 변경 실패: " + errorMsg, e);
            }
        } catch (RuntimeException e) {
            // 이미 처리된 예외는 그대로 전달
            throw e;
        } catch (Exception e) {
            log.error("Error changing model to {}: {}", modelPath, e.getMessage(), e);
            throw new RuntimeException("모델 변경 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 에러 응답 본문에서 메시지 추출
     */
    private String extractErrorMessage(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) {
            return "알 수 없는 오류";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> errorMap = mapper.readValue(errorBody, java.util.Map.class);
            String message = (String) errorMap.getOrDefault("message", errorMap.getOrDefault("error", errorBody));
            return message != null && !message.isEmpty() ? message : "알 수 없는 오류";
        } catch (Exception e) {
            log.warn("Failed to parse error response: {}", errorBody);
            return errorBody.length() > 200 ? errorBody.substring(0, 200) + "..." : errorBody;
        }
    }

    /**
     * LLM 서비스 헬스 체크
     */
    public Object checkHealth() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            return webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception e) {
            log.error("LLM service health check failed: {}", e.getMessage());
            return Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage()
            );
        }
    }

    private String normalizeModelName(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            return modelPath;
        }
        return Path.of(modelPath).getFileName().toString();
    }

    private String resolveModelPath(String modelPath) {
        if (modelPath == null) {
            return null;
        }
        String trimmed = modelPath.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        return DEFAULT_MODEL_DIR + "/" + trimmed;
    }
}
