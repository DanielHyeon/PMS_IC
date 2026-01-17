# TestDataFactory - 테스트 데이터 생성 도구

## 개요

TestDataFactory는 PMS 프로젝트의 테스트 데이터를 쉽게 생성할 수 있도록 설계된 데이터 팩토리입니다.
모든 한글 데이터(이름, 설명, 내용)와 영어/숫자 ID(코드, 이메일)를 지원합니다.

## 주요 특징

- **Fluent API**: 메서드 체이닝으로 직관적인 데이터 생성
- **한글 데이터**: 모든 비즈니스 데이터는 한글로 제공
- **기본값 설정**: 가장 일반적인 시나리오에 맞는 기본값 제공
- **Helper Methods**: `asXxx()` 메서드로 특정 시나리오 데이터 생성 용이

## 사용 방법

### 1. 기본 사용법

#### 사용자 생성

```java
// 기본 사용자 생성
User user = TestDataFactory.user()
    .id("user-001")
    .email("kim@insuretech.com")
    .name("김철수")
    .role(User.UserRole.DEVELOPER)
    .build();

// 특정 역할의 사용자 생성
User admin = TestDataFactory.user()
    .id("user-002")
    .email("admin@insuretech.com")
    .asAdmin()
    .build();

User pm = TestDataFactory.user()
    .id("user-003")
    .asProjectManager()
    .build();

// 여러 사용자 한번에 생성
List<User> users = TestDataFactory.createTestUsers();
```

#### 프로젝트 생성

```java
// 기본 프로젝트 생성
Project project = TestDataFactory.project()
    .id("proj-001")
    .name("AI 보험심사 시스템")
    .description("머신러닝 기반 자동화 시스템")
    .status(Project.ProjectStatus.IN_PROGRESS)
    .build();

// 사전 정의된 프로젝트 생성
Project aiProject = TestDataFactory.project()
    .asAIProject()
    .build();

Project mobileProject = TestDataFactory.project()
    .asMobileProject()
    .build();

// 여러 프로젝트 한번에 생성
List<Project> projects = TestDataFactory.createTestProjects();
```

#### 페이즈 생성

```java
// 기본 페이즈 생성
Phase phase = TestDataFactory.phase()
    .id("phase-001")
    .project(project)
    .name("요구사항 분석")
    .orderNum(1)
    .build();

// 사전 정의된 페이즈 생성
Phase designPhase = TestDataFactory.phase()
    .project(project)
    .asDesignPhase()
    .build();

Phase devPhase = TestDataFactory.phase()
    .project(project)
    .asDevelopmentPhase()
    .inProgress()
    .build();

// 프로젝트의 모든 페이즈 생성
List<Phase> phases = TestDataFactory.createPhasesForProject(project);
```

#### 프로젝트 멤버 생성

```java
// 기본 멤버 생성
ProjectMember member = TestDataFactory.projectMember()
    .id("pm-001")
    .project(project)
    .userId("user-001")
    .userName("박준영")
    .role(ProjectMember.ProjectRole.SPONSOR)
    .build();

// 특정 역할의 멤버 생성
ProjectMember pmMember = TestDataFactory.projectMember()
    .project(project)
    .userId("user-003")
    .asProjectManager()
    .build();

ProjectMember devMember = TestDataFactory.projectMember()
    .project(project)
    .userId("user-004")
    .asDeveloper()
    .build();

// 프로젝트의 모든 멤버 생성
List<ProjectMember> members = TestDataFactory.createMembersForProject(project);
```

#### RFP 생성

```java
// 기본 RFP 생성
Rfp rfp = TestDataFactory.rfp()
    .projectId(project.getId())
    .title("AI 요구사항서")
    .content("상세 요구사항...")
    .build();

// 사전 정의된 RFP 생성
Rfp aiRfp = TestDataFactory.rfp()
    .projectId(project.getId())
    .asAIRequirements()
    .asSubmitted()
    .build();

Rfp securityRfp = TestDataFactory.rfp()
    .projectId(project.getId())
    .asSecurityRequirements()
    .asUnderReview()
    .withFile("security-requirements.pdf", "application/pdf", 1024000L)
    .build();

// 프로젝트의 모든 RFP 생성
List<Rfp> rfps = TestDataFactory.createRfpsForProject(project.getId());
```

#### Requirement 생성

```java
// 기본 요구사항 생성
Requirement req = TestDataFactory.requirement()
    .projectId(project.getId())
    .code("REQ-AI-001")
    .title("AI 모델 정확도 90% 달성")
    .category(RequirementCategory.AI)
    .build();

// 사전 정의된 요구사항 생성
Requirement aiReq = TestDataFactory.requirement()
    .projectId(project.getId())
    .asAIRequirement()
    .highPriority()
    .approved()
    .withEstimatedEffort(40)
    .build();

Requirement secReq = TestDataFactory.requirement()
    .projectId(project.getId())
    .asSecurityRequirement()
    .criticalPriority()
    .build();

// 프로젝트의 모든 요구사항 생성
List<Requirement> requirements = TestDataFactory.createRequirementsForProject(project.getId());
```

