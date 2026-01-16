package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issues", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 50)
    @Builder.Default
    private IssueType issueType = IssueType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;

    @Column(name = "assignee", length = 100)
    private String assignee;

    @Column(name = "reporter", length = 100)
    private String reporter;

    @Column(name = "reviewer", length = 100)
    private String reviewer;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    public enum IssueType {
        BUG,
        RISK,
        BLOCKER,
        CHANGE_REQUEST,
        QUESTION,
        IMPROVEMENT,
        OTHER
    }

    public enum IssuePriority {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum IssueStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        VERIFIED,
        CLOSED,
        REOPENED,
        DEFERRED
    }
}
