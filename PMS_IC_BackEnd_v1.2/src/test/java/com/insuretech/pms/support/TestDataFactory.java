package com.insuretech.pms.support;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.project.entity.*;
import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.entity.RequirementCategory;
import com.insuretech.pms.rfp.entity.RequirementStatus;
import com.insuretech.pms.rfp.entity.Rfp;
import com.insuretech.pms.rfp.entity.RfpStatus;
import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.Task;
import com.insuretech.pms.task.entity.UserStory;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test Data Factory for creating mock entities with Korean data.
 * All IDs and codes are in English/numbers for technical consistency,
 * but names, descriptions, and content use Korean.
 */
@UtilityClass
public class TestDataFactory {

    // ============= User Builders =============

    public static TestUserBuilder user() {
        return new TestUserBuilder();
    }

    public User createUser(String id, String email, String name, User.UserRole role) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded_password")
                .name(name)
                .role(role)
                .active(true)
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public List<User> createTestUsers() {
        return List.of(
                createUser("user-001", "sponsor@insuretech.com", "박준영", User.UserRole.SPONSOR),
                createUser("user-002", "pmo.head@insuretech.com", "김영미", User.UserRole.PMO_HEAD),
                createUser("user-003", "pm.lee@insuretech.com", "이준호", User.UserRole.PM),
                createUser("user-004", "dev.kim@insuretech.com", "김철수", User.UserRole.DEVELOPER),
                createUser("user-005", "dev.park@insuretech.com", "박민수", User.UserRole.DEVELOPER),
                createUser("user-006", "qa.choi@insuretech.com", "최지원", User.UserRole.QA),
                createUser("user-007", "ba.jung@insuretech.com", "정수현", User.UserRole.BUSINESS_ANALYST),
                createUser("user-008", "auditor@insuretech.com", "송미영", User.UserRole.AUDITOR),
                createUser("user-009", "admin@insuretech.com", "조승호", User.UserRole.ADMIN),
                createUser("user-010", "pm.assistant@insuretech.com", "황정민", User.UserRole.PM)
        );
    }

    // ============= Project Builders =============

    public static TestProjectBuilder project() {
        return new TestProjectBuilder();
    }

    public Project createProject(String id, String name, String description,
                                Project.ProjectStatus status, LocalDate startDate, LocalDate endDate) {
        return Project.builder()
                .id(id)
                .name(name)
                .description(description)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .budget(BigDecimal.valueOf(100_000_000))
                .progress(0)
                .build();
    }

    public List<Project> createTestProjects() {
        LocalDate now = LocalDate.now();
        return List.of(
                createProject(
                        "proj-001",
                        "AI 보험심사 자동화 시스템",
                        "머신러닝을 활용한 보험심사 프로세스 자동화 및 신청자 심사 정확도 향상",
                        Project.ProjectStatus.IN_PROGRESS,
                        now.minusDays(60),
                        now.plusDays(120)
                ),
                createProject(
                        "proj-002",
                        "모바일 보험청구 플랫폼",
                        "고객 중심의 모바일 앱을 통한 보험청구 및 조회 기능 제공",
                        Project.ProjectStatus.PLANNING,
                        now.plusDays(30),
                        now.plusDays(180)
                ),
                createProject(
                        "proj-003",
                        "데이터 분석 대시보드",
                        "실시간 보험심사 데이터 분석 및 성과 지표 모니터링",
                        Project.ProjectStatus.PLANNING,
                        now.plusDays(60),
                        now.plusDays(150)
                )
        );
    }

    // ============= Phase Builders =============

    public static TestPhaseBuilder phase() {
        return new TestPhaseBuilder();
    }

    public Phase createPhase(String id, Project project, String name, Integer orderNum,
                            Phase.PhaseStatus status, LocalDate startDate, LocalDate endDate) {
        return Phase.builder()
                .id(id)
                .project(project)
                .name(name)
                .orderNum(orderNum)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .progress(0)
                .trackType(Phase.TrackType.COMMON)
                .build();
    }

