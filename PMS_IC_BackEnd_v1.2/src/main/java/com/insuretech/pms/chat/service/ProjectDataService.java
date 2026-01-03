package com.insuretech.pms.chat.service;

import com.insuretech.pms.project.dto.ProjectDto;
import com.insuretech.pms.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 프로젝트 데이터 조회 서비스
 * LLM이 프로젝트 관련 질문에 답변할 수 있도록 프로젝트 데이터를 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDataService {

    private final ProjectService projectService;

    /**
     * 프로젝트 관련 질문인지 확인
     */
    public boolean isProjectRelatedQuery(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        
        // 프로젝트 관련 키워드
        String[] projectKeywords = {
            "프로젝트", "project", "프로젝트 현황", "프로젝트 상태", "프로젝트 진행",
            "프로젝트 목록", "프로젝트 정보", "프로젝트 일정", "프로젝트 일정",
            "진행률", "progress", "상태", "status", "예산", "budget",
            "시작일", "종료일", "start", "end", "단계", "phase"
        };

        for (String keyword : projectKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 모든 프로젝트 정보를 텍스트 형식으로 반환
     */
    public String getAllProjectsSummary() {
        try {
            List<ProjectDto> projects = projectService.getAllProjects();
            
            if (projects.isEmpty()) {
                return "현재 등록된 프로젝트가 없습니다.";
            }

            StringBuilder summary = new StringBuilder();
            summary.append("=== 프로젝트 목록 ===\n\n");
            
            for (ProjectDto project : projects) {
                summary.append(formatProjectInfo(project));
                summary.append("\n");
            }

            return summary.toString();
        } catch (Exception e) {
            log.error("Failed to retrieve projects: {}", e.getMessage(), e);
            return "프로젝트 정보를 가져오는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 특정 프로젝트 정보를 텍스트 형식으로 반환
     */
    public String getProjectSummary(String projectId) {
        try {
            ProjectDto project = projectService.getProjectById(projectId);
            return formatProjectInfo(project);
        } catch (Exception e) {
            log.error("Failed to retrieve project {}: {}", projectId, e.getMessage(), e);
            return String.format("프로젝트 '%s' 정보를 가져오는 중 오류가 발생했습니다.", projectId);
        }
    }

    /**
     * 프로젝트 정보를 포맷팅
     */
    private String formatProjectInfo(ProjectDto project) {
        StringBuilder info = new StringBuilder();
        
        info.append(String.format("프로젝트 ID: %s\n", project.getId()));
        info.append(String.format("프로젝트명: %s\n", project.getName()));
        
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            info.append(String.format("설명: %s\n", project.getDescription()));
        }
        
        info.append(String.format("상태: %s\n", project.getStatus()));
        info.append(String.format("진행률: %d%%\n", project.getProgress() != null ? project.getProgress() : 0));
        
        if (project.getStartDate() != null) {
            info.append(String.format("시작일: %s\n", project.getStartDate()));
        }
        
        if (project.getEndDate() != null) {
            info.append(String.format("종료일: %s\n", project.getEndDate()));
        }
        
        if (project.getBudget() != null) {
            info.append(String.format("예산: %s\n", project.getBudget()));
        }

        if (project.getPhases() != null && !project.getPhases().isEmpty()) {
            info.append("단계:\n");
            project.getPhases().forEach(phase -> {
                info.append(String.format("  - %s (%s)\n", 
                    phase.getName(), 
                    phase.getStatus() != null ? phase.getStatus() : "N/A"));
            });
        }

        return info.toString();
    }

    /**
     * 메시지에서 프로젝트 ID 추출 시도
     */
    public String extractProjectId(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        // 간단한 패턴 매칭 (예: "프로젝트 PROJ-001", "PROJ-001 정보" 등)
        String[] patterns = {
            "프로젝트\\s+([A-Za-z0-9-_]+)",
            "([A-Za-z0-9-_]+)\\s+프로젝트",
            "프로젝트\\s+ID[\\s:]+([A-Za-z0-9-_]+)",
            "ID[\\s:]+([A-Za-z0-9-_]+)"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(message);
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }
}

