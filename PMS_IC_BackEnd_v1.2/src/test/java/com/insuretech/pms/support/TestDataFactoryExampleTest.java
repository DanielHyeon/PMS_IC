package com.insuretech.pms.support;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.project.entity.*;
import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.entity.RequirementCategory;
import com.insuretech.pms.rfp.entity.Rfp;
import com.insuretech.pms.rfp.entity.RfpStatus;
import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.entity.UserStory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 테스트 데이터 팩토리 사용 예제 및 검증
 * 실제 테스트에서 TestDataFactory 사용 방법을 보여줍니다.
 */
@DisplayName("TestDataFactory 예제 테스트")
class TestDataFactoryExampleTest {

    // ============= User 테스트 =============

    @Test
    @DisplayName("기본 사용자를 생성할 수 있다")
    void shouldCreateBasicUser() {
        // Given
        String userId = "user-test-001";
        String email = "test@insuretech.com";
        String name = "테스트 사용자";

        // When
        User user = TestDataFactory.user()
                .id(userId)
                .email(email)
                .name(name)
                .role(User.UserRole.DEVELOPER)
                .build();

        // Then
        assertThat(user)
                .isNotNull()
                .extracting("id", "email", "name", "role")
                .containsExactly(userId, email, name, User.UserRole.DEVELOPER);
    }

    @Test
    @DisplayName("여러 테스트 사용자를 생성할 수 있다")
    void shouldCreateMultipleTestUsers() {
        // When
        List<User> users = TestDataFactory.createTestUsers();

        // Then
        assertThat(users)
                .isNotEmpty()
                .hasSize(10)
                .extracting("role")
                .containsExactlyInAnyOrder(
                        User.UserRole.SPONSOR,
                        User.UserRole.PMO_HEAD,
                        User.UserRole.PM,
                        User.UserRole.DEVELOPER,
                        User.UserRole.DEVELOPER,
                        User.UserRole.QA,
                        User.UserRole.BUSINESS_ANALYST,
                        User.UserRole.AUDITOR,
                        User.UserRole.ADMIN,
                        User.UserRole.PM
                );
    }

    @Test
    @DisplayName("관리자 역할의 사용자를 생성할 수 있다")
    void shouldCreateAdminUser() {
        // When
        User admin = TestDataFactory.user()
                .id("admin-001")
                .asAdmin()
                .build();

        // Then
        assertThat(admin)
                .extracting("role", "name", "department")
                .containsExactly(User.UserRole.ADMIN, "관리자", "운영팀");
    }

    // ============= Project 테스트 =============

    @Test
    @DisplayName("기본 프로젝트를 생성할 수 있다")
    void shouldCreateBasicProject() {
        // When
        Project project = TestDataFactory.project()
                .id("proj-001")
                .name("테스트 프로젝트")
                .status(Project.ProjectStatus.PLANNING)
                .build();

        // Then
        assertThat(project)
                .isNotNull()
                .extracting("id", "name", "status", "progress")
                .containsExactly("proj-001", "테스트 프로젝트", Project.ProjectStatus.PLANNING, 0);
    }

    @Test
    @DisplayName("AI 프로젝트를 생성할 수 있다")
    void shouldCreateAIProject() {
        // When
        Project project = TestDataFactory.project()
                .asAIProject()
                .build();

        // Then
        assertThat(project)
                .extracting("name", "status", "progress")
                .contains("AI 보험심사 자동화 시스템", Project.ProjectStatus.IN_PROGRESS);
        assertThat(project.getProgress()).isGreaterThan(0);
    }

    @Test
    @DisplayName("여러 테스트 프로젝트를 생성할 수 있다")
    void shouldCreateMultipleTestProjects() {
        // When
        List<Project> projects = TestDataFactory.createTestProjects();

        // Then
        assertThat(projects)
                .hasSize(3)
                .extracting("name")
                .contains(
                        "AI 보험심사 자동화 시스템",
                        "모바일 보험청구 플랫폼",
                        "데이터 분석 대시보드"
                );
    }

