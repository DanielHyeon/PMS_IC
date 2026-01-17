package com.insuretech.pms.project.service;

import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.repository.BacklogItemRepository;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SprintPlanningService Tests")
class SprintPlanningServiceTest {

    @Mock
    private BacklogItemRepository backlogItemRepository;

    @Mock
    private SprintRepository sprintRepository;

    @InjectMocks
    private SprintPlanningService sprintPlanningService;

    private static final String PROJECT_ID = "proj-001";
    private static final String BACKLOG_ID = "backlog-001";
    private static final String SPRINT_ID = "sprint-001";
    private static final String ITEM_ID = "item-001";

    private Sprint testSprint;
    private BacklogItem testItem1;
    private BacklogItem testItem2;

    @BeforeEach
    void setUp() {
        testSprint = new Sprint();
        testSprint.setId(SPRINT_ID);
        testSprint.setProjectId(PROJECT_ID);
        testSprint.setName("Sprint 1");
        testSprint.setGoal("Complete core features");
        testSprint.setStartDate(LocalDate.now());
        testSprint.setEndDate(LocalDate.now().plusDays(14));
        testSprint.setStatus(Sprint.SprintStatus.PLANNED);

        testItem1 = new BacklogItem();
        testItem1.setId("item-001");
        testItem1.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
        testItem1.setStoryPoints(5);
        testItem1.setEstimatedEffortHours(10);
        testItem1.setPriorityOrder(0);

        testItem2 = new BacklogItem();
        testItem2.setId("item-002");
        testItem2.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
        testItem2.setStoryPoints(3);
        testItem2.setEstimatedEffortHours(5);
        testItem2.setPriorityOrder(1);
    }

    @Nested
    @DisplayName("Create Sprint from Selected Items")
    class CreateSprintFromSelectedTests {

        @Test
        @DisplayName("Should create sprint from selected backlog items")
        void shouldCreateSprintFromSelected() {
            List<BacklogItem> selectedItems = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(selectedItems);
            when(sprintRepository.save(any(Sprint.class)))
                    .thenReturn(testSprint);
            when(backlogItemRepository.saveAll(anyList()))
                    .thenReturn(selectedItems);

            Sprint result = sprintPlanningService.createSprintFromSelectedItems(
                    PROJECT_ID, "Sprint 1", "Complete features", LocalDate.now(), LocalDate.now().plusDays(14), BACKLOG_ID);

            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(result.getStatus()).isEqualTo(Sprint.SprintStatus.PLANNED);
            verify(sprintRepository).save(any(Sprint.class));
        }

