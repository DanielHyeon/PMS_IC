package com.insuretech.pms.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportDto {

    private String id;
    private String projectId;
    private String sprintId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String generatedBy;
    private LocalDate generatedAt;

    // Metrics
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer inProgressTasks;
    private Integer todoTasks;
    private Integer blockedTasks;
    private Double completionRate;
    private Double velocity;

    // Progress
    private Integer storyPointsCompleted;
    private Integer storyPointsInProgress;
    private Integer storyPointsPlanned;

    // WIP Analysis
    private Integer averageWipCount;
    private Integer peakWipCount;
    private Double flowEfficiency;
    private List<String> bottlenecks;

    // Trend Analysis
    private Double velocityTrend;
    private Double completionTrend;
    private List<String> recommendations;

    // Summary
    private String summary;
    private String generatedContent;

    // LLM Integration
    private String llmModel;
    private Double llmConfidenceScore;
}
