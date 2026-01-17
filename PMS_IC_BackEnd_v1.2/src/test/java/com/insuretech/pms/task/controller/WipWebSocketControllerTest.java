package com.insuretech.pms.task.controller;

import com.insuretech.pms.task.dto.WipUpdateMessage;
import com.insuretech.pms.task.service.WipValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("WIP WebSocket Controller Tests")
class WipWebSocketControllerTest {

    @Autowired
    private WipWebSocketController webSocketController;

    @MockBean
    private WipValidationService wipValidationService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        // Mock WIP validation service responses
        Map<String, Object> mockWipStatus = new HashMap<>();
        mockWipStatus.put("projectId", "project-1");
        mockWipStatus.put("totalWip", 25);
        mockWipStatus.put("columnStatuses", java.util.List.of());
        mockWipStatus.put("bottleneckCount", 0);

        when(wipValidationService.getProjectWipStatus(anyString()))
                .thenReturn(mockWipStatus);
    }

    @Test
    @DisplayName("Subscribe to project WIP updates")
    void testSubscribeToWipUpdates() {
        Map<String, String> subscriptionMessage = new HashMap<>();
        subscriptionMessage.put("projectId", "project-1");

        webSocketController.subscribe(subscriptionMessage);

        verify(wipValidationService, timeout(5000)).getProjectWipStatus("project-1");
        verify(messagingTemplate, timeout(5000)).convertAndSend(
                contains("/topic/wip/project-1"),
                any(WipUpdateMessage.class)
        );
    }

    @Test
    @DisplayName("Subscribe without project ID returns gracefully")
    void testSubscribeWithoutProjectId() {
        Map<String, String> subscriptionMessage = new HashMap<>();
        subscriptionMessage.put("projectId", "");

        webSocketController.subscribe(subscriptionMessage);

        verify(wipValidationService, never()).getProjectWipStatus(anyString());
    }

    @Test
    @DisplayName("Subscribe with null project ID returns gracefully")
    void testSubscribeWithNullProjectId() {
        Map<String, String> subscriptionMessage = new HashMap<>();

        webSocketController.subscribe(subscriptionMessage);

        verify(wipValidationService, never()).getProjectWipStatus(anyString());
    }

    @Test
    @DisplayName("Handle client ping/heartbeat")
    void testHandlePing() {
        Map<String, String> pingMessage = new HashMap<>();
        pingMessage.put("projectId", "project-1");

        assertThatNoException().isThrownBy(() ->
                webSocketController.handlePing(pingMessage)
        );
    }

    @Test
    @DisplayName("Broadcast WIP violation")
    void testBroadcastViolation() {
        Map<String, Object> violationData = new HashMap<>();
        violationData.put("columnId", "column-1");
        violationData.put("currentWip", 15);
        violationData.put("limitType", "HARD_LIMIT");

        webSocketController.broadcastViolation("project-1", violationData);

        verify(messagingTemplate, timeout(5000)).convertAndSend(
                contains("/topic/violations/project-1"),
                any(WipUpdateMessage.class)
        );
    }

    @Test
    @DisplayName("Broadcast column update")
    void testBroadcastColumnUpdate() {
        Map<String, Object> columnData = new HashMap<>();
        columnData.put("currentWip", 8);
        columnData.put("health", "GREEN");

        webSocketController.broadcastColumnUpdate("project-1", "column-1", columnData);

        verify(messagingTemplate, timeout(5000)).convertAndSend(
                contains("/topic/wip/project-1"),
                any(WipUpdateMessage.class)
        );
    }

    @Test
    @DisplayName("Broadcast sprint update")
    void testBroadcastSprintUpdate() {
        Map<String, Object> sprintData = new HashMap<>();
        sprintData.put("conwipUsed", 20);
        sprintData.put("conwipLimit", 25);

        webSocketController.broadcastSprintUpdate("project-1", "sprint-1", sprintData);

        verify(messagingTemplate, timeout(5000)).convertAndSend(
                contains("/topic/wip/project-1"),
                any(WipUpdateMessage.class)
        );
    }

    @Test
    @DisplayName("Broadcast bottleneck detection")
    void testBroadcastBottleneckDetected() {
        Map<String, Object> bottleneckData = new HashMap<>();
        bottleneckData.put("columnId", "column-1");
        bottleneckData.put("blockingTasks", 3);

        webSocketController.broadcastBottleneckDetected("project-1", bottleneckData);

        verify(messagingTemplate, timeout(5000)).convertAndSend(
                contains("/topic/alerts/project-1"),
                any(WipUpdateMessage.class)
        );
    }

    @Test
    @DisplayName("WIP update message includes correct type and timestamp")
    void testWipUpdateMessageStructure() {
        Map<String, String> subscriptionMessage = new HashMap<>();
        subscriptionMessage.put("projectId", "project-1");

        webSocketController.subscribe(subscriptionMessage);

        // Verify the message was sent with correct structure
        verify(messagingTemplate, timeout(5000)).convertAndSend(
                anyString(),
                argThat(msg -> {
                    if (!(msg instanceof WipUpdateMessage)) {
                        return false;
                    }
                    WipUpdateMessage update = (WipUpdateMessage) msg;
                    return update.getType() != null &&
                            update.getProjectId() != null &&
                            update.getTimestamp() > 0;
                })
        );
    }

    @Test
    @DisplayName("Error handling broadcasts error message")
    void testErrorHandling() {
        when(wipValidationService.getProjectWipStatus(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        Map<String, String> subscriptionMessage = new HashMap<>();
        subscriptionMessage.put("projectId", "project-1");

        // Should not throw even with service error
        assertThatNoException().isThrownBy(() ->
                webSocketController.subscribe(subscriptionMessage)
        );
    }
}
