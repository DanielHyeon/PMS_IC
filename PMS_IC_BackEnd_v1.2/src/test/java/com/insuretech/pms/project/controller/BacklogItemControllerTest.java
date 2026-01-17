package com.insuretech.pms.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insuretech.pms.project.dto.CreateBacklogItemRequest;
import com.insuretech.pms.project.dto.UpdateBacklogItemRequest;
import com.insuretech.pms.project.entity.Backlog;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.service.BacklogItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BacklogItemController.class)
@DisplayName("BacklogItemController Tests")
class BacklogItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BacklogItemService backlogItemService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PROJECT_ID = "proj-001";
    private static final String BACKLOG_ID = "backlog-001";
    private static final String ITEM_ID = "item-001";
    private static final String REQUIREMENT_ID = "req-001";

    private Backlog testBacklog;
    private BacklogItem testItem;

    @BeforeEach
    void setUp() {
        testBacklog = new Backlog();
        testBacklog.setId(BACKLOG_ID);
        testBacklog.setProjectId(PROJECT_ID);
        testBacklog.setStatus(Backlog.BacklogStatus.ACTIVE);

        testItem = new BacklogItem();
        testItem.setId(ITEM_ID);
        testItem.setBacklog(testBacklog);
        testItem.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        testItem.setPriorityOrder(0);
        testItem.setStoryPoints(5);
        testItem.setOriginType(BacklogItem.BacklogItemOrigin.MANUAL);
    }

    @Test
    @DisplayName("POST /from-requirement should create item from requirement")
    void testCreateFromRequirement() throws Exception {
        when(backlogItemService.createBacklogItemFromRequirement(BACKLOG_ID, REQUIREMENT_ID))
                .thenReturn(testItem);

        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/from-requirement", PROJECT_ID, BACKLOG_ID)
                .param("requirementId", REQUIREMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ITEM_ID))
                .andExpect(jsonPath("$.data.originType").value("MANUAL"));

        verify(backlogItemService).createBacklogItemFromRequirement(BACKLOG_ID, REQUIREMENT_ID);
    }

    @Test
    @DisplayName("POST /manual should create manual backlog item")
    void testCreateManual() throws Exception {
        CreateBacklogItemRequest request = new CreateBacklogItemRequest();
        request.setTitle("New Feature");
        request.setStoryPoints(5);

        when(backlogItemService.createManualBacklogItem(BACKLOG_ID, "New Feature", 5))
                .thenReturn(testItem);

        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/manual", PROJECT_ID, BACKLOG_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ITEM_ID));

        verify(backlogItemService).createManualBacklogItem(eq(BACKLOG_ID), eq("New Feature"), eq(5));
    }

    @Test
    @DisplayName("GET /{itemId} should return backlog item")
    void testGetBacklogItem() throws Exception {
        when(backlogItemService.getBacklogItem(ITEM_ID))
                .thenReturn(testItem);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ITEM_ID))
                .andExpect(jsonPath("$.data.status").value("BACKLOG"));

        verify(backlogItemService).getBacklogItem(ITEM_ID);
    }

    @Test
    @DisplayName("PUT /{itemId}/story-points should update story points")
    void testUpdateStoryPoints() throws Exception {
        testItem.setStoryPoints(8);
        when(backlogItemService.updateStoryPoints(ITEM_ID, 8))
                .thenReturn(testItem);

        mockMvc.perform(put("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/story-points", PROJECT_ID, BACKLOG_ID, ITEM_ID)
                .param("storyPoints", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storyPoints").value(8));

        verify(backlogItemService).updateStoryPoints(ITEM_ID, 8);
    }

    @Test
    @DisplayName("PUT /{itemId}/estimated-effort should update effort hours")
    void testUpdateEstimatedEffort() throws Exception {
        testItem.setEstimatedEffortHours(10);
        when(backlogItemService.updateEstimatedEffort(ITEM_ID, 10))
                .thenReturn(testItem);

        mockMvc.perform(put("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/estimated-effort", PROJECT_ID, BACKLOG_ID, ITEM_ID)
                .param("effortHours", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedEffortHours").value(10));

        verify(backlogItemService).updateEstimatedEffort(ITEM_ID, 10);
    }

    @Test
    @DisplayName("PUT /{itemId} should update backlog item")
    void testUpdateBacklogItem() throws Exception {
        UpdateBacklogItemRequest request = new UpdateBacklogItemRequest();
        request.setStoryPoints(8);
        request.setAcceptanceCriteria("Test criteria");

        testItem.setStoryPoints(8);
        testItem.setAcceptanceCriteria("Test criteria");

        when(backlogItemService.getBacklogItem(ITEM_ID))
                .thenReturn(testItem);
        when(backlogItemService.updateStoryPoints(ITEM_ID, 8))
                .thenReturn(testItem);

        mockMvc.perform(put("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}", PROJECT_ID, BACKLOG_ID, ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storyPoints").value(8));

        verify(backlogItemService).getBacklogItem(ITEM_ID);
    }

    @Test
    @DisplayName("POST /{itemId}/select-for-sprint should select item")
    void testSelectForSprintPlanning() throws Exception {
        testItem.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
        when(backlogItemService.selectForSprintPlanning(ITEM_ID))
                .thenReturn(testItem);

        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/select-for-sprint", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SELECTED"));

        verify(backlogItemService).selectForSprintPlanning(ITEM_ID);
    }

    @Test
    @DisplayName("POST /{itemId}/move-to-sprint should move to sprint")
    void testMoveToSprint() throws Exception {
        testItem.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
        when(backlogItemService.moveToSprint(ITEM_ID))
                .thenReturn(testItem);

        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/move-to-sprint", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SPRINT"));

        verify(backlogItemService).moveToSprint(ITEM_ID);
    }

    @Test
    @DisplayName("POST /{itemId}/complete should mark as completed")
    void testCompleteBacklogItem() throws Exception {
        testItem.setStatus(BacklogItem.BacklogItemStatus.COMPLETED);
        when(backlogItemService.completeBacklogItem(ITEM_ID))
                .thenReturn(testItem);

        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/complete", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(backlogItemService).completeBacklogItem(ITEM_ID);
    }

    @Test
    @DisplayName("POST /{itemId}/move-back-to-backlog should deselect item")
    void testMoveBackToBacklog() throws Exception {
        testItem.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        when(backlogItemService.moveBackToBacklog(ITEM_ID))
                .thenReturn(testItem);

        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/move-back-to-backlog", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BACKLOG"));

        verify(backlogItemService).moveBackToBacklog(ITEM_ID);
    }

    @Test
    @DisplayName("DELETE /{itemId} should delete backlog item")
    void testDeleteBacklogItem() throws Exception {
        mockMvc.perform(delete("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isNoContent());

        verify(backlogItemService).deleteBacklogItem(ITEM_ID);
    }

    @Test
    @DisplayName("POST /{itemId}/sync-story-points should sync from requirement")
    void testSyncStoryPointsFromRequirement() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/backlogs/{backlogId}/items/{itemId}/sync-story-points", PROJECT_ID, BACKLOG_ID, ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Story points synchronized"));

        verify(backlogItemService).syncStoryPointsFromRequirement(ITEM_ID);
    }
}
