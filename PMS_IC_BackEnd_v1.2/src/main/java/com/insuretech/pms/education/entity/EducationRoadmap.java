package com.insuretech.pms.education.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "education_roadmaps", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationRoadmap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_id", nullable = false)
    private Education education;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 50)
    private Education.TargetRole targetRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 50)
    @Builder.Default
    private EducationLevel level = EducationLevel.BASIC;

    @Column(name = "order_num", nullable = false)
    @Builder.Default
    private Integer orderNum = 1;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public enum EducationLevel {
        BASIC,
        INTERMEDIATE,
        ADVANCED
    }
}
