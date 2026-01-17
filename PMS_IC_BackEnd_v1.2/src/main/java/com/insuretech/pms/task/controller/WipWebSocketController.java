package com.insuretech.pms.task.controller;

import com.insuretech.pms.task.dto.WipUpdateMessage;
import com.insuretech.pms.task.service.WipValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for real-time WIP updates
 * Handles STOMP messages for project WIP status monitoring
 *
 * Message flow:
 * - Client connects to /ws/wip
 * - Client sends subscribe request to /app/subscribe with projectId
 * - Server broadcasts updates to /topic/wip/{projectId}
 * - Server broadcasts violations to /topic/violations/{projectId}
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WipWebSocketController {

    private final WipValidationService wipValidationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle client subscription to project WIP updates
     * Client sends: {projectId: "..."} to /app/subscribe
     * Server broadcasts: WIP status to /topic/wip/{projectId}
     */
    @MessageMapping("/subscribe")
    public void subscribe(@Payload Map<String, String> message) {
        String projectId = message.get("projectId");
        if (projectId == null || projectId.isEmpty()) {
            log.warn("Subscribe request received without projectId");
            return;
        }

        log.info("Client subscribing to WIP updates for project: {}", projectId);

        try {
            // Get current project WIP status
            Map<String, Object> wipStatus = wipValidationService.getProjectWipStatus(projectId);

            // Create update message
            WipUpdateMessage updateMessage = WipUpdateMessage.builder()
                    .type("INITIAL_LOAD")
                    .projectId(projectId)
                    .data(wipStatus)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Broadcast to all subscribers of this project's WIP topic
            messagingTemplate.convertAndSend("/topic/wip/" + projectId, updateMessage);

            log.info("Sent initial WIP status for project: {}", projectId);
        } catch (Exception e) {
            log.error("Error sending initial WIP status for project {}: {}", projectId, e.getMessage(), e);
            sendError(projectId, "Failed to load initial WIP status: " + e.getMessage());
        }
    }

    /**
     * Handle client ping/heartbeat
     * Keeps connection alive and detects disconnected clients
     */
    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, String> message) {
        String projectId = message.get("projectId");
        if (projectId != null && !projectId.isEmpty()) {
            log.debug("Heartbeat received from client for project: {}", projectId);
        }
    }

    /**
     * Broadcast WIP violation to specific project subscribers
     * Called by WipValidationService when violation is detected
     */
    public void broadcastViolation(String projectId, Map<String, Object> violationData) {
        try {
            WipUpdateMessage message = WipUpdateMessage.builder()
                    .type("WIP_VIOLATION")
                    .projectId(projectId)
                    .data(violationData)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/violations/" + projectId, message);
            log.info("Broadcasted WIP violation for project: {}", projectId);
        } catch (Exception e) {
            log.error("Error broadcasting violation for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast column update to specific project subscribers
     * Called when column WIP status changes
     */
    public void broadcastColumnUpdate(String projectId, String columnId, Map<String, Object> columnData) {
        try {
            Map<String, Object> data = Map.of(
                    "columnId", columnId,
                    "columnData", columnData
            );

            WipUpdateMessage message = WipUpdateMessage.builder()
                    .type("COLUMN_UPDATE")
                    .projectId(projectId)
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/wip/" + projectId, message);
            log.info("Broadcasted column update for project: {}, column: {}", projectId, columnId);
        } catch (Exception e) {
            log.error("Error broadcasting column update for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast sprint update to specific project subscribers
     * Called when sprint WIP status changes
     */
    public void broadcastSprintUpdate(String projectId, String sprintId, Map<String, Object> sprintData) {
        try {
            Map<String, Object> data = Map.of(
                    "sprintId", sprintId,
                    "sprintData", sprintData
            );

            WipUpdateMessage message = WipUpdateMessage.builder()
                    .type("SPRINT_UPDATE")
                    .projectId(projectId)
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/wip/" + projectId, message);
            log.info("Broadcasted sprint update for project: {}, sprint: {}", projectId, sprintId);
        } catch (Exception e) {
            log.error("Error broadcasting sprint update for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast bottleneck detection to specific project subscribers
     * Called when bottleneck is detected in workflow
     */
    public void broadcastBottleneckDetected(String projectId, Map<String, Object> bottleneckData) {
        try {
            WipUpdateMessage message = WipUpdateMessage.builder()
                    .type("BOTTLENECK_DETECTED")
                    .projectId(projectId)
                    .data(bottleneckData)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/alerts/" + projectId, message);
            log.info("Broadcasted bottleneck detection for project: {}", projectId);
        } catch (Exception e) {
            log.error("Error broadcasting bottleneck for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Send error message to project subscribers
     */
    private void sendError(String projectId, String errorMessage) {
        try {
            WipUpdateMessage message = WipUpdateMessage.builder()
                    .type("ERROR")
                    .projectId(projectId)
                    .data(Map.of("error", errorMessage))
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/errors/" + projectId, message);
        } catch (Exception e) {
            log.error("Error sending error message for project {}: {}", projectId, e.getMessage(), e);
        }
    }
}
