package com.insuretech.pms.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing impact analysis results.
 * Shows what would be affected if an entity changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactAnalysisDto {

    private String sourceId;

    private String sourceType;

    private String sourceTitle;

    private int impactedStories;

    private int impactedTasks;

    private int impactedSprints;

    private List<ImpactedEntityDto> directImpacts;

    private List<ImpactedEntityDto> indirectImpacts;

    private List<String> affectedSprintNames;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactedEntityDto {
        private String id;
        private String type;
        private String code;
        private String title;
        private String status;
        private ImpactLevel impactLevel;
        private int depth;
    }

    public enum ImpactLevel {
        DIRECT,
        INDIRECT,
        TRANSITIVE
    }
}
