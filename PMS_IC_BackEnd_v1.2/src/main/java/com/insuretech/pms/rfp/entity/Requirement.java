package com.insuretech.pms.rfp.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
}
