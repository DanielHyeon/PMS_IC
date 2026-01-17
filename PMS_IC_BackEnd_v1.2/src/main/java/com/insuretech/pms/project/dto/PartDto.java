package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Part;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartDto {
    private String id;
    private String name;
    private String description;
    private String projectId;
    private String leaderId;
    private String leaderName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progress;
    private Set<String> memberIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PartDto from(Part part) {
        return PartDto.builder()
                .id(part.getId())
                .name(part.getName())
                .description(part.getDescription())
                .projectId(part.getProject() != null ? part.getProject().getId() : null)
                .leaderId(part.getLeaderId())
                .leaderName(part.getLeaderName())
                .status(part.getStatus().name())
                .startDate(part.getStartDate())
                .endDate(part.getEndDate())
                .progress(part.getProgress())
                .memberIds(part.getMemberIds())
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }
}
