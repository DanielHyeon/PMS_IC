package com.insuretech.pms.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * RAG 검색 서비스
 * LLM 서비스의 벡터 DB에서 관련 문서 검색
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGSearchService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String llmServiceUrl;

    /**
     * 질문에 대한 관련 문서 검색
     */
    public List<String> searchRelevantDocuments(String query, int topK) {
        try {
            String url = llmServiceUrl + "/api/documents/search";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("top_k", topK);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

                List<String> documents = new ArrayList<>();
                for (Map<String, Object> result : results) {
                    String content = (String) result.get("content");
                    if (content != null && !content.trim().isEmpty()) {
                        documents.add(content);
                    }
                }

                log.info("RAG search found {} documents for query: {}", documents.size(), query);
                return documents;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to search documents from RAG: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 질문에 대한 관련 문서 검색 (기본 3개)
     */
    public List<String> searchRelevantDocuments(String query) {
        return searchRelevantDocuments(query, 3);
    }
}
