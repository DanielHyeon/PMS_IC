package com.insuretech.pms.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing a node in the lineage graph.
 * Can be a Requirement, UserStory, Task, or Sprint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageNodeDto {

    private String id;

    private LineageNodeType type;

    private String code;

    private String title;

    private String status;

    private Map<String, Object> metadata;

    public enum LineageNodeType {
        REQUIREMENT,
        USER_STORY,
        TASK,
        SPRINT
    }
}
