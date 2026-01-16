package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "phases", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phase extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "order_num", nullable = false)
    private Integer orderNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PhaseStatus status = PhaseStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_status", length = 50)
    private GateStatus gateStatus;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "progress", nullable = false)
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", length = 20)
    @Builder.Default
    private TrackType trackType = TrackType.COMMON;

    public enum PhaseStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        ON_HOLD
    }

    public enum GateStatus {
        PENDING,
        SUBMITTED,
        APPROVED,
        REJECTED
    }

    public enum TrackType {
        AI, SI, COMMON
    }
}
