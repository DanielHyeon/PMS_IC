package com.insuretech.pms.task.service;

import com.insuretech.pms.task.dto.WeeklyReportDto;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.entity.WeeklyReport;
import com.insuretech.pms.task.repository.SprintRepository;
import com.insuretech.pms.task.repository.TaskRepository;
import com.insuretech.pms.task.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final RestTemplate restTemplate;

    @Value("${ai-service.url:http://llm-service:8000}")
    private String aiServiceUrl;

    @Value("${ai-service.enabled:true}")
    private Boolean aiServiceEnabled;

    /**
     * Generate weekly report for a project
     * @param projectId Project ID
     * @param weekStartDate Start date of the week
     * @param userId User ID for audit
     * @return Generated report
     */
    @Transactional
    public WeeklyReportDto generateWeeklyReport(String projectId, LocalDate weekStartDate, String userId) {
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        WeeklyReport report = WeeklyReport.builder()
                .projectId(projectId)
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .generatedBy(userId)
                .generatedAt(LocalDate.now())
                .build();

        // Calculate metrics
        calculateMetrics(report);

        // Generate LLM insights if enabled
        if (aiServiceEnabled) {
            generateLlmInsights(report);
        }

        // Save and return
        WeeklyReport savedReport = weeklyReportRepository.save(report);
        return toDto(savedReport);
    }

    /**
     * Generate weekly report for a sprint
     * @param sprintId Sprint ID
     * @param userId User ID for audit
     * @return Generated report
     */
    @Transactional
    public WeeklyReportDto generateSprintWeeklyReport(String sprintId, String userId) {
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        if (sprintOpt.isEmpty()) {
            throw new IllegalArgumentException("Sprint not found: " + sprintId);
        }

        Sprint sprint = sprintOpt.get();
        LocalDate weekStartDate = LocalDate.now().with(java.time.temporal.ChronoField.DAY_OF_WEEK, 1);

        WeeklyReport report = WeeklyReport.builder()
                .projectId(sprint.getProjectId())
                .sprintId(sprintId)
                .weekStartDate(weekStartDate)
                .weekEndDate(weekStartDate.plusDays(6))
                .generatedBy(userId)
                .generatedAt(LocalDate.now())
                .build();

        // Calculate sprint-specific metrics
        calculateSprintMetrics(report);

        // Generate LLM insights if enabled
        if (aiServiceEnabled) {
            generateLlmInsights(report);
        }

        // Save and return
        WeeklyReport savedReport = weeklyReportRepository.save(report);
        return toDto(savedReport);
    }

    /**
     * Calculate metrics for the report
     */
    private void calculateMetrics(WeeklyReport report) {
        // Get all tasks for the project (simplified - in real scenario would filter by week)
        List<Task> allTasks = taskRepository.findAll();
        List<Task> projectTasks = allTasks.stream()
                .filter(t -> true) // Filter by project in real implementation
                .collect(Collectors.toList());

        int total = projectTasks.size();
        int completed = (int) projectTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.DONE)
                .count();
        int inProgress = (int) projectTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                .count();
        int todo = (int) projectTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.TODO)
                .count();
        int blocked = (int) projectTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.REVIEW)
                .count();

        report.setTotalTasks(total);
        report.setCompletedTasks(completed);
        report.setInProgressTasks(inProgress);
        report.setTodoTasks(todo);
        report.setBlockedTasks(blocked);
        report.setCompletionRate(total > 0 ? (completed * 100.0) / total : 0.0);

        // Calculate velocity (simplified)
        report.setVelocity((double) completed);

        // Set default values for other metrics
        report.setAverageWipCount(inProgress);
        report.setPeakWipCount(inProgress);
        report.setFlowEfficiency(total > 0 ? (completed * 100.0) / total : 0.0);
    }

    /**
     * Calculate sprint-specific metrics
     */
    private void calculateSprintMetrics(WeeklyReport report) {
        if (report.getSprintId() == null) {
            return;
        }

        List<Task> sprintTasks = taskRepository.findBySprintId(report.getSprintId());

        int total = sprintTasks.size();
        int completed = (int) sprintTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.DONE)
                .count();
        int inProgress = (int) sprintTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                .count();
        int todo = (int) sprintTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.TODO)
                .count();

        report.setTotalTasks(total);
        report.setCompletedTasks(completed);
        report.setInProgressTasks(inProgress);
        report.setTodoTasks(todo);
        report.setCompletionRate(total > 0 ? (completed * 100.0) / total : 0.0);
        report.setVelocity((double) completed);
        report.setAverageWipCount(inProgress);
        report.setFlowEfficiency(total > 0 ? (completed * 100.0) / total : 0.0);
    }

    /**
     * Generate LLM insights for the report
     */
    private void generateLlmInsights(WeeklyReport report) {
        try {
            // Build context for LLM
            Map<String, Object> context = buildLlmContext(report);

            // Call LLM service
            Map<String, Object> llmResponse = callLlmService(context);

            if (llmResponse != null) {
                report.setSummary((String) llmResponse.getOrDefault("summary", ""));
                report.setRecommendations((String) llmResponse.getOrDefault("recommendations", ""));
                report.setGeneratedContent((String) llmResponse.getOrDefault("content", ""));
                report.setLlmModel((String) llmResponse.getOrDefault("model", "unknown"));
                Object confidence = llmResponse.get("confidence");
                if (confidence instanceof Number) {
                    report.setLlmConfidenceScore(((Number) confidence).doubleValue());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail report generation
            report.setSummary("Report generated without LLM insights due to service unavailability");
        }
    }

    /**
     * Build context data for LLM
     */
    private Map<String, Object> buildLlmContext(WeeklyReport report) {
        Map<String, Object> context = new HashMap<>();
        context.put("weekStartDate", report.getWeekStartDate().toString());
        context.put("weekEndDate", report.getWeekEndDate().toString());
        context.put("totalTasks", report.getTotalTasks());
        context.put("completedTasks", report.getCompletedTasks());
        context.put("inProgressTasks", report.getInProgressTasks());
        context.put("todoTasks", report.getTodoTasks());
        context.put("completionRate", report.getCompletionRate());
        context.put("velocity", report.getVelocity());
        context.put("averageWip", report.getAverageWipCount());
        context.put("flowEfficiency", report.getFlowEfficiency());
        return context;
    }

    /**
     * Call LLM service for report generation
     */
    private Map<String, Object> callLlmService(Map<String, Object> context) {
        try {
            // Implementation would call actual LLM service
            // This is a placeholder for the integration
            Map<String, Object> request = new HashMap<>();
            request.put("prompt", generatePrompt(context));
            request.put("context", context);

            // In real implementation: return restTemplate.postForObject(...);
            return generateMockLlmResponse(context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate prompt for LLM
     */
    private String generatePrompt(Map<String, Object> context) {
        return String.format(
                "Generate a weekly report summary for the following metrics:\n" +
                        "Total Tasks: %d\n" +
                        "Completed: %d\n" +
                        "In Progress: %d\n" +
                        "To Do: %d\n" +
                        "Completion Rate: %.1f%%\n" +
                        "Provide key insights and recommendations.",
                context.get("totalTasks"),
                context.get("completedTasks"),
                context.get("inProgressTasks"),
                context.get("todoTasks"),
                context.get("completionRate")
        );
    }

    /**
     * Mock LLM response for demonstration
     */
    private Map<String, Object> generateMockLlmResponse(Map<String, Object> context) {
        Map<String, Object> response = new HashMap<>();
        response.put("summary", generateSummary(context));
        response.put("recommendations", generateRecommendations(context));
        response.put("content", generateFullContent(context));
        response.put("model", "mock-model");
        response.put("confidence", 0.85);
        return response;
    }

    /**
     * Generate summary from context
     */
    private String generateSummary(Map<String, Object> context) {
        Integer completed = (Integer) context.get("completedTasks");
        Integer total = (Integer) context.get("totalTasks");
        Double rate = (Double) context.get("completionRate");

        return String.format(
                "This week's report shows a completion rate of %.1f%% with %d out of %d tasks completed.",
                rate, completed, total
        );
    }

    /**
     * Generate recommendations from context
     */
    private String generateRecommendations(Map<String, Object> context) {
        List<String> recommendations = new ArrayList<>();

        Double rate = (Double) context.get("completionRate");
        if (rate < 50) {
            recommendations.add("- Consider reassigning tasks to improve throughput");
        }

        Integer inProgress = (Integer) context.get("inProgressTasks");
        if (inProgress != null && inProgress > 5) {
            recommendations.add("- High number of in-progress tasks. Consider completing before starting new ones");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("- Maintain current pace to meet sprint goals");
        }

        return String.join("\n", recommendations);
    }

    /**
     * Generate full report content
     */
    private String generateFullContent(Map<String, Object> context) {
        return String.format(
                "Weekly Report\n" +
                        "=============\n" +
                        "Completion Rate: %.1f%%\n" +
                        "Tasks Completed: %d\n" +
                        "Tasks In Progress: %d\n" +
                        "Average WIP: %d\n" +
                        "Flow Efficiency: %.1f%%",
                context.get("completionRate"),
                context.get("completedTasks"),
                context.get("inProgressTasks"),
                context.get("averageWip"),
                context.get("flowEfficiency")
        );
    }

    /**
     * Get recent reports for a project
     */
    @Transactional(readOnly = true)
    public List<WeeklyReportDto> getProjectReports(String projectId, int limit) {
        List<WeeklyReport> reports = weeklyReportRepository.findByProjectIdOrderByWeekStartDateDesc(projectId);
        return reports.stream()
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get recent reports for a sprint
     */
    @Transactional(readOnly = true)
    public List<WeeklyReportDto> getSprintReports(String sprintId, int limit) {
        List<WeeklyReport> reports = weeklyReportRepository.findBySprintIdOrderByWeekStartDateDesc(sprintId);
        return reports.stream()
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get report by ID
     */
    @Transactional(readOnly = true)
    public WeeklyReportDto getReportById(String reportId) {
        return weeklyReportRepository.findById(reportId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    }

    /**
     * Delete report by ID
     */
    @Transactional
    public void deleteReport(String reportId) {
        if (!weeklyReportRepository.existsById(reportId)) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        weeklyReportRepository.deleteById(reportId);
    }

    /**
     * Convert entity to DTO
     */
    private WeeklyReportDto toDto(WeeklyReport report) {
        return WeeklyReportDto.builder()
                .id(report.getId())
                .projectId(report.getProjectId())
                .sprintId(report.getSprintId())
                .weekStartDate(report.getWeekStartDate())
                .weekEndDate(report.getWeekEndDate())
                .generatedBy(report.getGeneratedBy())
                .generatedAt(report.getGeneratedAt())
                .totalTasks(report.getTotalTasks())
                .completedTasks(report.getCompletedTasks())
                .inProgressTasks(report.getInProgressTasks())
                .todoTasks(report.getTodoTasks())
                .blockedTasks(report.getBlockedTasks())
                .completionRate(report.getCompletionRate())
                .velocity(report.getVelocity())
                .storyPointsCompleted(report.getStoryPointsCompleted())
                .storyPointsInProgress(report.getStoryPointsInProgress())
                .storyPointsPlanned(report.getStoryPointsPlanned())
                .averageWipCount(report.getAverageWipCount())
                .peakWipCount(report.getPeakWipCount())
                .flowEfficiency(report.getFlowEfficiency())
                .velocityTrend(report.getVelocityTrend())
                .completionTrend(report.getCompletionTrend())
                .summary(report.getSummary())
                .generatedContent(report.getGeneratedContent())
                .llmModel(report.getLlmModel())
                .llmConfidenceScore(report.getLlmConfidenceScore())
                .build();
    }
}