#### Part(팀) 생성

```java
// 기본 파트 생성
Part part = TestDataFactory.part()
    .id("part-001")
    .project(project)
    .name("AI 엔진 개발팀")
    .description("머신러닝 모델 개발")
    .leaderId("user-004")
    .leaderName("김철수")
    .build();

// 사전 정의된 팀 생성
Part aiTeam = TestDataFactory.part()
    .project(project)
    .asAIEnginePart()
    .build();

Part mobileTeam = TestDataFactory.part()
    .project(project)
    .asMobileAppPart()
    .build();

Part qaTeam = TestDataFactory.part()
    .project(project)
    .asQAPart()
    .build();

// 프로젝트의 모든 팀 생성
List<Part> parts = TestDataFactory.createPartsForProject(project);
```

#### Kanban Column 생성

```java
// 기본 컬럼 생성
KanbanColumn column = TestDataFactory.kanbanColumn()
    .projectId(project.getId())
    .name("할 일")
    .orderNum(1)
    .build();

// 사전 정의된 컬럼 생성
KanbanColumn todoCol = TestDataFactory.kanbanColumn()
    .projectId(project.getId())
    .asTodoColumn()
    .build();

KanbanColumn inProgressCol = TestDataFactory.kanbanColumn()
    .projectId(project.getId())
    .asInProgressColumn()
    .build();

KanbanColumn completedCol = TestDataFactory.kanbanColumn()
    .projectId(project.getId())
    .asCompletedColumn()
    .build();

// 프로젝트의 모든 컬럼 생성
List<KanbanColumn> columns = TestDataFactory.createKanbanColumnsForProject(project.getId());
```

#### Sprint 생성

```java
// 기본 스프린트 생성
Sprint sprint = TestDataFactory.sprint()
    .projectId(project.getId())
    .name("Sprint 1")
    .goal("기본 기능 구현")
    .build();

// 사전 정의된 스프린트 생성
Sprint aiSprint = TestDataFactory.sprint()
    .projectId(project.getId())
    .asAIModelSprint()
    .active()
    .build();

Sprint mobileSprint = TestDataFactory.sprint()
    .projectId(project.getId())
    .asMobileAppSprint()
    .planned()
    .build();

// 프로젝트의 모든 스프린트 생성
List<Sprint> sprints = TestDataFactory.createSprintsForProject(project.getId());
```

#### UserStory 생성

```java
// 기본 사용자 스토리 생성
UserStory story = TestDataFactory.userStory()
    .projectId(project.getId())
    .title("AI 모델 훈련")
    .description("머신러닝 모델 훈련 구현")
    .build();

// 사전 정의된 스토리 생성
UserStory aiStory = TestDataFactory.userStory()
    .projectId(project.getId())
    .asAIModelTraining()
    .inProgress()
    .assigneeId("user-004")
    .build();

UserStory dataStory = TestDataFactory.userStory()
    .projectId(project.getId())
    .asDataPreprocessing()
    .highPriority()
    .withStoryPoints(5)
    .build();

// 프로젝트의 모든 스토리 생성
List<UserStory> stories = TestDataFactory.createUserStoriesForProject(project.getId());
```

#### Task 생성

```java
// 기본 작업 생성
Task task = TestDataFactory.task()
    .column(kanbanColumn)
    .title("데이터 준비")
    .description("학습 데이터 수집")
    .priority(Task.Priority.HIGH)
    .build();

// 사전 정의된 작업 생성
Task datasetTask = TestDataFactory.task()
    .column(kanbanColumn)
    .asDatasetPreparation()
    .todo()
    .dueIn(7)
    .build();

Task modelTask = TestDataFactory.task()
    .column(kanbanColumn)
    .asModelTuning()
    .inProgress()
    .assignedTo("user-004")
    .build();

// 컬럼의 모든 작업 생성
List<Task> tasks = TestDataFactory.createTasksForColumn(kanbanColumn);
```

## 테스트에서의 사용 예제

### 통합 테스트

```java
@SpringBootTest
@ActiveProfiles("test")
class ProjectIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Test
    void testProjectWithMembers() {
        // 1. 테스트 데이터 생성
        Project project = TestDataFactory.project()
            .asAIProject()
            .build();
        projectRepository.save(project);

        // 2. 멤버 생성
        List<ProjectMember> members = TestDataFactory.createMembersForProject(project);
        memberRepository.saveAll(members);

        // 3. 검증
        assertThat(memberRepository.findAll()).hasSize(7);
    }
}
```

### 단위 테스트

```java
class ProjectServiceTest {

    private ProjectService projectService;
    private ProjectRepository projectRepository;

    @BeforeEach
    void setup() {
        projectRepository = mock(ProjectRepository.class);
        projectService = new ProjectService(projectRepository);
    }

    @Test
    void testCreateProject() {
        // 1. 테스트 데이터 생성
        Project project = TestDataFactory.project()
            .asMobileProject()
            .build();

        when(projectRepository.save(any(Project.class)))
            .thenReturn(project);

        // 2. 메서드 실행
        Project result = projectService.createProject(project);

        // 3. 검증
        assertThat(result).isNotNull();
        assertThat(result.getName()).contains("모바일");
    }
}
```