    // ============= Phase 테스트 =============

    @Test
    @DisplayName("프로젝트의 모든 페이즈를 생성할 수 있다")
    void shouldCreatePhasesForProject() {
        // Given
        Project project = TestDataFactory.project().build();

        // When
        List<Phase> phases = TestDataFactory.createPhasesForProject(project);

        // Then
        assertThat(phases)
                .hasSize(6)
                .extracting("name")
                .contains(
                        "요구사항 분석",
                        "설계 및 계획",
                        "개발 구현",
                        "테스트 및 QA",
                        "배포 준비",
                        "운영 및 유지보수"
                );
    }

    @Test
    @DisplayName("개발 페이즈를 진행 중 상태로 생성할 수 있다")
    void shouldCreateDevelopmentPhaseInProgress() {
        // Given
        Project project = TestDataFactory.project().build();

        // When
        Phase phase = TestDataFactory.phase()
                .project(project)
                .asDevelopmentPhase()
                .inProgress()
                .build();

        // Then
        assertThat(phase)
                .extracting("name", "status", "progress")
                .containsExactly("개발 구현", Phase.PhaseStatus.IN_PROGRESS, 50);
    }

    // ============= ProjectMember 테스트 =============

    @Test
    @DisplayName("프로젝트 멤버를 생성할 수 있다")
    void shouldCreateProjectMember() {
        // Given
        Project project = TestDataFactory.project().build();

        // When
        ProjectMember member = TestDataFactory.projectMember()
                .project(project)
                .userId("user-004")
                .asProjectManager()
                .build();

        // Then
        assertThat(member)
                .extracting("role", "userName")
                .containsExactly(ProjectMember.ProjectRole.PM, "프로젝트 매니저");
        assertThat(member.getActive()).isTrue();
    }

    @Test
    @DisplayName("프로젝트의 모든 멤버를 생성할 수 있다")
    void shouldCreateMembersForProject() {
        // Given
        Project project = TestDataFactory.project().build();

        // When
        List<ProjectMember> members = TestDataFactory.createMembersForProject(project);

        // Then
        assertThat(members)
                .hasSize(7)
                .extracting("role")
                .containsExactlyInAnyOrder(
                        ProjectMember.ProjectRole.SPONSOR,
                        ProjectMember.ProjectRole.PM,
                        ProjectMember.ProjectRole.DEVELOPER,
                        ProjectMember.ProjectRole.DEVELOPER,
                        ProjectMember.ProjectRole.QA,
                        ProjectMember.ProjectRole.BUSINESS_ANALYST,
                        ProjectMember.ProjectRole.PMO_HEAD
                );
    }

    // ============= RFP & Requirement 테스트 =============

