package com.insuretech.pms.education.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "educations", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_type", nullable = false, length = 50)
    @Builder.Default
    private EducationType educationType = EducationType.IT_BASIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private EducationCategory category = EducationCategory.AGENT_AI;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 50)
    @Builder.Default
    private TargetRole targetRole = TargetRole.ALL;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    private String learningObjectives;

    @Column(name = "instructor", length = 100)
    private String instructor;

    @Column(name = "materials", columnDefinition = "TEXT")
    private String materials;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public enum EducationType {
        IT_BASIC,
        IT_INTERMEDIATE,
        IT_ADVANCED,
        BUSINESS_AI_AWARENESS,
        BUSINESS_CASE_STUDY,
        POST_DEPLOYMENT
    }

    public enum EducationCategory {
        AGENT_AI,
        MACHINE_LEARNING,
        DEEP_LEARNING,
        PYTHON,
        BUSINESS_PLANNING,
        BUSINESS_OPERATION,
        AGENT_ROLE_EXPLANATION
    }

    public enum TargetRole {
        ALL,
        PM,
        DEVELOPER,
        QA,
        BUSINESS_ANALYST,
        DATA_SCIENTIST
    }
}