    public List<Phase> createPhasesForProject(Project project) {
        LocalDate start = project.getStartDate();
        String[] phaseNames = {
                "요구사항 분석",
                "설계 및 계획",
                "개발 구현",
                "테스트 및 QA",
                "배포 준비",
                "운영 및 유지보수"
        };

        List<Phase> phases = new ArrayList<>();
        for (int i = 0; i < phaseNames.length; i++) {
            phases.add(createPhase(
                    project.getId() + "-phase-" + (i + 1),
                    project,
                    phaseNames[i],
                    i + 1,
                    i == 0 ? Phase.PhaseStatus.IN_PROGRESS : Phase.PhaseStatus.NOT_STARTED,
                    start.plusDays(i * 20),
                    start.plusDays((i + 1) * 20)
            ));
        }
        return phases;
    }

    // ============= ProjectMember Builders =============

    public static TestProjectMemberBuilder projectMember() {
        return new TestProjectMemberBuilder();
    }

    public ProjectMember createProjectMember(String id, Project project, String userId,
                                            String userName, ProjectMember.ProjectRole role) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .userId(userId)
                .userName(userName)
                .role(role)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    public List<ProjectMember> createMembersForProject(Project project) {
        return List.of(
                createProjectMember(
                        project.getId() + "-member-001",
                        project,
                        "user-001",
                        "박준영",
                        ProjectMember.ProjectRole.SPONSOR
                ),
                createProjectMember(
                        project.getId() + "-member-002",
                        project,
                        "user-003",
                        "이준호",
                        ProjectMember.ProjectRole.PM
                ),
                createProjectMember(
                        project.getId() + "-member-003",
                        project,
                        "user-004",
                        "김철수",
                        ProjectMember.ProjectRole.DEVELOPER
                ),
                createProjectMember(
                        project.getId() + "-member-004",
                        project,
                        "user-005",
                        "박민수",
                        ProjectMember.ProjectRole.DEVELOPER
                ),
                createProjectMember(
                        project.getId() + "-member-005",
                        project,
                        "user-006",
                        "최지원",
                        ProjectMember.ProjectRole.QA
                ),
                createProjectMember(
                        project.getId() + "-member-006",
                        project,
                        "user-007",
                        "정수현",
                        ProjectMember.ProjectRole.BUSINESS_ANALYST
                ),
                createProjectMember(
                        project.getId() + "-member-007",
                        project,
                        "user-002",
                        "김영미",
                        ProjectMember.ProjectRole.PMO_HEAD
                )
        );
    }

    // ============= RFP & Requirement Builders =============

    public static TestRfpBuilder rfp() {
        return new TestRfpBuilder();
    }

    public Rfp createRfp(String id, String projectId, String title, String content) {
        return Rfp.builder()
                .id(id)
                .projectId(projectId)
                .title(title)
                .content(content)
                .status(RfpStatus.DRAFT)
                .tenantId(projectId)
                .build();
    }

    public List<Rfp> createRfpsForProject(String projectId) {
        return List.of(
                createRfp(
                        UUID.randomUUID().toString(),
                        projectId,
                        "2024년 Q1 보험심사 자동화 요구사항서",
                        "AI 모델을 이용한 보험심사 자동화 시스템 요구사항 정의"
                ),
                createRfp(
                        UUID.randomUUID().toString(),
                        projectId,
                        "사용자 인터페이스 설계 가이드",
                        "모바일 및 웹 기반 보험청구 시스템의 UI/UX 설계 기준"
                )
        );
    }

    public static TestRequirementBuilder requirement() {
        return new TestRequirementBuilder();
    }

    public Requirement createRequirement(String id, String projectId, String code,
                                        String title, String description,
                                        RequirementCategory category, RequirementStatus status) {
        return Requirement.builder()
                .id(id)
                .projectId(projectId)
                .code(code)
                .title(title)
                .description(description)
                .category(category)
                .status(status)
                .progress(0)
                .tenantId(projectId)
                .build();
    }

