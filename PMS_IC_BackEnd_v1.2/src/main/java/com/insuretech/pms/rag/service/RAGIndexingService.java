package com.insuretech.pms.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * RAG 문서 인덱싱 서비스
 * 업로드된 파일을 LLM 서비스의 벡터 DB에 인덱싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGIndexingService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String llmServiceUrl;

    /**
     * 파일을 RAG 시스템에 인덱싱
     */
    public boolean indexFile(String documentId, Path filePath, Map<String, String> metadata) {
        try {
            // 파일에서 텍스트 추출
            String content = extractTextFromFile(filePath);

            if (content == null || content.trim().isEmpty()) {
                log.warn("No text extracted from file: {}", filePath);
                return false;
            }

            // LLM 서비스에 문서 전송
            return indexDocument(documentId, content, metadata);

        } catch (Exception e) {
            log.error("Failed to index file: {}", filePath, e);
            return false;
        }
    }

    /**
     * 파일에서 텍스트 추출
     */
    private String extractTextFromFile(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            return extractFromPdf(filePath);
        } else if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
            return extractFromDocx(filePath);
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(filePath);
        } else {
            log.warn("Unsupported file type: {}", fileName);
            return null;
        }
    }

    /**
     * PDF에서 텍스트 추출
     */
    private String extractFromPdf(Path filePath) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Word 문서에서 텍스트 추출
     */
    private String extractFromDocx(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n\n");
            }
            return text.toString();
        }
    }

    /**
     * LLM 서비스에 문서 인덱싱 요청
     */
    private boolean indexDocument(String documentId, String content, Map<String, String> metadata) {
        try {
            String url = llmServiceUrl + "/api/documents";

            // 요청 바디 구성
            Map<String, Object> document = new HashMap<>();
            document.put("id", documentId);
            document.put("content", content);
            document.put("metadata", metadata != null ? metadata : new HashMap<>());

            List<Map<String, Object>> documents = List.of(document);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("documents", documents);

            // HTTP 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("success_count")) {
                Integer successCount = (Integer) response.get("success_count");
                log.info("Document indexed successfully: {} (success_count={})", documentId, successCount);
                return successCount != null && successCount > 0;
            }

            return false;

        } catch (Exception e) {
            log.error("Failed to index document to LLM service: {}", documentId, e);
            return false;
        }
    }

    /**
     * 문서 삭제
     */
    public boolean deleteDocument(String documentId) {
        try {
            String url = llmServiceUrl + "/api/documents/" + documentId;
            restTemplate.delete(url);
            log.info("Document deleted from RAG: {}", documentId);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete document from RAG: {}", documentId, e);
            return false;
        }
    }

    /**
     * RAG 통계 조회
     */
    public Map<String, Object> getStats() {
        try {
            String url = llmServiceUrl + "/api/documents/stats";

            @SuppressWarnings("unchecked")
            Map<String, Object> stats = restTemplate.getForObject(url, Map.class);

            return stats != null ? stats : Collections.emptyMap();

        } catch (Exception e) {
            log.error("Failed to get RAG stats", e);
            return Collections.emptyMap();
        }
    }
}
