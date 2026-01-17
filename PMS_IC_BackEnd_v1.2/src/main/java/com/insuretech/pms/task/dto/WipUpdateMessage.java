package com.insuretech.pms.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * WebSocket message for real-time WIP updates
 * Sent from server to clients subscribed to /topic/wip/{projectId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WipUpdateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Message type indicating the nature of the update
     * - INITIAL_LOAD: Initial project WIP status on subscription
     * - COLUMN_UPDATE: Column WIP status changed
     * - SPRINT_UPDATE: Sprint CONWIP status changed
     * - WIP_VIOLATION: WIP soft or hard limit violated
     * - BOTTLENECK_DETECTED: Workflow bottleneck detected
     * - ERROR: Error occurred during processing
     */
    @JsonProperty("type")
    private String type;

    /**
     * Project ID this message is associated with
     */
    @JsonProperty("projectId")
    private String projectId;

    /**
     * Message payload containing update details
     * Structure varies by message type:
     * - INITIAL_LOAD: Full project WIP status (columnStatuses array, totalWip count)
     * - COLUMN_UPDATE: {columnId, currentWip, wipLimitSoft, wipLimitHard, health}
     * - SPRINT_UPDATE: {sprintId, conwipUsed, conwipLimit, percentageUsed}
     * - WIP_VIOLATION: {columnId, columnName, currentWip, limitType, health}
     * - BOTTLENECK_DETECTED: {columnId, columnName, blockingTasks, affectedTasks}
     * - ERROR: {error: error message}
     */
    @JsonProperty("data")
    private Map<String, Object> data;

    /**
     * Server timestamp when message was created (milliseconds)
     */
    @JsonProperty("timestamp")
    private long timestamp;
}
