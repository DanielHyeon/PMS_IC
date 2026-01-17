# TestDataFactory 빠른 시작 가이드

## 파일 위치
```
src/test/java/com/insuretech/pms/support/
├── TestDataFactory.java               # 메인 팩토리 (정적 메서드)
├── TestUserBuilder.java               # User 빌더
├── TestProjectBuilder.java            # Project 빌더
├── TestPhaseBuilder.java              # Phase 빌더
├── TestProjectMemberBuilder.java      # ProjectMember 빌더
├── TestRfpBuilder.java                # RFP 빌더
├── TestRequirementBuilder.java        # Requirement 빌더
├── TestPartBuilder.java               # Part 빌더
├── TestKanbanColumnBuilder.java       # KanbanColumn 빌더
├── TestSprintBuilder.java             # Sprint 빌더
├── TestUserStoryBuilder.java          # UserStory 빌더
├── TestTaskBuilder.java               # Task 빌더
├── TestDataFactoryExampleTest.java    # 사용 예제 테스트
├── README.md                          # 상세 문서
└── QUICK_START.md                     # 이 파일
```

## 5분 안에 시작하기

### 1. 단일 엔티티 생성

```java
// User 생성
User user = TestDataFactory.user()
    .id("user-001")
    .asProjectManager()
    .build();

// Project 생성
Project project = TestDataFactory.project()
    .asAIProject()
    .build();

// Phase 생성
Phase phase = TestDataFactory.phase()
    .project(project)
    .asDevelopmentPhase()
    .inProgress()
    .build();
```

### 2. 벌크 데이터 생성 (권장)

```java
// 모든 테스트 사용자
List<User> users = TestDataFactory.createTestUsers();

// 모든 테스트 프로젝트
List<Project> projects = TestDataFactory.createTestProjects();

// 프로젝트의 모든 페이즈
List<Phase> phases = TestDataFactory.createPhasesForProject(project);

// 프로젝트의 모든 멤버
List<ProjectMember> members = TestDataFactory.createMembersForProject(project);
```

### 3. 테스트에 사용

```java
@SpringBootTest
@ActiveProfiles("test")
class MyTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void myTest() {
        // Given: 테스트 데이터 생성
        Project project = TestDataFactory.project()
            .asAIProject()
            .build();
        projectRepository.save(project);

        // When: 테스트 실행
        Project result = projectRepository.findById(project.getId());

        // Then: 검증
        assertThat(result).isNotNull();
    }
}
```

## 주요 Builder 패턴

### User
```java
TestDataFactory.user()
    .id("user-001")
    .email("kim@test.com")
    .name("김철수")
    .role(User.UserRole.DEVELOPER)
    // 또는 .asAdmin(), .asProjectManager(), .asDeveloper() 등
    .build();
```

### Project
```java
TestDataFactory.project()
    .id("proj-001")
    .name("프로젝트명")
    .status(Project.ProjectStatus.IN_PROGRESS)
    // 또는 .asAIProject(), .asMobileProject() 등
    .inProgress()
    .withBudget(100_000_000)
    .build();
```

### Phase
```java
TestDataFactory.phase()
    .project(project)
    .name("페이즈명")
    .orderNum(1)
    // 또는 .asDesignPhase(), .asDevelopmentPhase() 등
    .status(Phase.PhaseStatus.IN_PROGRESS)
    .build();
```

### Requirement
```java
TestDataFactory.requirement()
    .projectId(project.getId())
    .code("REQ-001")
    .title("요구사항명")
    // 또는 .asAIRequirement(), .asSecurityRequirement() 등
    .category(RequirementCategory.FUNCTIONAL)
    .priority(Priority.HIGH)
    .status(RequirementStatus.APPROVED)
    .build();
```

### Task
```java
TestDataFactory.task()
    .column(kanbanColumn)
    .title("작업명")
    // 또는 .asDatasetPreparation(), .asModelTuning() 등
    .priority(Task.Priority.HIGH)
    .status(Task.TaskStatus.IN_PROGRESS)
    .assignedTo("user-004")
    .trackTypeAI()
    .build();
```

## 자주 사용하는 조합

### 시나리오 1: 기본 프로젝트 설정
```java
// 프로젝트 생성
Project project = TestDataFactory.project()
    .asAIProject()
    .build();

// 사용자 생성
List<User> users = TestDataFactory.createTestUsers();

// 멤버 할당
List<ProjectMember> members = TestDataFactory.createMembersForProject(project);

// 저장소에 저장
projectRepository.save(project);
userRepository.saveAll(users);
projectMemberRepository.saveAll(members);
```

