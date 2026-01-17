package com.insuretech.pms.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing upstream or downstream lineage as a tree structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageTreeDto {

    private LineageNodeDto root;

    private List<LineageNodeDto> nodes;

    private List<LineageEdgeDto> edges;

    private int maxDepth;

    private int totalNodes;
}
