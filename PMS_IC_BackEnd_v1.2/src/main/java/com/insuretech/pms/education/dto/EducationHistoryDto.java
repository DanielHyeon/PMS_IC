package com.insuretech.pms.education.dto;

import com.insuretech.pms.education.entity.EducationHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationHistoryDto {
    private String id;
    private String sessionId;
    private String educationTitle;
    private String participantId;
    private String participantName;
    private String participantDepartment;
    private String completionStatus;
    private LocalDateTime registeredAt;
    private LocalDateTime completedAt;
    private Integer score;
    private String feedback;
    private Boolean certificateIssued;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EducationHistoryDto from(EducationHistory history) {
        return EducationHistoryDto.builder()
                .id(history.getId())
                .sessionId(history.getSession() != null ? history.getSession().getId() : null)
                .educationTitle(history.getSession() != null && history.getSession().getEducation() != null
                        ? history.getSession().getEducation().getTitle() : null)
                .participantId(history.getParticipantId())
                .participantName(history.getParticipantName())
                .participantDepartment(history.getParticipantDepartment())
                .completionStatus(history.getCompletionStatus() != null ? history.getCompletionStatus().name() : null)
                .registeredAt(history.getRegisteredAt())
                .completedAt(history.getCompletedAt())
                .score(history.getScore())
                .feedback(history.getFeedback())
                .certificateIssued(history.getCertificateIssued())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .build();
    }
}
