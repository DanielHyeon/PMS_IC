package com.insuretech.pms.education.dto;

import com.insuretech.pms.education.entity.EducationRoadmap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationRoadmapDto {
    private String id;
    private String educationId;
    private String educationTitle;
    private String targetRole;
    private String level;
    private Integer orderNum;
    private Boolean isRequired;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EducationRoadmapDto from(EducationRoadmap roadmap) {
        return EducationRoadmapDto.builder()
                .id(roadmap.getId())
                .educationId(roadmap.getEducation() != null ? roadmap.getEducation().getId() : null)
                .educationTitle(roadmap.getEducation() != null ? roadmap.getEducation().getTitle() : null)
                .targetRole(roadmap.getTargetRole() != null ? roadmap.getTargetRole().name() : null)
                .level(roadmap.getLevel() != null ? roadmap.getLevel().name() : null)
                .orderNum(roadmap.getOrderNum())
                .isRequired(roadmap.getIsRequired())
                .description(roadmap.getDescription())
                .createdAt(roadmap.getCreatedAt())
                .updatedAt(roadmap.getUpdatedAt())
                .build();
    }
}
