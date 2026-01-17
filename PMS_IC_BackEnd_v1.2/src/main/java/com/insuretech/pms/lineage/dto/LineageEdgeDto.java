package com.insuretech.pms.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing an edge (relationship) in the lineage graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageEdgeDto {

    private String id;

    private String source;

    private String target;

    private LineageRelationship relationship;

    private LocalDateTime createdAt;

    public enum LineageRelationship {
        DERIVES,
        BREAKS_DOWN_TO,
        IMPLEMENTED_BY,
        BELONGS_TO_SPRINT
    }
}