### 시나리오 2: RFP 및 요구사항
```java
// RFP 생성
Rfp rfp = TestDataFactory.rfp()
    .projectId(project.getId())
    .asAIRequirements()
    .asSubmitted()
    .build();

// 요구사항 생성
List<Requirement> requirements = TestDataFactory.createRequirementsForProject(project.getId());

// 저장
rfpRepository.save(rfp);
requirementRepository.saveAll(requirements);
```

### 시나리오 3: Sprint 및 Task
```java
// Sprint 생성
List<Sprint> sprints = TestDataFactory.createSprintsForProject(project.getId());

// Kanban Column 생성
List<KanbanColumn> columns = TestDataFactory.createKanbanColumnsForProject(project.getId());

// Task 생성
KanbanColumn todoCol = columns.get(0);
List<Task> tasks = TestDataFactory.createTasksForColumn(todoCol);

// 저장
sprintRepository.saveAll(sprints);
kanbanRepository.saveAll(columns);
taskRepository.saveAll(tasks);
```

## 데이터 커스터마이징

모든 빌더는 메서드 체이닝을 지원합니다:

```java
User customUser = TestDataFactory.user()
    .id("custom-user-001")
    .email("custom@test.com")
    .name("커스텀 사용자")
    .role(User.UserRole.QA)
    .department("QA팀")
    .active(true)
    .build();

Task customTask = TestDataFactory.task()
    .column(kanbanColumn)
    .title("커스텀 작업")
    .description("상세 설명")
    .priority(Task.Priority.CRITICAL)
    .status(Task.TaskStatus.REVIEW)
    .assigneeId("user-006")
    .dueDate(LocalDate.now().plusDays(3))
    .trackTypeSI()
    .withTag("urgent")
    .build();
```

## 고유성 처리

ID와 이메일은 고유해야 합니다:

```java
// 잘못된 방법 - 중복 ID 가능
for (int i = 0; i < 10; i++) {
    User user = TestDataFactory.user().build(); // 중복 가능
}

// 올바른 방법 1 - 명시적 ID 지정
for (int i = 0; i < 10; i++) {
    User user = TestDataFactory.user()
        .id("user-" + i)
        .email("user" + i + "@test.com")
        .build();
}

// 올바른 방법 2 - UUID 사용
User user = TestDataFactory.user()
    .id("user-" + UUID.randomUUID())
    .email("user-" + System.nanoTime() + "@test.com")
    .build();
```

## IDE 자동완성 팁

모든 Builder 클래스는 `TestXxxBuilder` 패턴을 따릅니다.
IDE의 자동완성을 사용하여 쉽게 찾을 수 있습니다:

1. `TestDataFactory.` 입력 후 Ctrl+Space
2. 원하는 빌더 선택 (예: `project()`)
3. 메서드 체이닝으로 필드 설정
4. `.build()` 호출

## 예제 테스트 보기

자세한 사용 예제는 [TestDataFactoryExampleTest.java](TestDataFactoryExampleTest.java)를 참고하세요:

```bash
src/test/java/com/insuretech/pms/support/TestDataFactoryExampleTest.java
```

## 문제 해결

### "고유 제약 조건 위반" 오류
→ ID 또는 이메일이 중복되었습니다. 명시적으로 고유한 값을 지정하세요.

### "외래 키 제약 조건 오류"
→ 부모 엔티티를 먼저 저장한 후 자식 엔티티를 생성하세요.

### "Null Pointer Exception"
→ 필수 필드를 빌더에서 설정했는지 확인하세요 (예: `project()`, `column()`)

## 성능 팁

- **대량 데이터**: 한 번에 모든 엔티티를 생성하지 말고 필요한 것만 생성
- **배치 저장**: `saveAll()` 사용으로 성능 향상
- **트랜잭션**: `@Transactional` 사용으로 데이터베이스 쓰기 최적화

```java
@Test
@Transactional
void testWithBatchInsert() {
    // 생성
    List<Task> tasks = TestDataFactory.createTasksForColumn(column);

    // 배치 저장
    taskRepository.saveAll(tasks); // 훨씬 빠름
}
```

## 다음 단계

1. [README.md](README.md) - 상세 문서 읽기
2. [TestDataFactoryExampleTest.java](TestDataFactoryExampleTest.java) - 모든 예제 살펴보기
3. 자신의 테스트에 `TestDataFactory` 적용하기

Happy Testing! 🎉