    public List<Requirement> createRequirementsForProject(String projectId) {
        String[][] requirementData = {
                {"REQ-AI-001", "AI 모델 정확도 90% 이상 달성", "머신러닝 모델의 보험심사 정확도 90% 이상 확보", RequirementCategory.AI.name()},
                {"REQ-AI-002", "실시간 심사 결과 제공", "사용자 요청 후 5분 이내 심사 결과 제공", RequirementCategory.AI.name()},
                {"REQ-SEC-001", "고객 정보 암호화", "모든 고객 개인정보는 AES-256 암호화 적용", RequirementCategory.SECURITY.name()},
                {"REQ-FUNC-001", "모바일 청구 기능", "iOS 및 안드로이드 모바일 앱에서 청구 가능", RequirementCategory.FUNCTIONAL.name()},
                {"REQ-FUNC-002", "조회 및 승인 프로세스", "고객이 청구 상태를 실시간으로 조회 가능", RequirementCategory.FUNCTIONAL.name()},
                {"REQ-INT-001", "기존 레거시 시스템 연동", "현재 운영 중인 보험 관리 시스템과 API 연동", RequirementCategory.INTEGRATION.name()}
        };

        List<Requirement> requirements = new ArrayList<>();
        for (int i = 0; i < requirementData.length; i++) {
            requirements.add(createRequirement(
                    UUID.randomUUID().toString(),
                    projectId,
                    requirementData[i][0],
                    requirementData[i][1],
                    requirementData[i][2],
                    RequirementCategory.valueOf(requirementData[i][3]),
                    RequirementStatus.IDENTIFIED
            ));
        }
        return requirements;
    }

    // ============= Part Builders =============

    public static TestPartBuilder part() {
        return new TestPartBuilder();
    }

    public Part createPart(String id, Project project, String name, String description,
                          String leaderId, String leaderName) {
        return Part.builder()
                .id(id)
                .project(project)
                .name(name)
                .description(description)
                .leaderId(leaderId)
                .leaderName(leaderName)
                .status(Part.PartStatus.ACTIVE)
                .progress(0)
                .build();
    }

    public List<Part> createPartsForProject(Project project) {
        return List.of(
                createPart(
                        project.getId() + "-part-001",
                        project,
                        "AI 엔진 개발팀",
                        "머신러닝 모델 개발 및 최적화",
                        "user-004",
                        "김철수"
                ),
                createPart(
                        project.getId() + "-part-002",
                        project,
                        "모바일 앱 개발팀",
                        "iOS 및 안드로이드 앱 개발",
                        "user-005",
                        "박민수"
                ),
                createPart(
                        project.getId() + "-part-003",
                        project,
                        "QA 및 테스트팀",
                        "품질보증 및 시스템 테스트",
                        "user-006",
                        "최지원"
                )
        );
    }

    // ============= Kanban & Sprint Builders =============

    public static TestKanbanColumnBuilder kanbanColumn() {
        return new TestKanbanColumnBuilder();
    }

    public KanbanColumn createKanbanColumn(String id, String projectId, String name,
                                          Integer orderNum, Integer wipLimit) {
        return KanbanColumn.builder()
                .id(id)
                .projectId(projectId)
                .name(name)
                .orderNum(orderNum)
                .wipLimit(wipLimit)
                .color("#" + String.format("%06X", (int)(Math.random() * 0xFFFFFF)))
                .build();
    }

    public List<KanbanColumn> createKanbanColumnsForProject(String projectId) {
        String[] columnNames = {"할 일", "진행 중", "검토 중", "완료", "보류"};
        Integer[] wipLimits = {null, 5, 3, null, 2};

        List<KanbanColumn> columns = new ArrayList<>();
        for (int i = 0; i < columnNames.length; i++) {
            columns.add(createKanbanColumn(
                    UUID.randomUUID().toString(),
                    projectId,
                    columnNames[i],
                    i + 1,
                    wipLimits[i]
            ));
        }
        return columns;
    }

    public static TestSprintBuilder sprint() {
        return new TestSprintBuilder();
    }

