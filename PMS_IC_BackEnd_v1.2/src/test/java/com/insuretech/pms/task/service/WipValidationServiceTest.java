package com.insuretech.pms.task.service;

import com.insuretech.pms.task.dto.WipValidationResult;
import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.repository.KanbanColumnRepository;
import com.insuretech.pms.task.repository.SprintRepository;
import com.insuretech.pms.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * E2E Integration Tests for WIP Validation Service
 * Tests the complete workflow of WIP limit validation across columns and sprints
 */
@SpringBootTest
@Transactional
@DisplayName("WIP Validation Service Integration Tests")
class WipValidationServiceTest {

    @Autowired
    private WipValidationService wipValidationService;

    @Autowired
    private KanbanColumnRepository kanbanColumnRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SprintRepository sprintRepository;

    private String projectId;
    private String sprintId;
    private KanbanColumn column;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID().toString();
        sprintId = UUID.randomUUID().toString();

        // Create sprint with CONWIP limit
        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .projectId(projectId)
                .name("Test Sprint")
                .status(Sprint.SprintStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .enableWipValidation(true)
                .conwipLimit(25)
                .build();

        sprintRepository.save(sprint);

        // Create kanban column with WIP limits
        column = KanbanColumn.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name("In Progress")
                .orderNum(1)
                .wipLimitSoft(8)
                .wipLimitHard(12)
                .isBottleneckColumn(false)
                .build();

        kanbanColumnRepository.save(column);
    }

    @Test
    @DisplayName("Validate column WIP within soft limit")
    void testColumnWipWithinSoftLimit() {
        // Create 5 tasks (under soft limit of 8)
        for (int i = 0; i < 5; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .status(Task.TaskStatus.TODO)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Validate
        WipValidationResult result = wipValidationService.validateColumnWipLimit(column.getId(), false);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Detect column WIP exceeding soft limit")
    void testColumnWipExceedsSoftLimit() {
        // Create 10 tasks (exceeds soft limit of 8 but under hard limit of 12)
        for (int i = 0; i < 10; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .status(Task.TaskStatus.IN_PROGRESS)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Validate without allowing soft limit exceeding
        WipValidationResult result = wipValidationService.validateColumnWipLimit(column.getId(), false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolationType()).isEqualTo(WipValidationResult.WipViolationType.COLUMN_SOFT_LIMIT);
    }

    @Test
    @DisplayName("Enforce hard WIP limit on column")
    void testColumnWipExceedsHardLimit() {
        // Create 15 tasks (exceeds hard limit of 12)
        for (int i = 0; i < 15; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .status(Task.TaskStatus.IN_PROGRESS)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Validate (hard limit should be violated)
        WipValidationResult result = wipValidationService.validateColumnWipLimit(column.getId(), true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolationType()).isEqualTo(WipValidationResult.WipViolationType.COLUMN_HARD_LIMIT);
    }

    @Test
    @DisplayName("Validate sprint CONWIP within limit")
    void testSprintConwipWithinLimit() {
        // Create 20 tasks in sprint (under CONWIP limit of 25)
        for (int i = 0; i < 20; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .sprintId(sprintId)
                    .status(Task.TaskStatus.IN_PROGRESS)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Validate
        WipValidationResult result = wipValidationService.validateSprintConwip(sprintId);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Detect sprint CONWIP exceeding limit")
    void testSprintConwipExceedsLimit() {
        // Create 30 tasks in sprint (exceeds CONWIP limit of 25)
        for (int i = 0; i < 30; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .sprintId(sprintId)
                    .status(Task.TaskStatus.IN_PROGRESS)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Validate
        WipValidationResult result = wipValidationService.validateSprintConwip(sprintId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolationType()).isEqualTo(WipValidationResult.WipViolationType.SPRINT_CONWIP_LIMIT);
    }

    @Test
    @DisplayName("Validate personal WIP limit per assignee")
    void testPersonalWipLimitValidation() {
        String assigneeId = "user-1";

        // Create 6 tasks assigned to user (exceeds default limit of 5)
        for (int i = 0; i < 6; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .assigneeId(assigneeId)
                    .status(Task.TaskStatus.IN_PROGRESS)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Validate with max personal WIP of 5
        WipValidationResult result = wipValidationService.validatePersonalWipLimit(assigneeId, 5);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolationType()).isEqualTo(WipValidationResult.WipViolationType.PERSONAL_WIP_LIMIT);
    }

    @Test
    @DisplayName("Get project-wide WIP status")
    void testGetProjectWipStatus() {
        // Add tasks to different columns
        for (int i = 0; i < 5; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(column)
                    .title("Task " + i)
                    .status(Task.TaskStatus.IN_PROGRESS)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Get project status
        var wipStatus = wipValidationService.getProjectWipStatus(projectId);

        assertThat(wipStatus).containsKeys("projectId", "totalWip", "columnStatuses", "bottleneckCount");
        assertThat(wipStatus.get("projectId")).isEqualTo(projectId);
        assertThat((Integer) wipStatus.get("totalWip")).isGreaterThan(0);
    }

    @Test
    @DisplayName("Detect bottleneck columns")
    void testBottleneckDetection() {
        // Create a bottleneck column
        KanbanColumn bottleneckColumn = KanbanColumn.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name("Review")
                .orderNum(2)
                .wipLimitSoft(5)
                .wipLimitHard(8)
                .isBottleneckColumn(true)
                .build();

        kanbanColumnRepository.save(bottleneckColumn);

        // Add tasks to bottleneck column
        for (int i = 0; i < 7; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .column(bottleneckColumn)
                    .title("Review Task " + i)
                    .status(Task.TaskStatus.REVIEW)
                    .priority(Task.Priority.MEDIUM)
                    .build();
            taskRepository.save(task);
        }

        // Get project status
        var wipStatus = wipValidationService.getProjectWipStatus(projectId);

        assertThat(wipStatus.get("bottleneckCount")).isGreaterThan(0);
    }
}