    @Test
    @DisplayName("RFP를 생성할 수 있다")
    void shouldCreateRfp() {
        // When
        Rfp rfp = TestDataFactory.rfp()
                .projectId("proj-001")
                .asAIRequirements()
                .asSubmitted()
                .build();

        // Then
        assertThat(rfp)
                .extracting("title", "status")
                .contains("AI 보험심사 요구사항서", RfpStatus.SUBMITTED);
        assertThat(rfp.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("프로젝트의 모든 RFP를 생성할 수 있다")
    void shouldCreateRfpsForProject() {
        // When
        List<Rfp> rfps = TestDataFactory.createRfpsForProject("proj-001");

        // Then
        assertThat(rfps)
                .hasSize(2)
                .extracting("title")
                .contains(
                        "2024년 Q1 보험심사 자동화 요구사항서",
                        "사용자 인터페이스 설계 가이드"
                );
    }

    @Test
    @DisplayName("요구사항을 생성할 수 있다")
    void shouldCreateRequirement() {
        // When
        Requirement req = TestDataFactory.requirement()
                .projectId("proj-001")
                .asAIRequirement()
                .highPriority()
                .build();

        // Then
        assertThat(req)
                .extracting("category", "priority")
                .contains(RequirementCategory.AI, com.insuretech.pms.rfp.entity.Priority.HIGH);
        assertThat(req.getTitle()).contains("AI");
    }

    @Test
    @DisplayName("프로젝트의 모든 요구사항을 생성할 수 있다")
    void shouldCreateRequirementsForProject() {
        // When
        List<Requirement> requirements = TestDataFactory.createRequirementsForProject("proj-001");

        // Then
        assertThat(requirements)
                .hasSize(6)
                .extracting("category")
                .contains(
                        RequirementCategory.AI,
                        RequirementCategory.AI,
                        RequirementCategory.SECURITY,
                        RequirementCategory.FUNCTIONAL,
                        RequirementCategory.FUNCTIONAL,
                        RequirementCategory.INTEGRATION
                );
    }

    // ============= Part 테스트 =============

    @Test
    @DisplayName("팀(Part)을 생성할 수 있다")
    void shouldCreatePart() {
        // Given
        Project project = TestDataFactory.project().build();

        // When
        Part part = TestDataFactory.part()
                .project(project)
                .asAIEnginePart()
                .build();

        // Then
        assertThat(part)
                .extracting("name", "status")
                .containsExactly("AI 엔진 개발팀", Part.PartStatus.ACTIVE);
        assertThat(part.getLeaderName()).contains("리더");
    }

    @Test
    @DisplayName("프로젝트의 모든 팀을 생성할 수 있다")
    void shouldCreatePartsForProject() {
        // Given
        Project project = TestDataFactory.project().build();

        // When
        List<Part> parts = TestDataFactory.createPartsForProject(project);

        // Then
        assertThat(parts)
                .hasSize(3)
                .extracting("name")
                .contains(
                        "AI 엔진 개발팀",
                        "모바일 앱 개발팀",
                        "QA 및 테스트팀"
                );
    }

    // ============= Kanban Column 테스트 =============

    @Test
    @DisplayName("Kanban 컬럼을 생성할 수 있다")
    void shouldCreateKanbanColumn() {
        // When
        KanbanColumn column = TestDataFactory.kanbanColumn()
                .projectId("proj-001")
                .asTodoColumn()
                .build();

        // Then
        assertThat(column)
                .extracting("name", "orderNum")
                .contains("할 일", 1);
    }

    @Test
    @DisplayName("프로젝트의 모든 Kanban 컬럼을 생성할 수 있다")
    void shouldCreateKanbanColumnsForProject() {
        // When
        List<KanbanColumn> columns = TestDataFactory.createKanbanColumnsForProject("proj-001");

        // Then
        assertThat(columns)
                .hasSize(5)
                .extracting("name")
                .contains("할 일", "진행 중", "검토 중", "완료", "보류");
    }

    // ============= Sprint 테스트 =============

    @Test
    @DisplayName("Sprint를 생성할 수 있다")
    void shouldCreateSprint() {
        // When
        Sprint sprint = TestDataFactory.sprint()
                .projectId("proj-001")
                .asSprint1()
                .active()
                .build();

        // Then
        assertThat(sprint)
                .extracting("name", "status")
                .containsExactly("Sprint 1", Sprint.SprintStatus.ACTIVE);
        assertThat(sprint.getGoal()).contains("기본 기능");
    }

    @Test
    @DisplayName("프로젝트의 모든 Sprint를 생성할 수 있다")
    void shouldCreateSprintsForProject() {
        // When
        List<Sprint> sprints = TestDataFactory.createSprintsForProject("proj-001");

        // Then
        assertThat(sprints)
                .hasSize(3)
                .extracting("name")
                .contains("Sprint 1", "Sprint 2", "Sprint 3");
    }

    // ============= UserStory 테스트 =============

    @Test
    @DisplayName("UserStory를 생성할 수 있다")
    void shouldCreateUserStory() {
        // When
        UserStory story = TestDataFactory.userStory()
                .projectId("proj-001")
                .asAIModelTraining()
                .highPriority()
                .build();

        // Then
        assertThat(story)
                .extracting("priority", "storyPoints")
                .containsExactly(UserStory.Priority.HIGH, 8);
        assertThat(story.getTitle()).contains("AI");
    }

    @Test
    @DisplayName("프로젝트의 모든 UserStory를 생성할 수 있다")
    void shouldCreateUserStoriesForProject() {
        // When
        List<UserStory> stories = TestDataFactory.createUserStoriesForProject("proj-001");

        // Then
        assertThat(stories)
                .hasSize(5)
                .allMatch(story -> story.getTitle() != null && !story.getTitle().isEmpty());
    }

    // ============= Task 테스트 =============

    @Test
    @DisplayName("Task를 생성할 수 있다")
    void shouldCreateTask() {
        // Given
        KanbanColumn column = TestDataFactory.kanbanColumn().build();

        // When
        Task task = TestDataFactory.task()
                .column(column)
                .asDatasetPreparation()
                .inProgress()
                .build();

        // Then
        assertThat(task)
                .extracting("status", "trackType")
                .containsExactly(Task.TaskStatus.IN_PROGRESS, Task.TrackType.AI);
        assertThat(task.getAssigneeId()).isEqualTo("user-004");
    }

    @Test
    @DisplayName("컬럼의 모든 Task를 생성할 수 있다")
    void shouldCreateTasksForColumn() {
        // Given
        KanbanColumn column = TestDataFactory.kanbanColumn().build();

        // When
        List<Task> tasks = TestDataFactory.createTasksForColumn(column);

        // Then
        assertThat(tasks)
                .hasSize(5)
                .allMatch(task -> task.getTitle() != null && !task.getTitle().isEmpty());
    }

    // ============= 통합 시나리오 테스트 =============

    @Test
    @DisplayName("전체 프로젝트 구조를 생성할 수 있다")
    void shouldCreateCompleteProjectStructure() {
        // When - 프로젝트 및 관련 엔티티 생성
        Project project = TestDataFactory.project()
                .asAIProject()
                .build();

        List<Phase> phases = TestDataFactory.createPhasesForProject(project);
        List<ProjectMember> members = TestDataFactory.createMembersForProject(project);
        List<Requirement> requirements = TestDataFactory.createRequirementsForProject(project.getId());
        List<Part> parts = TestDataFactory.createPartsForProject(project);
        List<Sprint> sprints = TestDataFactory.createSprintsForProject(project.getId());
        List<KanbanColumn> columns = TestDataFactory.createKanbanColumnsForProject(project.getId());

        // Then - 모든 데이터 검증
        assertThat(project).isNotNull();
        assertThat(phases).hasSize(6);
        assertThat(members).hasSize(7);
        assertThat(requirements).hasSize(6);
        assertThat(parts).hasSize(3);
        assertThat(sprints).hasSize(3);
        assertThat(columns).hasSize(5);
    }

    @Test
    @DisplayName("메서드 체이닝으로 복잡한 엔티티를 생성할 수 있다")
    void shouldCreateComplexEntityWithMethodChaining() {
        // When
        Task task = TestDataFactory.task()
                .column(TestDataFactory.kanbanColumn()
                        .projectId("proj-001")
                        .asInProgressColumn()
                        .build())
                .asModelTuning()
                .inProgress()
                .assignedTo("user-004")
                .dueIn(5)
                .trackTypeAI()
                .criticalPriority()
                .build();

        // Then
        assertThat(task)
                .extracting("priority", "status", "trackType")
                .containsExactly(
                        Task.Priority.CRITICAL,
                        Task.TaskStatus.IN_PROGRESS,
                        Task.TrackType.AI
                );
        assertThat(task.getAssigneeId()).isEqualTo("user-004");
        assertThat(task.getDueDate()).isNotNull();
    }
}
