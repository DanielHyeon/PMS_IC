# PMS RFP 관리 기능 - 구현 계획서 (v2.0)

## 3개 추가 고려사항 통합 버전

**작성일**: 2026-01-17
**버전**: 2.0
**상태**: Ready for Implementation

---

## 1. 추가 고려사항 종합 분석

### 1.1 Backlog 엔티티와의 관계 명확화

#### 현재 상태 분석

- **설계서**: RFP → Requirement → Sprint/Task 매핑 정의
- **구현**: Backlog 엔티티가 없음 (존재하지 않음)
- **문제**: PMS 표준 플로우(요구사항 → 제품 백로그 → Sprint → Kanban)에서 Backlog 역할 불명확

#### 개념 설계

**Backlog의 역할:**

```
Backlog 엔티티의 목적:
├── 1. Requirement 수집 공간
│   ├── RFP에서 자동 추출된 Requirement
│   └── 수동으로 등록된 Requirement (고객 인터뷰, 피드백 등)
├── 2. 우선순위 정렬 공간
│   ├── Drag-n-drop으로 순서 변경
│   ├── Story Point 추정
│   └── Epic 연결
└── 3. Sprint Planning 소스
    └── 상위 아이템 pull → Sprint Backlog 생성
```

**데이터베이스 설계:**

```sql
-- 새로운 Backlog 엔티티
CREATE TABLE project.backlogs (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) DEFAULT 'Product Backlog',
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_backlog_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id)
);

-- Backlog Item (Requirement과 함께 관리)
CREATE TABLE project.backlog_items (
    id VARCHAR(36) PRIMARY KEY,
    backlog_id VARCHAR(36) NOT NULL,
    requirement_id VARCHAR(36), -- Nullable: 수동 등록 item도 지원
    epic_id VARCHAR(36), -- Epic 연결
    priority_order INTEGER NOT NULL, -- 우선순위 순서
    status VARCHAR(50) DEFAULT 'BACKLOG', -- BACKLOG, SELECTED, SPRINT, COMPLETED
    story_points INTEGER, -- Story Point 추정
    estimated_effort INTEGER, -- 추정 시간/포인트
    acceptance_criteria TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_backlog_item_backlog FOREIGN KEY (backlog_id)
        REFERENCES project.backlogs(id),
    CONSTRAINT fk_backlog_item_requirement FOREIGN KEY (requirement_id)
        REFERENCES project.requirements(id),
    CONSTRAINT uk_backlog_requirement UNIQUE (backlog_id, requirement_id)
);
```

**프로세스 플로우:**

```
1. 요구사항 수집 (고객 인터뷰, 피드백, RFP)
   └─> Requirement 생성 (자동 추출 또는 수동)

2. Product Backlog 화면 (Backlog 엔티티 UI)
   ├─> Backlog Item 추가 (Requirement 또는 수동 생성)
   ├─> Drag-n-drop 우선순위 정렬
   ├─> Story Point 추정
   ├─> Epic 연결
   └─> Status 전환: BACKLOG → SELECTED

3. Sprint Planning
   └─> 상위 N개 SELECTED items → Sprint으로 pull
   └─> Status 변경: SELECTED → SPRINT
   └─> User Story 생성 (스프린트 백로그)

4. Active Sprint (Kanban Board)
   └─> Task 이동: TODO → IN_PROGRESS → REVIEW → DONE
   └─> Task 완료 시 부모 Requirement의 진행률 자동 업데이트

5. Requirement 변경 시
   └─> Product Backlog로 이동
   └─> 우선순위 재정렬
   └─> 다음 스프린트에 반영
```

---

### 1.2 Story Point 필드 추가 (설계서에서 누락)

#### 현재 상태

- **구현됨**: UserStory.storyPoints (Integer)
- **누락됨**: Requirement 엔티티에 Story Point 필드 없음
- **문제**: Requirement 진행률 계산에 노력 예측 불가능

#### 설계 개선사항

**Requirement 엔티티 확장:**

```java
@Entity
@Table(name = "requirements", schema = "project")
public class Requirement extends BaseEntity {
    // ... 기존 필드 ...

    @Column(name = "story_points")
    private Integer storyPoints; // ✓ 추가

    @Column(name = "estimated_effort_hours")
    private Integer estimatedEffortHours; // 세부 예측 시간

    @Column(name = "actual_effort_hours")
    private Integer actualEffortHours; // 실제 소비 시간

    // Burndown 추적용
    @Column(name = "remaining_effort_hours")
    private Integer remainingEffortHours;
}
```