    public Sprint createSprint(String id, String projectId, String name, String goal,
                              LocalDate startDate, LocalDate endDate) {
        return Sprint.builder()
                .id(id)
                .projectId(projectId)
                .name(name)
                .goal(goal)
                .startDate(startDate)
                .endDate(endDate)
                .status(Sprint.SprintStatus.PLANNED)
                .build();
    }

    public List<Sprint> createSprintsForProject(String projectId) {
        LocalDate now = LocalDate.now();
        return List.of(
                createSprint(
                        UUID.randomUUID().toString(),
                        projectId,
                        "Sprint 1",
                        "기본 AI 모델 구축 및 테스트",
                        now.plusDays(7),
                        now.plusDays(21)
                ),
                createSprint(
                        UUID.randomUUID().toString(),
                        projectId,
                        "Sprint 2",
                        "모델 정확도 개선 및 최적화",
                        now.plusDays(21),
                        now.plusDays(35)
                ),
                createSprint(
                        UUID.randomUUID().toString(),
                        projectId,
                        "Sprint 3",
                        "통합 테스트 및 배포 준비",
                        now.plusDays(35),
                        now.plusDays(49)
                )
        );
    }

    // ============= UserStory Builders =============

    public static TestUserStoryBuilder userStory() {
        return new TestUserStoryBuilder();
    }

    public UserStory createUserStory(String id, String projectId, String title,
                                    String description, UserStory.Priority priority) {
        return UserStory.builder()
                .id(id)
                .projectId(projectId)
                .title(title)
                .description(description)
                .priority(priority)
                .status(UserStory.StoryStatus.BACKLOG)
                .storyPoints(5)
                .build();
    }

    public List<UserStory> createUserStoriesForProject(String projectId) {
        String[][] storyData = {
                {"AI 모델 훈련 파이프라인 구축", "머신러닝 모델을 자동으로 훈련하는 파이프라인 시스템 개발"},
                {"보험 청구 데이터 전처리", "원본 보험 청구 데이터를 학습에 적합하게 전처리"},
                {"모바일 앱 기본 UI 개발", "기본 로그인 및 메인 화면 UI 구현"},
                {"청구 상태 조회 기능", "고객이 자신의 청구 상태를 실시간으로 조회"},
                {"보안 감사 및 취약점 분석", "OWASP Top 10 기준에 따른 보안 검증"}
        };

        List<UserStory> stories = new ArrayList<>();
        for (int i = 0; i < storyData.length; i++) {
            stories.add(createUserStory(
                    UUID.randomUUID().toString(),
                    projectId,
                    storyData[i][0],
                    storyData[i][1],
                    i % 2 == 0 ? UserStory.Priority.HIGH : UserStory.Priority.MEDIUM
            ));
        }
        return stories;
    }

    // ============= Task Builders =============

    public static TestTaskBuilder task() {
        return new TestTaskBuilder();
    }

    public Task createTask(String id, KanbanColumn column, String title,
                          String description, Task.Priority priority) {
        return Task.builder()
                .id(id)
                .column(column)
                .title(title)
                .description(description)
                .priority(priority)
                .status(Task.TaskStatus.TODO)
                .trackType(Task.TrackType.COMMON)
                .build();
    }

    public List<Task> createTasksForColumn(KanbanColumn column) {
        String[][] taskData = {
                {"데이터셋 확보 및 검증", "학습에 필요한 보험 청구 데이터 수집 및 품질 검증"},
                {"모델 파라미터 튜닝", "하이퍼파라미터 최적화를 통한 모델 성능 개선"},
                {"API 엔드포인트 개발", "REST API 기반 예측 요청 인터페이스 구현"},
                {"로깅 및 모니터링 설정", "프로덕션 환경에서의 성능 모니터링 구성"},
                {"에러 처리 및 복구 로직", "시스템 오류 발생 시 적절한 예외 처리"}
        };

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < taskData.length; i++) {
            tasks.add(createTask(
                    UUID.randomUUID().toString(),
                    column,
                    taskData[i][0],
                    taskData[i][1],
                    Task.Priority.values()[i % Task.Priority.values().length]
            ));
        }
        return tasks;
    }
}
