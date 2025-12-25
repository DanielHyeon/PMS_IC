package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kpis", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kpi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "target", nullable = false, length = 100)
    private String target;

    @Column(name = "current", length = 100)
    private String current;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private KpiStatus status = KpiStatus.ON_TRACK;

    public enum KpiStatus {
        ACHIEVED,
        ON_TRACK,
        AT_RISK
    }
}