**Story Point 처리:**

```
Backlog Item 생성 시:
├─ Requirement 선택 케이스:
│  └─ backlog_item.story_points = requirement.story_points (자동)
└─ 수동 생성 케이스:
   └─ backlog_item.story_points = 입력값

Story Point 변경 시:
├─ BacklogItem에서 변경 (Product Backlog 화면)
└─ 동기화 로직:
   └─ backlog_item.story_points → requirement.story_points (UPDATE)

Sprint 계획 시:
├─ 선택된 Item의 story_points 합산 = Sprint Capacity 계획
└─ Sprint 진행 중:
   └─ Task 완료 → Requirement progress% 자동 업데이트
   └─ 완료된 story_points 합산 = Velocity 계산
```

---

### 1.3 Status vs Progress 혼동 해결

#### 현재 문제

- **설계서**: 4-level progress (NOT_STARTED/IN_PROGRESS/COMPLETED/DELAYED)
- **구현**: 7-level status (IDENTIFIED/ANALYZED/APPROVED/IMPLEMENTED/VERIFIED/DEFERRED/REJECTED)
- **혼동**: Status와 Progress의 의미가 섞임

#### 설계 명확화

**Requirement 상태 모델 재정의:**

```
생명주기 상태 (Status) - 비즈니스 관점:
├─ IDENTIFIED: 요구사항 발견됨 (RFP에서 추출 또는 수동)
├─ ANALYZED: 분석 완료 (상세 내용 정리, 수용성 검증)
├─ APPROVED: 승인됨 (Product Owner 승인, Sprint에 포함 가능)
├─ IMPLEMENTED: 구현 중 또는 완료됨 (한 개 이상의 Task 링크)
├─ VERIFIED: 검증 완료 (QA 승인, 프로덕션 배포)
├─ DEFERRED: 연기됨 (다음 릴리스로 미루기)
└─ REJECTED: 거절됨 (구현하지 않기로 결정)

진행 상태 (Progress) - 작업 진척도:
├─ progress_percentage: 0~100 (%)
│  └─ 계산식: (completed_tasks_count / total_linked_tasks_count) × 100
├─ progress_stage: 4단계 (계산됨, read-only)
│  ├─ NOT_STARTED: progress = 0%
│  ├─ IN_PROGRESS: 0% < progress < 100%
│  ├─ COMPLETED: progress = 100%
│  └─ DELAYED: 기한 초과 + progress < 100%
└─ last_progress_update: 마지막 진행률 변경 시간
```

**Requirement 엔티티 업데이트:**

```java
@Entity
@Table(name = "requirements", schema = "project")
public class Requirement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequirementStatus status; // IDENTIFIED, ANALYZED, APPROVED, ...

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0; // 0~100

    @Transient
    private ProgressStage progressStage; // Derived: NOT_STARTED, IN_PROGRESS, ...

    @Column(name = "last_progress_update")
    private LocalDateTime lastProgressUpdate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // 계산 메서드
    @PostLoad
    @PostPersist
    @PostUpdate
    private void calculateProgressStage() {
        if (dueDate != null && LocalDate.now().isAfter(dueDate)
            && progressPercentage < 100) {
            this.progressStage = ProgressStage.DELAYED;
        } else if (progressPercentage == 0) {
            this.progressStage = ProgressStage.NOT_STARTED;
        } else if (progressPercentage == 100) {
            this.progressStage = ProgressStage.COMPLETED;
        } else {
            this.progressStage = ProgressStage.IN_PROGRESS;
        }
    }
}

public enum ProgressStage {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    DELAYED
}
```

---

### 1.4 WIP 제한 고려사항 (동적 WIP + CONWIP)

#### 현재 상태

- **구현**: KanbanColumn.wipLimit 필드 있음
- **문제**: WIP 제한 검증 로직 없음 (UI에서만 표시)

#### 설계 개선사항

**다층 WIP 제한 전략:**

