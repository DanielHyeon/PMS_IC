package com.insuretech.pms.lineage.dto;

import com.insuretech.pms.lineage.entity.LineageEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing a lineage event for the activity timeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageEventDto {

    private String id;

    private LineageEventType eventType;

    private String aggregateType;

    private String aggregateId;

    private String entityCode;

    private String entityTitle;

    private String actorId;

    private String actorName;

    private LocalDateTime timestamp;

    private Map<String, Object> changes;

    private String description;
}
