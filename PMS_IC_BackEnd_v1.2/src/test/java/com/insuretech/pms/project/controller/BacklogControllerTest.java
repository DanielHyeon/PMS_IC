package com.insuretech.pms.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.BacklogDto;
import com.insuretech.pms.project.dto.BacklogItemDto;
import com.insuretech.pms.project.dto.ReorderBacklogItemsRequest;
import com.insuretech.pms.project.dto.SprintPlanningCapacityDto;
import com.insuretech.pms.project.entity.Backlog;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.service.BacklogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BacklogController.class)
@DisplayName("BacklogController Tests")
class BacklogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BacklogService backlogService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PROJECT_ID = "proj-001";
    private static final String BACKLOG_ID = "backlog-001";

    private Backlog testBacklog;
    private BacklogItem testItem1;
    private BacklogItem testItem2;

    @BeforeEach
    void setUp() {
        testBacklog = new Backlog();
        testBacklog.setId(BACKLOG_ID);
        testBacklog.setProjectId(PROJECT_ID);
        testBacklog.setName("Product Backlog");
        testBacklog.setStatus(Backlog.BacklogStatus.ACTIVE);

        testItem1 = new BacklogItem();
        testItem1.setId("item-001");
        testItem1.setBacklog(testBacklog);
        testItem1.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        testItem1.setPriorityOrder(0);
        testItem1.setStoryPoints(5);

        testItem2 = new BacklogItem();
        testItem2.setId("item-002");
        testItem2.setBacklog(testBacklog);
        testItem2.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
        testItem2.setPriorityOrder(1);
        testItem2.setStoryPoints(3);
    }

    @Test
    @DisplayName("GET /active should return active backlog")
    void testGetOrCreateActiveBacklog() throws Exception {
        when(backlogService.getOrCreateBacklog(PROJECT_ID))
                .thenReturn(testBacklog);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/active", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(BACKLOG_ID))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID));

        verify(backlogService).getOrCreateBacklog(PROJECT_ID);
    }

    @Test
    @DisplayName("GET /{backlogId}/items should return all items sorted by priority")
    void testGetBacklogItems() throws Exception {
        List<BacklogItem> items = Arrays.asList(testItem1, testItem2);
        when(backlogService.getBacklogItems(BACKLOG_ID))
                .thenReturn(items);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/{backlogId}/items", PROJECT_ID, BACKLOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("item-001"))
                .andExpect(jsonPath("$.data[1].id").value("item-002"));

        verify(backlogService).getBacklogItems(BACKLOG_ID);
    }

    @Test
    @DisplayName("GET /{backlogId}/items/by-status should filter by status")
    void testGetBacklogItemsByStatus() throws Exception {
        List<BacklogItem> selectedItems = Arrays.asList(testItem2);
        when(backlogService.getBacklogItemsByStatus(BACKLOG_ID, "SELECTED"))
                .thenReturn(selectedItems);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/{backlogId}/items/by-status", PROJECT_ID, BACKLOG_ID)
                .param("status", "SELECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("SELECTED"));

        verify(backlogService).getBacklogItemsByStatus(BACKLOG_ID, "SELECTED");
    }

    @Test
    @DisplayName("GET /{backlogId}/sprint-planning-capacity should return capacity metrics")
    void testGetSprintPlanningCapacity() throws Exception {
        List<BacklogItem> selectedItems = Arrays.asList(testItem2);
        when(backlogService.getSelectedItemsForSprintPlanning(BACKLOG_ID))
                .thenReturn(selectedItems);
        when(backlogService.countItemsByStatus(BACKLOG_ID, "BACKLOG"))
                .thenReturn(1L);
        when(backlogService.countItemsByStatus(BACKLOG_ID, "SELECTED"))
                .thenReturn(1L);
        when(backlogService.countItemsByStatus(BACKLOG_ID, "SPRINT"))
                .thenReturn(0L);
        when(backlogService.countItemsByStatus(BACKLOG_ID, "COMPLETED"))
                .thenReturn(0L);
        when(backlogService.getTotalStoryPointsForSelectedItems(BACKLOG_ID))
                .thenReturn(3);
        when(backlogService.calculateTotalEffortForSelectedItems(BACKLOG_ID))
                .thenReturn(5);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/{backlogId}/sprint-planning-capacity", PROJECT_ID, BACKLOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.backlogItemCount").value(1))
                .andExpect(jsonPath("$.data.selectedItemCount").value(1))
                .andExpect(jsonPath("$.data.totalStoryPointsForSelected").value(3));

        verify(backlogService).getSelectedItemsForSprintPlanning(BACKLOG_ID);
    }

    @Test
    @DisplayName("PUT /{backlogId}/items/reorder should reorder items")
    void testReorderBacklogItems() throws Exception {
        ReorderBacklogItemsRequest request = new ReorderBacklogItemsRequest();
        request.setItemIds(Arrays.asList("item-002", "item-001"));

        mockMvc.perform(put("/api/projects/{projectId}/backlogs/{backlogId}/items/reorder", PROJECT_ID, BACKLOG_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(backlogService).reorderBacklogItems(eq(BACKLOG_ID), anyList());
    }

    @Test
    @DisplayName("GET /{backlogId}/selected-story-points should return total story points")
    void testGetSelectedStoryPoints() throws Exception {
        when(backlogService.getTotalStoryPointsForSelectedItems(BACKLOG_ID))
                .thenReturn(8);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/{backlogId}/selected-story-points", PROJECT_ID, BACKLOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(8));

        verify(backlogService).getTotalStoryPointsForSelectedItems(BACKLOG_ID);
    }

    @Test
    @DisplayName("GET /{backlogId}/item-count-by-status should return count by status")
    void testGetItemCountByStatus() throws Exception {
        when(backlogService.countItemsByStatus(BACKLOG_ID, "SELECTED"))
                .thenReturn(1L);

        mockMvc.perform(get("/api/projects/{projectId}/backlogs/{backlogId}/item-count-by-status", PROJECT_ID, BACKLOG_ID)
                .param("status", "SELECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        verify(backlogService).countItemsByStatus(BACKLOG_ID, "SELECTED");
    }
}
