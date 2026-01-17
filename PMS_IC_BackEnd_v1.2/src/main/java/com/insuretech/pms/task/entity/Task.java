package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private KanbanColumn column;

    @Column(name = "phase_id", length = 50)
    private String phaseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_id", length = 50)
    private String assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "order_num")
    private Integer orderNum;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "sprint_id", length = 50)
    private String sprintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", length = 20)
    @Builder.Default
    private TrackType trackType = TrackType.COMMON;

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TaskStatus {
        TODO, IN_PROGRESS, REVIEW, DONE
    }

    public enum TrackType {
        AI, SI, COMMON
    }
}