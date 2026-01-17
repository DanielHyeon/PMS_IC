package com.insuretech.pms.project.service;

import com.insuretech.pms.project.entity.Backlog;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.repository.BacklogItemRepository;
import com.insuretech.pms.project.repository.BacklogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BacklogService Tests")
class BacklogServiceTest {

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private BacklogItemRepository backlogItemRepository;

    @InjectMocks
    private BacklogService backlogService;

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
        testItem1.setOriginType(BacklogItem.BacklogItemOrigin.MANUAL);

        testItem2 = new BacklogItem();
        testItem2.setId("item-002");
        testItem2.setBacklog(testBacklog);
        testItem2.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        testItem2.setPriorityOrder(1);
        testItem2.setStoryPoints(3);
        testItem2.setOriginType(BacklogItem.BacklogItemOrigin.MANUAL);
    }

    @Nested
    @DisplayName("Get or Create Backlog")
    class GetOrCreateBacklogTests {

        @Test
        @DisplayName("Should get existing backlog for project")
        void shouldGetExistingBacklog() {
            when(backlogRepository.findActiveBacklogByProjectId(PROJECT_ID))
                    .thenReturn(Optional.of(testBacklog));

            Backlog result = backlogService.getOrCreateBacklog(PROJECT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(BACKLOG_ID);
            assertThat(result.getProjectId()).isEqualTo(PROJECT_ID);
            verify(backlogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new backlog if not exists")
        void shouldCreateNewBacklog() {
            when(backlogRepository.findActiveBacklogByProjectId(PROJECT_ID))
                    .thenReturn(Optional.empty());
            when(backlogRepository.save(any(Backlog.class)))
                    .thenReturn(testBacklog);

            Backlog result = backlogService.getOrCreateBacklog(PROJECT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isEqualTo(PROJECT_ID);
            verify(backlogRepository, times(1)).save(any(Backlog.class));
        }
    }

    @Nested
    @DisplayName("Get Backlog Items")
    class GetBacklogItemsTests {

        @Test
        @DisplayName("Should get all items sorted by priority")
        void shouldGetAllItemsSortedByPriority() {
            List<BacklogItem> items = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findByBacklogIdOrderByPriorityOrderAsc(BACKLOG_ID))
                    .thenReturn(items);

            List<BacklogItem> result = backlogService.getBacklogItems(BACKLOG_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPriorityOrder()).isEqualTo(0);
            assertThat(result.get(1).getPriorityOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get items by status")
        void shouldGetItemsByStatus() {
            List<BacklogItem> selectedItems = Arrays.asList(testItem1);
            when(backlogItemRepository.findByBacklogIdAndStatus(BACKLOG_ID, "SELECTED"))
                    .thenReturn(selectedItems);

            List<BacklogItem> result = backlogService.getBacklogItemsByStatus(BACKLOG_ID, "SELECTED");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.SELECTED);
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperationsTests {

        @Test
        @DisplayName("Should count items by status")
        void shouldCountItemsByStatus() {
            when(backlogItemRepository.countByBacklogIdAndStatus(BACKLOG_ID, "BACKLOG"))
                    .thenReturn(2L);

            long count = backlogService.countItemsByStatus(BACKLOG_ID, "BACKLOG");

            assertThat(count).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should calculate total story points for selected items")
        void shouldCalculateTotalStoryPoints() {
            when(backlogItemRepository.sumStoryPointsForSelectedItems(BACKLOG_ID))
                    .thenReturn(8);

            Integer total = backlogService.getTotalStoryPointsForSelectedItems(BACKLOG_ID);

            assertThat(total).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Effort Calculation")
    class EffortCalculationTests {

        @Test
        @DisplayName("Should calculate total effort for selected items")
        void shouldCalculateTotalEffort() {
            testItem1.setEstimatedEffortHours(10);
            testItem2.setEstimatedEffortHours(5);
            List<BacklogItem> selectedItems = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(selectedItems);

            Integer totalEffort = backlogService.calculateTotalEffortForSelectedItems(BACKLOG_ID);

            assertThat(totalEffort).isEqualTo(15);
        }

        @Test
        @DisplayName("Should handle null effort hours in calculation")
        void shouldHandleNullEffortHours() {
            testItem1.setEstimatedEffortHours(null);
            testItem2.setEstimatedEffortHours(5);
            List<BacklogItem> selectedItems = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(selectedItems);

            Integer totalEffort = backlogService.calculateTotalEffortForSelectedItems(BACKLOG_ID);

            assertThat(totalEffort).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Selected Items Operations")
    class SelectedItemsOperationsTests {

        @Test
        @DisplayName("Should get selected items for sprint planning")
        void shouldGetSelectedItems() {
            List<BacklogItem> selectedItems = Arrays.asList(testItem1);
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(selectedItems);

            List<BacklogItem> result = backlogService.getSelectedItemsForSprintPlanning(BACKLOG_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.SELECTED);
        }
    }
}
