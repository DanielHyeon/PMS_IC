package com.insuretech.pms.education.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "education_histories", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private EducationSession session;

    @Column(name = "participant_id", nullable = false, length = 50)
    private String participantId;

    @Column(name = "participant_name", nullable = false, length = 100)
    private String participantName;

    @Column(name = "participant_department", length = 100)
    private String participantDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false, length = 50)
    @Builder.Default
    private CompletionStatus completionStatus = CompletionStatus.REGISTERED;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "score")
    private Integer score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "certificate_issued")
    @Builder.Default
    private Boolean certificateIssued = false;

    public enum CompletionStatus {
        REGISTERED,
        IN_PROGRESS,
        COMPLETED,
        DROPPED,
        NO_SHOW
    }
}