## 사용 가능한 Helper Methods

### User Builders
- `asAdmin()` - 관리자
- `asProjectManager()` - 프로젝트 매니저
- `asDeveloper()` - 개발자
- `asQA()` - QA 담당자
- `asBusinessAnalyst()` - 비즈니스 분석가

### Project Builders
- `asAIProject()` - AI 보험심사 프로젝트
- `asMobileProject()` - 모바일 앱 프로젝트
- `asDataAnalyticsProject()` - 데이터 분석 대시보드
- `inProgress()` - 진행 중 상태
- `planning()` - 계획 상태
- `completed()` - 완료 상태

### Phase Builders
- `asRequirementAnalysisPhase()` - 요구사항 분석 페이즈
- `asDesignPhase()` - 설계 페이즈
- `asDevelopmentPhase()` - 개발 페이즈
- `asTestingPhase()` - 테스트 페이즈
- `asDeploymentPhase()` - 배포 페이즈
- `asMaintenancePhase()` - 운영 페이즈

### Requirement Builders
- `asAIRequirement()` - AI 관련 요구사항
- `asSecurityRequirement()` - 보안 요구사항
- `asFunctionalRequirement()` - 기능 요구사항
- `asIntegrationRequirement()` - 통합 요구사항
- `verified()` - 검증 완료
- `approved()` - 승인 완료

### Task Builders
- `asDatasetPreparation()` - 데이터셋 준비
- `asModelTuning()` - 모델 튜닝
- `asAPIEndpointDevelopment()` - API 개발
- `asSecurityTest()` - 보안 테스트
- `inProgress()` - 진행 중
- `done()` - 완료

## 벌크 데이터 생성

전체 테스트 시나리오를 위해 한 번에 여러 엔티티를 생성할 수 있습니다:

```java
@Test
void testCompleteProjectScenario() {
    // 1. 프로젝트 생성
    Project project = TestDataFactory.project()
        .asAIProject()
        .build();
    projectRepository.save(project);

    // 2. 사용자 및 멤버 생성
    List<User> users = TestDataFactory.createTestUsers();
    userRepository.saveAll(users);

    List<ProjectMember> members = TestDataFactory.createMembersForProject(project);
    memberRepository.saveAll(members);

    // 3. 페이즈 생성
    List<Phase> phases = TestDataFactory.createPhasesForProject(project);
    phaseRepository.saveAll(phases);

    // 4. 요구사항 생성
    List<Requirement> requirements = TestDataFactory.createRequirementsForProject(project.getId());
    requirementRepository.saveAll(requirements);

    // 5. 팀 생성
    List<Part> parts = TestDataFactory.createPartsForProject(project);
    partRepository.saveAll(parts);

    // 6. 스프린트 및 스토리 생성
    List<Sprint> sprints = TestDataFactory.createSprintsForProject(project.getId());
    sprintRepository.saveAll(sprints);

    // 7. Kanban 및 작업 생성
    List<KanbanColumn> columns = TestDataFactory.createKanbanColumnsForProject(project.getId());
    kanbanRepository.saveAll(columns);

    // 검증
    assertThat(memberRepository.findAll()).hasSize(7);
    assertThat(phaseRepository.findAll()).hasSize(6);
    assertThat(requirementRepository.findAll()).hasSize(6);
}
```

## 주의사항

1. **고유 제약 조건**: 코드와 이메일은 고유해야 합니다. 테스트 간 중복을 피하기 위해 `@BeforeEach`에서 데이터를 초기화하세요.

2. **데이터베이스 초기화**: 통합 테스트 시 매 테스트마다 데이터베이스를 초기화하려면:
   ```java
   @ActiveProfiles("test")
   @SpringBootTest
   class TestClass {
       @DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
       void test() { }
   }
   ```

3. **ID 생성**: 기본적으로 UUID 또는 타임스탬프 기반 ID가 자동 생성됩니다. 필요시 직접 지정하세요.

4. **관계 설정**: `@ManyToOne` 관계는 빌더에서 설정해야 합니다(예: `.project(project)`)

## 문제 해결

### "고유 제약 조건 위반" 오류
```java
// 올바른 방법: 각 테스트마다 고유한 ID 생성
User user = TestDataFactory.user()
    .id("user-" + UUID.randomUUID())
    .email("user-" + System.currentTimeMillis() + "@test.com")
    .build();
```

### 외래 키 제약 조건 오류
```java
// 올바른 방법: 먼저 부모 엔티티를 저장
Project project = TestDataFactory.project().build();
projectRepository.save(project);

Phase phase = TestDataFactory.phase()
    .project(project)  // 저장된 프로젝트 참조
    .build();
phaseRepository.save(phase);
```

## 라이선스

이 테스트 데이터 팩토리는 PMS 프로젝트 내부용입니다.
