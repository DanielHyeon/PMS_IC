package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "weekly_reports", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "project_id", length = 50, nullable = false)
    private String projectId;

    @Column(name = "sprint_id", length = 50)
    private String sprintId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "generated_by", length = 50)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDate generatedAt;

    // Metrics
    @Column(name = "total_tasks")
    private Integer totalTasks;

    @Column(name = "completed_tasks")
    private Integer completedTasks;

    @Column(name = "in_progress_tasks")
    private Integer inProgressTasks;

    @Column(name = "todo_tasks")
    private Integer todoTasks;

    @Column(name = "blocked_tasks")
    private Integer blockedTasks;

    @Column(name = "completion_rate")
    private Double completionRate;

    @Column(name = "velocity")
    private Double velocity;

    // Progress
    @Column(name = "story_points_completed")
    private Integer storyPointsCompleted;

    @Column(name = "story_points_in_progress")
    private Integer storyPointsInProgress;

    @Column(name = "story_points_planned")
    private Integer storyPointsPlanned;

    // WIP Analysis
    @Column(name = "average_wip_count")
    private Integer averageWipCount;

    @Column(name = "peak_wip_count")
    private Integer peakWipCount;

    @Column(name = "flow_efficiency")
    private Double flowEfficiency;

    @Column(name = "bottlenecks", columnDefinition = "TEXT")
    private String bottlenecks;

    // Trend Analysis
    @Column(name = "velocity_trend")
    private Double velocityTrend;

    @Column(name = "completion_trend")
    private Double completionTrend;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    // Summary
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "generated_content", columnDefinition = "TEXT")
    private String generatedContent;

    // LLM Integration
    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @Column(name = "llm_confidence_score")
    private Double llmConfidenceScore;
}