```
1. 동적 WIP 제한 (병목 단계별)
   ├─ TODO: 8개 (개발 시작)
   ├─ IN_PROGRESS: 6개 (병목: 개발)
   ├─ REVIEW: 3개 (병목: 리뷰)
   └─ DONE: Unlimited (처리 완료)

2. 클래스 오브 서비스 (우선순위별 우회)
   ├─ URGENT: 별도 열 (WIP 제한 우회)
   ├─ HIGH: 일반 열 (WIP 제한 적용)
   ├─ MEDIUM: 일반 열
   └─ LOW: 일반 열

3. 개인 WIP 제한 (개발자별)
   └─ 1인당 최대 3개 작업 (컨텍스트 스위칭 최소화)

4. CONWIP (전체 스프린트 WIP 제한)
   └─ 스프린트 전체 WIP = min(sprint_capacity, 12개)
```

**데이터베이스 설계:**

```sql
-- KanbanColumn 확장
ALTER TABLE task.kanban_columns
ADD COLUMN wip_limit_soft INTEGER; -- Soft limit (경고)
ADD COLUMN wip_limit_hard INTEGER; -- Hard limit (차단)
ADD COLUMN is_bottleneck_column BOOLEAN DEFAULT FALSE;
ADD COLUMN service_class VARCHAR(50); -- STANDARD, EXPRESS, URGENT

-- Sprint WIP 설정
ALTER TABLE task.sprints
ADD COLUMN conwip_limit INTEGER; -- 전체 WIP 제한
ADD COLUMN enable_wip_validation BOOLEAN DEFAULT TRUE;

-- 사용자별 WIP 제한
CREATE TABLE task.user_wip_limits (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    max_wip_per_person INTEGER DEFAULT 3,
    created_at TIMESTAMP,
    CONSTRAINT fk_user_wip_user FOREIGN KEY (user_id)
        REFERENCES auth.users(id),
    CONSTRAINT uk_user_wip UNIQUE (user_id, project_id)
);

-- Task의 Service Class 추가
ALTER TABLE task.tasks
ADD COLUMN service_class VARCHAR(50) DEFAULT 'STANDARD'; -- STANDARD, EXPRESS, URGENT
```

**WIP 검증 Service:**

- validateColumnWipLimit(): 칼럼별 Soft/Hard limit 확인
- validatePersonalWipLimit(): 1인 최대 작업 수 확인
- validateConwip(): 스프린트 전체 WIP 확인
- getSuggestions(): 최적화 제안

---

## 2. 통합 구현 플랜

### Phase 1: 기반 설계 및 데이터 모델 (1주)

#### Week 1 (2026-01-20~01-24)

**Task 1.1: 데이터베이스 마이그레이션 (V20260120)**

```
- [ ] Backlog 엔티티 테이블 생성
- [ ] BacklogItem 테이블 생성
- [ ] Requirement 필드 확장 (story_points, effort fields)
- [ ] Progress 필드 표준화 (progress_percentage 이름 변경)
- [ ] WIP 필드 확장 (KanbanColumn, Sprint, UserWipLimits)
- [ ] View/Index 생성
```

**Task 1.2: Backend 엔티티 구현**

```
- [ ] Backlog.java 엔티티 생성
- [ ] BacklogItem.java 엔티티 생성
- [ ] Requirement.java 확장 (story_points 추가)
- [ ] Progress 계산 로직 (@PostLoad/@PostUpdate)
- [ ] 모든 엔티티에 필드 주석 추가 (영어)
```

**Task 1.3: Repository 인터페이스**

```
- [ ] BacklogRepository
- [ ] BacklogItemRepository
- [ ] 기존 Repository 메서드 추가
```

**Deliverable**:

- Database migration script
- 5개 새로운 Entity Java classes
- 3개 new Repository interfaces

---

### Phase 2: 백로그 관리 기능 (2주)

#### Week 2 (2026-01-27~01-31)

**Task 2.1: BacklogService 구현**

```
- [ ] BacklogServiceImpl 구현
- [ ] BacklogItemServiceImpl 구현
- [ ] Lineage event publishing
- [ ] Unit tests
```

**Task 2.2: BacklogController 구현**

