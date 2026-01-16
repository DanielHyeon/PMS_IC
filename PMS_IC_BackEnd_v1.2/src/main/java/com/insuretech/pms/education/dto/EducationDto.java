package com.insuretech.pms.education.dto;

import com.insuretech.pms.education.entity.Education;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {
    private String id;
    private String title;
    private String description;
    private String educationType;
    private String category;
    private String targetRole;
    private Integer durationHours;
    private String prerequisites;
    private String learningObjectives;
    private String instructor;
    private String materials;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EducationDto from(Education education) {
        return EducationDto.builder()
                .id(education.getId())
                .title(education.getTitle())
                .description(education.getDescription())
                .educationType(education.getEducationType() != null ? education.getEducationType().name() : null)
                .category(education.getCategory() != null ? education.getCategory().name() : null)
                .targetRole(education.getTargetRole() != null ? education.getTargetRole().name() : null)
                .durationHours(education.getDurationHours())
                .prerequisites(education.getPrerequisites())
                .learningObjectives(education.getLearningObjectives())
                .instructor(education.getInstructor())
                .materials(education.getMaterials())
                .isActive(education.getIsActive())
                .createdAt(education.getCreatedAt())
                .updatedAt(education.getUpdatedAt())
                .build();
    }
}