        @Test
        @DisplayName("Should throw exception when no selected items exist")
        void shouldThrowExceptionWhenNoSelectedItems() {
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(Arrays.asList());

            assertThatThrownBy(() ->
                    sprintPlanningService.createSprintFromSelectedItems(
                            PROJECT_ID, "Sprint 1", "Goal", LocalDate.now(), LocalDate.now().plusDays(14), BACKLOG_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("selected items");
        }
    }

    @Nested
    @DisplayName("Move Items to Sprint")
    class MoveItemsToSprintTests {

        @Test
        @DisplayName("Should move selected items to sprint")
        void shouldMoveItemsToSprint() {
            List<BacklogItem> selectedItems = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(selectedItems);
            when(backlogItemRepository.saveAll(anyList()))
                    .thenReturn(selectedItems);

            List<BacklogItem> result = sprintPlanningService.moveItemsToSprint(BACKLOG_ID, SPRINT_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.SPRINT);
            assertThat(result.get(0).getSprintId()).isEqualTo(SPRINT_ID);
            verify(backlogItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception when items have no story points")
        void shouldThrowExceptionWhenNoStoryPoints() {
            testItem1.setStoryPoints(null);
            testItem2.setStoryPoints(null);
            List<BacklogItem> selectedItems = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findSelectedItemsForSprintPlanning(BACKLOG_ID))
                    .thenReturn(selectedItems);

            assertThatThrownBy(() -> sprintPlanningService.moveItemsToSprint(BACKLOG_ID, SPRINT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("story points");
        }
    }

    @Nested
    @DisplayName("Sprint Item Operations")
    class SprintItemOperationsTests {

        @Test
        @DisplayName("Should get sprint items")
        void shouldGetSprintItems() {
            testItem1.setSprintId(SPRINT_ID);
            testItem2.setSprintId(SPRINT_ID);
            List<BacklogItem> items = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findBySprintId(SPRINT_ID))
                    .thenReturn(items);

            List<BacklogItem> result = sprintPlanningService.getSprintItems(SPRINT_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSprintId()).isEqualTo(SPRINT_ID);
        }

        @Test
        @DisplayName("Should remove item from sprint")
        void shouldRemoveItemFromSprint() {
            testItem1.setSprintId(SPRINT_ID);
            testItem1.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem1));
            when(backlogItemRepository.save(testItem1))
                    .thenReturn(testItem1);

            BacklogItem result = sprintPlanningService.removeItemFromSprint(ITEM_ID);

            assertThat(result.getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.SELECTED);
            assertThat(result.getSprintId()).isNull();
            verify(backlogItemRepository).save(testItem1);
        }

        @Test
        @DisplayName("Should throw exception when removing non-sprint item")
        void shouldThrowExceptionForNonSprintItem() {
            testItem1.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem1));

            assertThatThrownBy(() -> sprintPlanningService.removeItemFromSprint(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Sprint Management")
    class SprintManagementTests {

        @Test
        @DisplayName("Should get sprint by ID")
        void shouldGetSprint() {
            when(sprintRepository.findById(SPRINT_ID))
                    .thenReturn(Optional.of(testSprint));

            Sprint result = sprintPlanningService.getSprint(SPRINT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SPRINT_ID);
        }

        @Test
        @DisplayName("Should throw exception when sprint not found")
        void shouldThrowExceptionForMissingSprint() {
            when(sprintRepository.findById(SPRINT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sprintPlanningService.getSprint(SPRINT_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should get active sprint for project")
        void shouldGetActiveSprint() {
            testSprint.setStatus(Sprint.SprintStatus.ACTIVE);
            when(sprintRepository.findByProjectIdAndStatus(PROJECT_ID, Sprint.SprintStatus.ACTIVE))
                    .thenReturn(Optional.of(testSprint));

            Optional<Sprint> result = sprintPlanningService.getActiveSprint(PROJECT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(Sprint.SprintStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should complete sprint")
        void shouldCompleteSprint() {
            when(sprintRepository.findById(SPRINT_ID))
                    .thenReturn(Optional.of(testSprint));
            when(sprintRepository.save(testSprint))
                    .thenReturn(testSprint);

            Sprint result = sprintPlanningService.completeSprint(SPRINT_ID);

            assertThat(result.getStatus()).isEqualTo(Sprint.SprintStatus.COMPLETED);
            verify(sprintRepository).save(testSprint);
        }
    }

    @Nested
    @DisplayName("Sprint Capacity Metrics")
    class SprintCapacityMetricsTests {

        @Test
        @DisplayName("Should calculate sprint capacity metrics")
        void shouldCalculateCapacityMetrics() {
            testItem1.setSprintId(SPRINT_ID);
            testItem1.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
            testItem2.setSprintId(SPRINT_ID);
            testItem2.setStatus(BacklogItem.BacklogItemStatus.SPRINT);

            List<BacklogItem> items = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findBySprintId(SPRINT_ID))
                    .thenReturn(items);

            SprintPlanningService.SprintCapacityMetrics metrics = sprintPlanningService.getSprintCapacity(SPRINT_ID);

            assertThat(metrics.getTotalItems()).isEqualTo(2);
            assertThat(metrics.getTotalStoryPoints()).isEqualTo(8);
            assertThat(metrics.getTotalEffortHours()).isEqualTo(15);
            assertThat(metrics.getCompletedItems()).isEqualTo(0);
            assertThat(metrics.getRemainingItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate metrics with completed items")
        void shouldCalculateMetricsWithCompletedItems() {
            testItem1.setSprintId(SPRINT_ID);
            testItem1.setStatus(BacklogItem.BacklogItemStatus.COMPLETED);
            testItem2.setSprintId(SPRINT_ID);
            testItem2.setStatus(BacklogItem.BacklogItemStatus.SPRINT);

            List<BacklogItem> items = Arrays.asList(testItem1, testItem2);
            when(backlogItemRepository.findBySprintId(SPRINT_ID))
                    .thenReturn(items);

            SprintPlanningService.SprintCapacityMetrics metrics = sprintPlanningService.getSprintCapacity(SPRINT_ID);

            assertThat(metrics.getCompletedItems()).isEqualTo(1);
            assertThat(metrics.getRemainingItems()).isEqualTo(1);
        }
    }
}