```
GET  /api/v1/projects/{projectId}/backlog
POST /api/v1/projects/{projectId}/backlog/items
PUT  /api/v1/projects/{projectId}/backlog/items/{itemId}/order
PUT  /api/v1/projects/{projectId}/backlog/items/{itemId}/story-points
POST /api/v1/projects/{projectId}/backlog/select-for-sprint
GET  /api/v1/projects/{projectId}/backlog/velocity
```

**Task 2.3: UI Component (Frontend)**

```
- [ ] ProductBacklogPage.tsx
- [ ] BacklogItemCard.tsx
- [ ] Drag-drop integration
- [ ] Story Point UI
```

---

#### Week 3 (2026-02-03~02-07)

**Task 2.4: Sprint Planning 통합**

```
- [ ] SprintService.createSprintFromBacklog() 구현
- [ ] Backlog item 상태 전환 로직
- [ ] User Story 자동 생성
```

**Task 2.5: Sprint Board 연계**

```
- [ ] Sprint 시작 시 자동 Task 생성
- [ ] Kanban 칼럼에 Task 배치
- [ ] Backlog item 상태 자동 추적
```

**Task 2.6: Requirement Progress 자동 업데이트**

```
- [ ] Event listener: Task 상태 변경 → Progress% 업데이트
- [ ] ProgressStage 계산
- [ ] Integration tests
```

---

### Phase 3: 상태 및 진행률 명확화 (1주)

#### Week 4 (2026-02-10~02-14)

**Task 3.1-3.4: Status vs Progress 명확화**

```
- [ ] RequirementResponse DTO 업데이트
- [ ] View 생성
- [ ] Frontend components 개선
- [ ] Metrics/Reports
```

---

### Phase 4: WIP 제한 구현 (2주)

#### Week 5-6 (2026-02-17~02-28)

**Task 4.1-4.8: WIP 검증 및 시각화**

```
- [ ] WipValidationService 구현
- [ ] KanbanService 통합
- [ ] WIP UI components
- [ ] Dashboard 및 recommendations
- [ ] Comprehensive tests
```

---

### Phase 5: 보고서 생성 기능 (2주)

#### Week 7-8 (2026-03-03~03-14)

**Task 5.1-5.6: Report 생성 및 UI**

```
- [ ] WeeklyReportService 구현
- [ ] LLM 연계
- [ ] Report UI components
- [ ] E2E 테스트
```

---

## 3. 구현 우선순위

### MVP (4주) - 필수 기능

```
✓ Backlog 관리 (CRUD, ordering, story points)
✓ Sprint Planning (backlog → sprint)
✓ Requirement Progress 자동 계산
✓ Status vs Progress 명확화
```

### Phase 2 (6주) - 중요 기능

```
✓ WIP 제한 검증 (soft/hard/CONWIP)
✓ WIP 시각화 대시보드
```

### Phase 3 (8주) - 고급 기능

```
✓ 주간보고서 생성 (AI)
✓ 보고서 버전 관리
```

---

## 4. 성공 기준 및 테스트

### Unit Tests

```
- Entity: 계산 로직 (progress%, stage)
- Service: CRUD, state transitions, validations
- Repository: 쿼리 정확성
- Coverage: 80% 이상
```

### Integration Tests

```
- Backlog → Sprint Planning workflow
- Task completion → Progress update
- WIP validation flows
- Report generation pipeline
```

### E2E Tests

```
- 전체 요구사항 → 백로그 → 스프린트 → 칸반 → 완료 플로우
- 다중 스프린트 시나리오
- WIP 위반 및 복구 시나리오
```

---

## 5. 위험 요소 및 대응


| 위험                    | 영향도 | 대응                            |
| ----------------------- | ------ | ------------------------------- |
| LLM 요구사항 추출 실패  | High   | Fallback: 수동 입력 UI + 템플릿 |
| WIP 검증 성능 저하      | Medium | DB 인덱스 최적화 + Redis 캐싱   |
| Story Point 추정 어려움 | Medium | 팀 코칭 + Planning Poker 도구   |
| Neo4j 동기화 지연       | Medium | Event-driven async 처리 + retry |

---

## 6. 팀 리소스 배분

```
Backend (2명):
├─ 1명: Entity, Service, Repository
└─ 1명: Controller, API, Security & Tests

Frontend (1명):
├─ UI components
└─ State management

QA (1명):
├─ Test case authoring
└─ E2E automation

AI/LLM (1명, part-time):
└─ Report generator integration
```
