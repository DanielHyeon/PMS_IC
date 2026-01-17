package com.insuretech.pms.project.service;

import com.insuretech.pms.project.entity.Backlog;
import com.insuretech.pms.project.entity.BacklogItem;
import com.insuretech.pms.project.repository.BacklogItemRepository;
import com.insuretech.pms.project.repository.BacklogRepository;
import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.repository.RequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BacklogItemService Tests")
class BacklogItemServiceTest {

    @Mock
    private BacklogItemRepository backlogItemRepository;

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private RequirementRepository requirementRepository;

    @InjectMocks
    private BacklogItemService backlogItemService;

    private static final String BACKLOG_ID = "backlog-001";
    private static final String ITEM_ID = "item-001";
    private static final String REQUIREMENT_ID = "req-001";

    private Backlog testBacklog;
    private BacklogItem testItem;
    private Requirement testRequirement;

    @BeforeEach
    void setUp() {
        testBacklog = new Backlog();
        testBacklog.setId(BACKLOG_ID);
        testBacklog.setProjectId("proj-001");
        testBacklog.setStatus(Backlog.BacklogStatus.ACTIVE);

        testItem = new BacklogItem();
        testItem.setId(ITEM_ID);
        testItem.setBacklog(testBacklog);
        testItem.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
        testItem.setPriorityOrder(0);
        testItem.setOriginType(BacklogItem.BacklogItemOrigin.MANUAL);

        testRequirement = new Requirement();
        testRequirement.setId(REQUIREMENT_ID);
        testRequirement.setStoryPoints(8);
    }

    @Nested
    @DisplayName("Create Backlog Item")
    class CreateBacklogItemTests {

        @Test
        @DisplayName("Should create manual backlog item")
        void shouldCreateManualBacklogItem() {
            when(backlogRepository.findById(BACKLOG_ID))
                    .thenReturn(Optional.of(testBacklog));
            when(backlogItemRepository.findMaxPriorityOrderByBacklogId(BACKLOG_ID))
                    .thenReturn(0);
            when(backlogItemRepository.save(any(BacklogItem.class)))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.createManualBacklogItem(BACKLOG_ID, "Test Item", 5);

            assertThat(result).isNotNull();
            assertThat(result.getOriginType()).isEqualTo(BacklogItem.BacklogItemOrigin.MANUAL);
            assertThat(result.getStoryPoints()).isEqualTo(5);
            verify(backlogItemRepository).save(any(BacklogItem.class));
        }

        @Test
        @DisplayName("Should throw exception for non-existent backlog")
        void shouldThrowExceptionForNonExistentBacklog() {
            when(backlogRepository.findById(BACKLOG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> backlogItemService.createManualBacklogItem(BACKLOG_ID, "Test", 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should select item for sprint planning if story points exist")
        void shouldSelectItemForSprintPlanning() {
            testItem.setOriginType(BacklogItem.BacklogItemOrigin.MANUAL);
            testItem.setStoryPoints(5);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));
            when(backlogItemRepository.save(testItem))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.selectForSprintPlanning(ITEM_ID);

            assertThat(result.getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.SELECTED);
            verify(backlogItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should throw exception when requirement item has no story points")
        void shouldThrowExceptionWhenRequirementNoStoryPoints() {
            testItem.setOriginType(BacklogItem.BacklogItemOrigin.REQUIREMENT);
            testItem.setStoryPoints(null);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            assertThatThrownBy(() -> backlogItemService.selectForSprintPlanning(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("story points");
        }

        @Test
        @DisplayName("Should move selected item to sprint")
        void shouldMoveToSprint() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));
            when(backlogItemRepository.save(testItem))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.moveToSprint(ITEM_ID);

            assertThat(result.getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.SPRINT);
            verify(backlogItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should throw exception when moving non-selected item to sprint")
        void shouldThrowExceptionForInvalidStateTransition() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            assertThatThrownBy(() -> backlogItemService.moveToSprint(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should complete backlog item in sprint")
        void shouldCompleteBacklogItem() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));
            when(backlogItemRepository.save(testItem))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.completeBacklogItem(ITEM_ID);

            assertThat(result.getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.COMPLETED);
            verify(backlogItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should move selected item back to backlog")
        void shouldMoveBackToBacklog() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.SELECTED);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));
            when(backlogItemRepository.save(testItem))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.moveBackToBacklog(ITEM_ID);

            assertThat(result.getStatus()).isEqualTo(BacklogItem.BacklogItemStatus.BACKLOG);
            verify(backlogItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should throw exception when moving completed item back to backlog")
        void shouldThrowExceptionForCompletedItemDeselect() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.COMPLETED);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            assertThatThrownBy(() -> backlogItemService.moveBackToBacklog(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Story Point Operations")
    class StoryPointOperationsTests {

        @Test
        @DisplayName("Should update story points")
        void shouldUpdateStoryPoints() {
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));
            when(backlogItemRepository.save(testItem))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.updateStoryPoints(ITEM_ID, 8);

            assertThat(result.getStoryPoints()).isEqualTo(8);
            verify(backlogItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should sync story points from requirement")
        void shouldSyncStoryPointsFromRequirement() {
            testItem.setRequirement(testRequirement);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));
            when(backlogItemRepository.save(testItem))
                    .thenReturn(testItem);

            BacklogItem result = backlogItemService.syncStoryPointsFromRequirement(ITEM_ID);

            assertThat(result.getStoryPoints()).isEqualTo(testRequirement.getStoryPoints());
            verify(backlogItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should throw exception when syncing story points for non-linked item")
        void shouldThrowExceptionWhenNoRequirement() {
            testItem.setRequirement(null);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            assertThatThrownBy(() -> backlogItemService.syncStoryPointsFromRequirement(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperationsTests {

        @Test
        @DisplayName("Should delete backlog item in BACKLOG status")
        void shouldDeleteBacklogItem() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.BACKLOG);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            backlogItemService.deleteBacklogItem(ITEM_ID);

            verify(backlogItemRepository).deleteById(ITEM_ID);
        }

        @Test
        @DisplayName("Should throw exception when deleting item in SPRINT status")
        void shouldThrowExceptionForSprintItemDeletion() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.SPRINT);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            assertThatThrownBy(() -> backlogItemService.deleteBacklogItem(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should throw exception when deleting completed item")
        void shouldThrowExceptionForCompletedItemDeletion() {
            testItem.setStatus(BacklogItem.BacklogItemStatus.COMPLETED);
            when(backlogItemRepository.findById(ITEM_ID))
                    .thenReturn(Optional.of(testItem));

            assertThatThrownBy(() -> backlogItemService.deleteBacklogItem(ITEM_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
