package com.insuretech.pms.rfp.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "requirements", schema = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Requirement extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfp_id")
    private Rfp rfp;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "requirement_code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private RequirementCategory category = RequirementCategory.FUNCTIONAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private RequirementStatus status = RequirementStatus.IDENTIFIED;

    @Column
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "assignee_id", length = 36)
    private String assigneeId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Column(name = "estimated_effort")
    private Integer estimatedEffort;

    @Column(name = "actual_effort")
    private Integer actualEffort;

    /**
     * Story Points estimation (Fibonacci sequence: 1, 2, 3, 5, 8, 13, 21)
     * Optional at creation, becomes mandatory when moving to Ready/Committed state
     * Can be auto-suggested by LLM (requires approval)
     * When linked to BacklogItem, value is copied (not forced synchronization)
     */
    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "estimated_effort_hours")
    private Integer estimatedEffortHours;

    @Column(name = "actual_effort_hours")
    private Integer actualEffortHours;

    @Column(name = "remaining_effort_hours")
    private Integer remainingEffortHours;

    /**
     * Progress percentage (0-100)
     * Primary calculation: Story Point based (when storyPoints is set)
     * Fallback calculation: Task count based (when storyPoints is null)
     * Time-based calculation: Optional when time tracking is enabled
     * Updated on: status changes, storyPoints changes, linked task creation/deletion
     */
    @Column(name = "progress_percentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "last_progress_update")
    private LocalDateTime lastProgressUpdate;

    @Transient
    private ProgressStage progressStage;

    /**
     * Progress calculation method used for this requirement
     * STORY_POINT: SP-based (default when SP available)
     * TASK_COUNT: Count-based (fallback when SP unavailable)
     * TIME_BASED: Time-based (optional, when time tracking enabled)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_calc_method", length = 50)
    @Builder.Default
    private ProgressCalculationMethod progressCalcMethod = ProgressCalculationMethod.STORY_POINT;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "neo4j_node_id", length = 100)
    private String neo4jNodeId;

    @ElementCollection
    @CollectionTable(name = "requirement_task_links", schema = "project",
            joinColumns = @JoinColumn(name = "requirement_id"))
    @Column(name = "task_id")
    @Builder.Default
    private Set<String> linkedTaskIds = new HashSet<>();

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.tenantId == null) {
            this.tenantId = this.projectId;
        }
    }

    public void linkTask(String taskId) {
        linkedTaskIds.add(taskId);
    }

    public void unlinkTask(String taskId) {
        linkedTaskIds.remove(taskId);
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void calculateProgressStage() {
        if (dueDate != null && LocalDate.now().isAfter(dueDate)
            && progressPercentage < 100) {
            this.progressStage = ProgressStage.DELAYED;
        } else if (progressPercentage == 0) {
            this.progressStage = ProgressStage.NOT_STARTED;
        } else if (progressPercentage == 100) {
            this.progressStage = ProgressStage.COMPLETED;
        } else {
            this.progressStage = ProgressStage.IN_PROGRESS;
        }
    }

    /**
     * Progress stage enumeration - derived from progressPercentage and dueDate
     */
    public enum ProgressStage {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        DELAYED
    }

    /**
     * Progress calculation method enumeration
     * STORY_POINT: Primary method using story points (when available)
     * TASK_COUNT: Fallback method using linked task completion count
     * TIME_BASED: Optional time-based calculation when time tracking is enabled
     */
    public enum ProgressCalculationMethod {
        STORY_POINT,
        TASK_COUNT,
        TIME_BASED
    }
}
