package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Phase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhaseDto {
    private String id;
    private String projectId;
    private String name;
    private Integer orderNum;
    private String status;
    private String gateStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progress;
    private String description;

    public static PhaseDto from(Phase phase) {
        return PhaseDto.builder()
                .id(phase.getId())
                .projectId(phase.getProject() != null ? phase.getProject().getId() : null)
                .name(phase.getName())
                .orderNum(phase.getOrderNum())
                .status(phase.getStatus().name())
                .gateStatus(phase.getGateStatus() != null ? phase.getGateStatus().name() : null)
                .startDate(phase.getStartDate())
                .endDate(phase.getEndDate())
                .progress(phase.getProgress())
                .description(phase.getDescription())
                .build();
    }
}