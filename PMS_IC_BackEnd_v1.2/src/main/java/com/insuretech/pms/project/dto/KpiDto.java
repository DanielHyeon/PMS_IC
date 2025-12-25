package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Kpi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiDto {
    private String id;
    private String phaseId;
    private String name;
    private String target;
    private String current;
    private String status;

    public static KpiDto from(Kpi kpi) {
        return KpiDto.builder()
                .id(kpi.getId())
                .phaseId(kpi.getPhase() != null ? kpi.getPhase().getId() : null)
                .name(kpi.getName())
                .target(kpi.getTarget())
                .current(kpi.getCurrent())
                .status(kpi.getStatus() != null ? kpi.getStatus().name() : null)
                .build();
    }
}
