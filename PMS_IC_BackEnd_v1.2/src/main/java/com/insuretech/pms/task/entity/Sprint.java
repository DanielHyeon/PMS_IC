package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sprints", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "project_id", length = 50, nullable = false)
    private String projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNED;

    @Column(name = "conwip_limit")
    private Integer conwipLimit;

    @Column(name = "enable_wip_validation", nullable = false)
    @Builder.Default
    private Boolean enableWipValidation = true;

    @Column(name = "neo4j_node_id")
    private String neo4jNodeId;

    public enum SprintStatus {
        PLANNED, ACTIVE, COMPLETED, CANCELLED
    }
}