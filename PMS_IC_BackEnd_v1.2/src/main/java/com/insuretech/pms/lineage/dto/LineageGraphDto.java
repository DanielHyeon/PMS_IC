package com.insuretech.pms.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the complete lineage graph for a project.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageGraphDto {

    private List<LineageNodeDto> nodes;

    private List<LineageEdgeDto> edges;

    private LineageStatisticsDto statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineageStatisticsDto {
        private int requirements;
        private int stories;
        private int tasks;
        private int sprints;
        private double coverage;
        private int linkedRequirements;
        private int unlinkedRequirements;
    }
}
