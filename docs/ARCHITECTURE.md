# PMS Insurance Claims - 시스템 아키텍처 문서

> **버전**: 2.2
> **최종 업데이트**: 2026-01-17
> **작성자**: PMS Insurance Claims Team

---

## 목차

1. [시스템 개요](#1-시스템-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [백엔드 (Spring Boot)](#3-백엔드-spring-boot)
4. [프론트엔드 (React)](#4-프론트엔드-react)
5. [LLM 서비스 (Flask + LangGraph)](#5-llm-서비스-flask--langgraph)
6. [데이터베이스 스키마](#6-데이터베이스-스키마)
7. [API 설계](#7-api-설계)
8. [보안 아키텍처](#8-보안-아키텍처)
9. [데이터 계보 (Lineage) 아키텍처](#9-데이터-계보-lineage-아키텍처)
10. [배포 및 인프라](#10-배포-및-인프라)

---

## 1. 시스템 개요

### 1.1 시스템 목적

PMS Insurance Claims는 **보험 심사 프로젝트의 전주기 관리**를 위한 AI 통합 플랫폼입니다.  
Neo4j GraphRAG 기반의 지능형 챗봇을 통해 프로젝트 관리 의사결정을 지원합니다.

### 1.2 핵심 기능

| 기능 영역 | 설명 |
|-----------|------|
| **프로젝트 관리** | 프로젝트 생성, 페이즈 관리, 진척률 추적 |
| **태스크 관리** | 칸반 보드, 백로그 관리, 스프린트 계획 |
| **이슈/리스크 관리** | 이슈 등록, 리스크 평가, 해결 추적 |
| **산출물 관리** | 문서 업로드, 승인 워크플로우, 버전 관리 |
| **AI 어시스턴트** | RAG 기반 질의응답, 프로젝트 현황 분석 |
| **Lineage & History** | 데이터 계보 시각화, 활동 타임라인, 영향 분석 |
| **교육 관리** | 교육 프로그램, 세션 관리, 이수 이력 |
| **권한 관리** | 역할 기반 접근 제어 (RBAC) |

### 1.3 기술 스택 요약

| 레이어 | 기술 | 버전 |
|--------|------|------|
| Frontend | React + TypeScript + Vite | React 18, Vite 5 |
| Backend | Spring Boot + JPA | Spring Boot 3.2 |
| LLM Service | Flask + LangGraph | Python 3.11 |
| Database | PostgreSQL | 15 |
| Graph DB | Neo4j | 5.20 |
| Cache | Redis | 7 |
| Container | Docker Compose | - |

---

### 1.4 공통 개발 원칙

  이 워크스페이스는 여러 AI 관련 프로젝트를 포함하며, 공통 개발 원칙을 따릅니다.

  ### 테스트 기반 개발 (TDD)
  - **새로운 기능 구현 시**: 테스트를 먼저 작성한 후 구현
  - **테스트 프레임워크**: pytest 사용
  - **테스트 범위**: 핵심 로직에 대한 단위 테스트 필수
  
  ### Git 워크플로우
  - **커밋 규칙**:
    - 테스트 통과 후 커밋
    - 변경한 파일만 선택적으로 add (`git add <specific-files>`)
    - 기능별 작은 단위로 커밋
  - **브랜치 전략**:
    - Git Worktree를 활용한 멀티 브랜치 작업 지원 (상세: `WORKTREE_GUIDE.md`)
    - 직접 push 금지 → PR을 통한 코드 리뷰
  - **커밋 메시지**: 명확하고 설명적인 메시지 작성 (예: "Add Slack event validation")
  
  ### MCP (Model Context Protocol) 활용
  - **Context7**: 라이브러리 문서 조회
  - **Sequential**: 복잡한 분석 및 디버깅
  - **Linear**: 이슈 트래킹 및 프로젝트 관리
  
  ### FrontEnd Code Style
- Functional components
- Airbnb ESLint
- JSDoc for public functions
- 한글 주석은 영어로 변환

### FrontEnd Commands
- dev: `npm run dev`
- build: `npm run build`
- test: `npm test`
- lint: `npm run lint`

### FrontEnd Important Notes
- API calls는 /api 폴더에서만


---

## 2. 전체 아키텍처

### 2.1 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Layer                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    React SPA (Vite)                          │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │   │
│  │  │Dashboard │ │ Kanban   │ │ AI Chat  │ │ Settings │       │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP/REST (Port 5173)
┌────────────────────────────▼────────────────────────────────────────┐
│                      Application Layer                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              Spring Boot Backend (Port 8083)                 │   │
│  │  ┌────────────────────────────────────────────────────────┐ │   │
│  │  │                    REST Controllers                     │ │   │
│  │  │  Auth │ Project │ Task │ Chat │ Issue │ Education      │ │   │
│  │  └────────────────────────────────────────────────────────┘ │   │
│  │  ┌────────────────────────────────────────────────────────┐ │   │
│  │  │                    Service Layer                        │ │   │
│  │  │  AuthService │ ProjectService │ ChatService │ ...      │ │   │
│  │  └────────────────────────────────────────────────────────┘ │   │
│  │  ┌────────────────────────────────────────────────────────┐ │   │
│  │  │                  Repository Layer (JPA)                 │ │   │
│  │  └────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
└───────┬──────────────────────┬──────────────────────┬───────────────┘
        │                      │                      │
┌───────▼───────┐      ┌───────▼───────┐      ┌───────▼───────┐
│  PostgreSQL   │      │     Redis     │      │  LLM Service  │
│  (Port 5433)  │      │  (Port 6379)  │      │  (Port 8000)  │
│               │      │               │      │               │
│ ┌───────────┐ │      │ • Session     │      │ ┌───────────┐ │
│ │ auth      │ │      │ • Cache       │      │ │  Flask    │ │
│ │ project   │ │      │ • Rate Limit  │      │ │ LangGraph │ │
│ │ task      │ │      └───────────────┘      │ │ llama-cpp │ │
│ │ chat      │ │                             │ └─────┬─────┘ │
│ │ report    │ │                             └───────┼───────┘
│ └───────────┘ │                                     │
└───────────────┘                             ┌───────▼───────┐
                                              │    Neo4j      │
                                              │  (Port 7687)  │
                                              │               │
                                              │ • Vector Index│
                                              │ • Graph Store │
                                              │ • RAG Chunks  │
                                              └───────────────┘
```

### 2.2 서비스 간 통신

| Source | Target | Protocol | 용도 |
|--------|--------|----------|------|
| Frontend | Backend | REST/HTTP | API 호출 |
| Backend | LLM Service | REST/HTTP | AI 채팅 요청 |
| Backend | PostgreSQL | JDBC | 데이터 영속화 |
| Backend | Redis | Redis Protocol | 세션/캐시 |
| LLM Service | Neo4j | Bolt | RAG 검색 |

---

## 3. 백엔드 (Spring Boot)

### 3.1 모듈 구조

```
com.insuretech.pms/
├── PmsApplication.java          # 메인 애플리케이션
├── auth/                        # 인증/인가 모듈
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   └── PermissionController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── JwtTokenProvider.java
│   │   └── UserService.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Permission.java
│   │   └── RolePermission.java
│   ├── repository/
│   └── dto/
│
├── project/                     # 프로젝트 관리 모듈
│   ├── controller/
│   │   ├── ProjectController.java
│   │   ├── PhaseController.java
│   │   ├── DeliverableController.java
│   │   ├── IssueController.java
│   │   ├── MeetingController.java
│   │   └── KpiController.java
│   ├── service/
│   ├── entity/
│   │   ├── Project.java
│   │   ├── Phase.java
│   │   ├── Deliverable.java
│   │   ├── Issue.java
│   │   ├── Meeting.java
│   │   └── Kpi.java
│   └── repository/
│
├── task/                        # 태스크 관리 모듈
│   ├── controller/
│   │   ├── TaskController.java
│   │   └── UserStoryController.java
│   ├── service/
│   │   ├── TaskService.java
│   │   ├── UserStoryService.java
│   │   └── KanbanBoardService.java
│   ├── entity/
│   │   ├── Task.java
│   │   ├── UserStory.java
│   │   ├── KanbanColumn.java
│   │   └── Sprint.java
│   └── repository/
│
├── chat/                        # AI 챗봇 모듈
│   ├── controller/
│   │   ├── ChatController.java
│   │   └── LlmController.java
│   ├── service/
│   │   ├── ChatService.java
│   │   ├── AIChatClient.java
│   │   └── ProjectDataService.java
│   ├── entity/
│   │   ├── ChatSession.java
│   │   └── ChatMessage.java
│   └── repository/
│
├── education/                   # 교육 관리 모듈
│   ├── controller/
│   ├── service/
│   ├── entity/
│   └── repository/
│
├── report/                      # 리포트/대시보드 모듈
│   ├── controller/
│   │   └── DashboardController.java
│   ├── service/
│   │   └── DashboardService.java
│   └── dto/
│       └── DashboardStats.java
│
├── lineage/                     # Lineage & History 모듈
│   ├── controller/
│   │   └── LineageController.java
│   ├── service/
│   │   ├── LineageQueryService.java
│   │   └── LineageEventProducer.java
│   ├── entity/
│   │   └── OutboxEvent.java
│   ├── repository/
│   │   └── OutboxEventRepository.java
│   └── dto/
│       ├── LineageGraphDto.java
│       ├── LineageNodeDto.java
│       ├── LineageEdgeDto.java
│       ├── LineageEventDto.java
│       ├── LineageStatisticsDto.java
│       ├── LineageTreeDto.java
│       └── ImpactAnalysisDto.java
│
└── common/                      # 공통 모듈
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── CorsConfig.java
    │   ├── RedisConfig.java
    │   └── WebClientConfig.java
    ├── entity/
    │   └── BaseEntity.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── CustomException.java
    └── dto/
        └── ApiResponse.java
```

### 3.2 주요 엔티티

#### User (사용자)

```java
@Entity
@Table(name = "users", schema = "auth")
public class User extends BaseEntity {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;  // SPONSOR, PMO_HEAD, PM, DEVELOPER, QA, etc.
    
    private String department;
    private Boolean active;
    private LocalDateTime lastLoginAt;
}
```

#### Project (프로젝트)

```java
@Entity
@Table(name = "projects", schema = "project")
public class Project extends BaseEntity {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;  // PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED
    
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer progress;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<Phase> phases;
}
```

#### Task (태스크)

```java
@Entity
@Table(name = "tasks", schema = "task")
public class Task extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id")
    private KanbanColumn column;
    
    private String phaseId;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    private String assigneeId;
    
    @Enumerated(EnumType.STRING)
    private Priority priority;      // LOW, MEDIUM, HIGH, CRITICAL
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;      // TODO, IN_PROGRESS, REVIEW, DONE
    
    @Enumerated(EnumType.STRING)
    private TrackType trackType;    // AI, SI, COMMON
    
    private LocalDate dueDate;
    private Integer orderNum;
    private String tags;
}
```

#### ChatSession / ChatMessage (채팅)

```java
@Entity
@Table(name = "chat_sessions", schema = "chat")
public class ChatSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String userId;
    private String title;
    private Boolean active;
}

@Entity
@Table(name = "chat_messages", schema = "chat")
public class ChatMessage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatSession session;
    
    @Enumerated(EnumType.STRING)
    private Role role;  // USER, ASSISTANT
    
    @Column(columnDefinition = "TEXT")
    private String content;
}
```

### 3.3 설정 (application.yml)

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  datasource:
    url: jdbc:postgresql://postgres:5432/pms_db
    username: ${POSTGRES_USER:pms_user}
    password: ${POSTGRES_PASSWORD:pms_password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        default_schema: public
  
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6379}

ai:
  service:
    url: ${AI_SERVICE_URL:http://llm-service:8000}
    model: ${AI_SERVICE_MODEL:google.gemma-3-12b-pt.Q5_K_M.gguf}

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400000  # 24시간
```

---

## 4. 프론트엔드 (React)

### 4.1 디렉토리 구조

```
PMS_IC_FrontEnd_v1.2/
├── src/
│   ├── main.tsx                 # 애플리케이션 진입점
│   ├── app/
│   │   ├── App.tsx              # 메인 앱 컴포넌트
│   │   └── components/
│   │       ├── AIAssistant.tsx        # AI 챗봇 UI
│   │       ├── BacklogManagement.tsx  # 백로그 관리
│   │       ├── CommonManagement.tsx   # 공통 관리
│   │       ├── Dashboard.tsx          # 대시보드
│   │       ├── EducationManagement.tsx# 교육 관리
│   │       ├── Header.tsx             # 헤더
│   │       ├── KanbanBoard.tsx        # 칸반 보드
│   │       ├── LoginScreen.tsx        # 로그인
│   │       ├── PhaseManagement.tsx    # 페이즈 관리
│   │       ├── ProjectSelector.tsx    # 프로젝트 선택
│   │       ├── RequirementManagement.tsx # 요구사항 관리
│   │       ├── RfpManagement.tsx      # RFP 관리
│   │       ├── RoleManagement.tsx     # 역할/권한 관리
│   │       ├── Settings.tsx           # 설정
│   │       ├── Sidebar.tsx            # 사이드바
│   │       ├── TaskFormModal.tsx      # 태스크 폼
│   │       ├── WeeklyReportManagement.tsx # 주간 보고
│   │       ├── lineage/               # Lineage UI 컴포넌트
│   │       │   ├── index.ts           # 모듈 exports
│   │       │   ├── LineageManagement.tsx  # 메인 Lineage 관리 화면
│   │       │   ├── LineageGraph.tsx   # React Flow 기반 그래프 시각화
│   │       │   └── LineageTimeline.tsx # 활동 타임라인
│   │       └── ui/                    # 공통 UI 컴포넌트
│   │           ├── button.tsx
│   │           ├── card.tsx
│   │           ├── dialog.tsx
│   │           ├── input.tsx
│   │           └── ...
│   │
│   ├── contexts/
│   │   └── ProjectContext.tsx   # 프로젝트 상태 관리
│   │
│   ├── services/
│   │   └── api.ts               # API 서비스 클래스
│   │
│   ├── mocks/
│   │   ├── index.ts             # Mock 데이터
│   │   └── dashboard.mock.ts
│   │
│   ├── types/
│   │   ├── project.ts           # TypeScript 타입 정의
│   │   └── lineage.ts           # Lineage 관련 타입 정의
│   │
│   ├── utils/
│   │   └── status.ts            # 유틸리티 함수
│   │
│   └── styles/
│       ├── index.css
│       ├── tailwind.css
│       └── theme.css
│
├── index.html
├── vite.config.ts
├── package.json
└── postcss.config.mjs
```

### 4.2 API 서비스 구조

```typescript
// src/services/api.ts
export class ApiService {
  private token: string | null = null;
  private useMockData = false;

  // 인증 API
  async login(email: string, password: string): Promise<LoginResponse>
  
  // 대시보드 API
  async getDashboardStats(): Promise<DashboardStats>
  async getActivities(): Promise<Activity[]>
  
  // 프로젝트 API
  async getPhases(): Promise<Phase[]>
  async updatePhase(phaseId: number, data: PhaseData): Promise<Phase>
  
  // 태스크 API
  async getTaskColumns(): Promise<KanbanColumn[]>
  async createTask(task: TaskData): Promise<Task>
  async updateTask(taskId: number, data: TaskData): Promise<Task>
  async moveTask(taskId: number, toColumn: string): Promise<void>
  
  // 이슈 API
  async getIssues(projectId: string): Promise<Issue[]>
  async createIssue(projectId: string, data: IssueData): Promise<Issue>
  async updateIssueStatus(projectId: string, issueId: string, status: string): Promise<Issue>
  
  // 채팅 API
  async sendChatMessage(params: ChatParams): Promise<ChatResponse>
  
  // 산출물 API
  async uploadDeliverable(params: DeliverableParams): Promise<Deliverable>
  async approveDeliverable(deliverableId: string, approved: boolean): Promise<void>
  
  // 교육 API
  async getEducations(): Promise<Education[]>
  async createEducation(data: EducationData): Promise<Education>
  
  // 권한 API
  async getPermissions(): Promise<Permission[]>
  async updateRolePermission(role: string, permissionId: string, granted: boolean): Promise<void>
}
```

### 4.3 주요 컴포넌트 설명

| 컴포넌트 | 역할 |
|----------|------|
| `Dashboard` | 프로젝트 현황, 진척률, 예산 사용률 표시 |
| `KanbanBoard` | 드래그앤드롭 칸반 보드 |
| `AIAssistant` | RAG 기반 AI 챗봇 인터페이스 |
| `PhaseManagement` | 프로젝트 페이즈 및 게이트 관리 |
| `BacklogManagement` | 사용자 스토리 및 백로그 관리 |
| `RfpManagement` | RFP 요구사항 분류 및 관리 |
| `LineageManagement` | Lineage 시각화 및 히스토리 관리 |
| `LineageGraph` | React Flow 기반 Lineage 그래프 시각화 |
| `LineageTimeline` | 활동 이력 타임라인 |

### 4.4 Lineage UI 아키텍처

Lineage UI는 **React Flow**와 **Dagre** 라이브러리를 사용하여 데이터 계보를 시각화합니다.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       LineageManagement                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    Statistics Cards                              │    │
│  │  [Requirements] [Stories] [Tasks] [Sprints] [Coverage] [...]    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  Tabs: [Lineage Graph] [Activity Timeline] [Impact Analysis]     │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     Active Tab Content                             │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │  LineageGraph (React Flow + Dagre)                          │  │  │
│  │  │                                                              │  │  │
│  │  │    [REQ-001] ─────► [STORY-001] ─────► [TASK-001]          │  │  │
│  │  │         │                  │                                 │  │  │
│  │  │         └─────► [STORY-002] ─────► [TASK-002]              │  │  │
│  │  │                            │                                 │  │  │
│  │  │                            └─────► [TASK-003]              │  │  │
│  │  │                                                              │  │  │
│  │  │  Controls: [Zoom] [Fit] [MiniMap]                           │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                       Legend                                       │  │
│  │  [■ Requirement] [■ UserStory] [■ Task] [■ Sprint]               │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

**주요 의존성:**

- `@xyflow/react` (v12+): 인터랙티브 노드 그래프 시각화
- `@dagrejs/dagre`: 자동 계층형 레이아웃 (Left-to-Right)
- `date-fns`: 타임스탬프 포맷팅

**LineageGraph 노드 타입:**

| 타입 | 색상 | 설명 |
|------|------|------|
| REQUIREMENT | Blue (#2563eb) | 요구사항 노드 |
| USER_STORY | Purple (#9333ea) | 사용자 스토리 노드 |
| TASK | Green (#16a34a) | 태스크 노드 |
| SPRINT | Red (#dc2626) | 스프린트 노드 |

---

## 5. LLM 서비스 (Flask + LangGraph)

### 5.1 서비스 구조

```
llm-service/
├── app.py                       # Flask 메인 애플리케이션
├── chat_workflow.py             # LangGraph 워크플로우
├── rag_service_neo4j.py         # Neo4j GraphRAG 서비스
├── document_parser.py           # MinerU 문서 파서
├── pdf_ocr_pipeline.py          # PDF OCR 파이프라인
├── load_ragdata_pdfs_neo4j.py   # RAG 데이터 로더
├── requirements.txt
└── Dockerfile
```

### 5.2 LangGraph 워크플로우

```
                    ┌─────────────────────┐
                    │  classify_intent    │
                    │    (간단한 분류)     │
                    └──────────┬──────────┘
                               │
              ┌────────────────┴────────────────┐
              │                                  │
         casual                            uncertain
              │                                  │
              ▼                                  ▼
    ┌─────────────────┐              ┌─────────────────┐
    │ generate_response│              │   rag_search    │
    │  (인사말 응답)    │              │  (RAG 검색)     │
    └────────┬────────┘              └────────┬────────┘
             │                                 │
             │                                 ▼
             │                     ┌─────────────────────┐
             │                     │ verify_rag_quality  │
             │                     │   (품질 검증)        │
             │                     └──────────┬──────────┘
             │                                 │
             │                    ┌────────────┴────────────┐
             │                    │                          │
             │               품질 낮음                   품질 좋음
             │                    │                          │
             │                    ▼                          │
             │          ┌─────────────────┐                  │
             │          │  refine_query   │                  │
             │          │  (쿼리 개선)     │──────────────────┤
             │          └────────┬────────┘                  │
             │                   │                           │
             │                   └───────► rag_search        │
             │                     (재검색)                   │
             │                                               │
             │                                               ▼
             │                                    ┌─────────────────┐
             │                                    │  refine_intent  │
             │                                    │  (의도 재분류)    │
             │                                    └────────┬────────┘
             │                                             │
             │                                             ▼
             │                                  ┌─────────────────────┐
             └─────────────────────────────────►│ generate_response   │
                                                │   (LLM 응답 생성)    │
                                                └──────────┬──────────┘
                                                           │
                                                           ▼
                                                        [ END ]
```

### 5.3 Neo4j GraphRAG 검색

```python
# rag_service_neo4j.py

class RAGServiceNeo4j:
    """Neo4j 기반 GraphRAG 서비스 - 벡터 + 그래프 통합"""
    
    def __init__(self):
        # Neo4j 연결
        self.driver = GraphDatabase.driver(neo4j_uri, auth=(user, password))
        
        # 임베딩 모델: multilingual-e5-large (1024차원)
        self.embedding_model = SentenceTransformer('intfloat/multilingual-e5-large')
        
        # MinerU 문서 파서 초기화
        self.parser = MinerUDocumentParser()
        self.chunker = LayoutAwareChunker(max_chunk_size=800, overlap=100)
    
    def search(self, query: str, top_k: int = 3, use_graph_expansion: bool = True):
        """하이브리드 검색 (벡터 + 그래프 확장)"""
        
        # 1. 쿼리 임베딩 생성
        query_embedding = self.embedding_model.encode(f"query: {query}")
        
        # 2. Neo4j 벡터 검색
        if use_graph_expansion:
            # GraphRAG: 벡터 검색 + 순차 컨텍스트 확장
            cypher = """
                CALL db.index.vector.queryNodes('chunk_embeddings', $top_k, $embedding)
                YIELD node AS c, score
                
                // 순차 컨텍스트 확장
                OPTIONAL MATCH (prev:Chunk)-[:NEXT_CHUNK]->(c)
                OPTIONAL MATCH (c)-[:NEXT_CHUNK]->(next:Chunk)
                
                // 문서 정보
                MATCH (d:Document)-[:HAS_CHUNK]->(c)
                OPTIONAL MATCH (d)-[:BELONGS_TO]->(cat:Category)
                
                RETURN c.content, score, prev.content, next.content, d.title, cat.name
            """
        else:
            # 단순 벡터 검색
            cypher = """
                CALL db.index.vector.queryNodes('chunk_embeddings', $top_k, $embedding)
                YIELD node AS c, score
                MATCH (d:Document)-[:HAS_CHUNK]->(c)
                RETURN c.content, score, d.title
            """
        
        return session.run(cypher, embedding=query_embedding, top_k=top_k)
```

### 5.4 API 엔드포인트

| 엔드포인트 | 메서드 | 설명 |
|------------|--------|------|
| `/health` | GET | 헬스 체크 |
| `/api/chat` | POST | 채팅 요청 처리 |
| `/api/documents` | POST | 문서 추가 (RAG 인덱싱) |
| `/api/documents/<id>` | DELETE | 문서 삭제 |
| `/api/documents/stats` | GET | 컬렉션 통계 |
| `/api/documents/search` | POST | 문서 검색 |
| `/api/model/current` | GET | 현재 모델 정보 |
| `/api/model/change` | PUT | 모델 변경 |
| `/api/model/available` | GET | 사용 가능한 모델 목록 |

---

## 6. 데이터베이스 스키마

### 6.1 PostgreSQL 스키마 구조

```sql
-- 스키마 생성 (MSA 전환 대비)
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS project;
CREATE SCHEMA IF NOT EXISTS task;
CREATE SCHEMA IF NOT EXISTS chat;
CREATE SCHEMA IF NOT EXISTS risk;
CREATE SCHEMA IF NOT EXISTS report;

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- 텍스트 검색 최적화
```

### 6.2 ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AUTH SCHEMA                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌────────────────────┐                       │
│  │       users        │        │    permissions     │                       │
│  ├────────────────────┤        ├────────────────────┤                       │
│  │ id (PK)           │        │ id (PK)           │                       │
│  │ email (UNIQUE)    │        │ category          │                       │
│  │ password          │        │ name              │                       │
│  │ name              │        │ description       │                       │
│  │ role              │        └────────────────────┘                       │
│  │ department        │                 │                                    │
│  │ active            │                 │                                    │
│  │ last_login_at     │        ┌────────▼───────────┐                       │
│  │ created_at        │        │  role_permissions  │                       │
│  │ updated_at        │        ├────────────────────┤                       │
│  └────────────────────┘        │ id (PK)           │                       │
│                                │ role              │                       │
│                                │ permission_id (FK)│                       │
│                                │ granted           │                       │
│                                └────────────────────┘                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                             PROJECT SCHEMA                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐                                                     │
│  │     projects       │                                                     │
│  ├────────────────────┤                                                     │
│  │ id (PK)           │◄──────────────────────────────────────┐             │
│  │ name              │                                        │             │
│  │ description       │     ┌────────────────────┐            │             │
│  │ status            │     │      phases        │            │             │
│  │ start_date        │     ├────────────────────┤            │             │
│  │ end_date          │     │ id (PK)           │            │             │
│  │ budget            │     │ project_id (FK)───┼────────────┘             │
│  │ progress          │     │ name              │                           │
│  │ created_at        │     │ order_num         │◄────────────┐             │
│  │ updated_at        │     │ status            │             │             │
│  └────────────────────┘     │ gate_status       │             │             │
│           │                 │ start_date        │             │             │
│           │                 │ end_date          │             │             │
│           │                 │ progress          │             │             │
│           │                 │ track_type        │             │             │
│           │                 └────────────────────┘             │             │
│           │                                                    │             │
│           │     ┌────────────────────┐     ┌─────────────────┐│             │
│           │     │      issues        │     │  deliverables   ││             │
│           │     ├────────────────────┤     ├─────────────────┤│             │
│           └────►│ id (PK)           │     │ id (PK)         ││             │
│                 │ project_id (FK)   │     │ phase_id (FK)───┘│             │
│                 │ title             │     │ name             │             │
│                 │ description       │     │ type             │             │
│                 │ issue_type        │     │ status           │             │
│                 │ priority          │     │ file_path        │             │
│                 │ status            │     │ uploaded_by      │             │
│                 │ assignee          │     │ approved_by      │             │
│                 │ reporter          │     │ approved_at      │             │
│                 │ due_date          │     └─────────────────┘│             │
│                 │ resolved_at       │                         │             │
│                 │ resolution        │     ┌─────────────────┐│             │
│                 └────────────────────┘     │     meetings    ││             │
│                                           ├─────────────────┤│             │
│                                           │ id (PK)         ││             │
│                                           │ project_id (FK) ││             │
│                                           │ title           ││             │
│                                           │ date            ││             │
│                                           │ agenda          ││             │
│                                           │ minutes         ││             │
│                                           │ attendees       ││             │
│                                           └─────────────────┘│             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              TASK SCHEMA                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐     ┌────────────────────┐                         │
│  │  kanban_columns    │     │      sprints       │                         │
│  ├────────────────────┤     ├────────────────────┤                         │
│  │ id (PK)           │     │ id (PK)           │                         │
│  │ project_id        │     │ project_id        │                         │
│  │ name              │     │ name              │                         │
│  │ order_num         │     │ start_date        │                         │
│  │ wip_limit         │     │ end_date          │                         │
│  └─────────┬──────────┘     │ status            │                         │
│            │                │ goal              │                         │
│            │                └────────────────────┘                         │
│            │                                                               │
│  ┌─────────▼──────────┐     ┌────────────────────┐                         │
│  │       tasks        │     │    user_stories    │                         │
│  ├────────────────────┤     ├────────────────────┤                         │
│  │ id (PK)           │     │ id (PK)           │                         │
│  │ column_id (FK)    │     │ project_id        │                         │
│  │ phase_id          │     │ sprint_id (FK)    │                         │
│  │ title             │     │ epic              │                         │
│  │ description       │     │ title             │                         │
│  │ assignee_id       │     │ description       │                         │
│  │ priority          │     │ acceptance_criteria│                        │
│  │ status            │     │ story_points      │                         │
│  │ track_type        │     │ priority          │                         │
│  │ due_date          │     │ status            │                         │
│  │ order_num         │     │ order_num         │                         │
│  │ tags              │     └────────────────────┘                         │
│  │ created_at        │                                                     │
│  │ updated_at        │                                                     │
│  └────────────────────┘                                                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              CHAT SCHEMA                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐     ┌────────────────────┐                         │
│  │   chat_sessions    │     │   chat_messages    │                         │
│  ├────────────────────┤     ├────────────────────┤                         │
│  │ id (PK)           │◄────┤ id (PK)           │                         │
│  │ user_id           │     │ session_id (FK)   │                         │
│  │ title             │     │ role              │                         │
│  │ active            │     │ content           │                         │
│  │ created_at        │     │ created_at        │                         │
│  │ updated_at        │     │ updated_at        │                         │
│  └────────────────────┘     └────────────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Neo4j 그래프 스키마

```cypher
// 노드 타입
(:Document {doc_id, title, content, file_type, file_path, created_at})
(:Chunk {chunk_id, content, chunk_index, title, doc_id, structure_type, 
         has_table, has_list, section_title, page_number, embedding})
(:Category {name})

// 관계 타입
(:Document)-[:HAS_CHUNK]->(:Chunk)
(:Chunk)-[:NEXT_CHUNK]->(:Chunk)
(:Document)-[:BELONGS_TO]->(:Category)

// 벡터 인덱스
CREATE VECTOR INDEX chunk_embeddings
FOR (c:Chunk) ON c.embedding
OPTIONS {
    indexConfig: {
        `vector.dimensions`: 1024,
        `vector.similarity_function`: 'cosine'
    }
}

// 제약조건
CREATE CONSTRAINT FOR (d:Document) REQUIRE d.doc_id IS UNIQUE
CREATE CONSTRAINT FOR (c:Chunk) REQUIRE c.chunk_id IS UNIQUE
CREATE CONSTRAINT FOR (cat:Category) REQUIRE cat.name IS UNIQUE
```

---

## 7. API 설계

### 7.1 REST API 규칙

- **Base URL**: `http://localhost:8083/api`
- **인증**: `Authorization: Bearer <JWT_TOKEN>`
- **응답 형식**: JSON

### 7.2 주요 API 엔드포인트

#### 인증 API

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| POST | `/auth/login` | 로그인 |
| POST | `/auth/logout` | 로그아웃 |
| POST | `/auth/refresh` | 토큰 갱신 |
| GET | `/users` | 사용자 목록 |
| GET | `/permissions` | 권한 목록 |
| PUT | `/permissions/role` | 역할 권한 수정 |

#### 프로젝트 API

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/projects` | 프로젝트 목록 |
| POST | `/projects` | 프로젝트 생성 |
| GET | `/projects/{id}` | 프로젝트 상세 |
| PUT | `/projects/{id}` | 프로젝트 수정 |
| DELETE | `/projects/{id}` | 프로젝트 삭제 |
| GET | `/phases` | 페이즈 목록 |
| PUT | `/phases/{id}` | 페이즈 수정 |
| GET | `/phases/{id}/deliverables` | 산출물 목록 |
| POST | `/phases/{id}/deliverables` | 산출물 업로드 |

#### 태스크 API

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/tasks/columns` | 칸반 컬럼 조회 |
| POST | `/tasks` | 태스크 생성 |
| PUT | `/tasks/{id}` | 태스크 수정 |
| PUT | `/tasks/{id}/move` | 태스크 이동 |
| DELETE | `/tasks/{id}` | 태스크 삭제 |
| GET | `/stories` | 사용자 스토리 목록 |
| POST | `/stories` | 스토리 생성 |

#### 이슈 API

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/projects/{id}/issues` | 이슈 목록 |
| POST | `/projects/{id}/issues` | 이슈 생성 |
| PUT | `/projects/{id}/issues/{issueId}` | 이슈 수정 |
| PATCH | `/projects/{id}/issues/{issueId}/status` | 상태 변경 |
| DELETE | `/projects/{id}/issues/{issueId}` | 이슈 삭제 |

#### 채팅 API

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/chat/sessions` | 세션 목록 |
| POST | `/chat/sessions` | 세션 생성 |
| POST | `/chat/message` | 메시지 전송 |
| GET | `/chat/sessions/{id}/messages` | 메시지 조회 |
| PUT | `/chat/sessions/{id}/title` | 세션 제목 변경 |

#### Lineage API

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/lineage/graph/{projectId}` | 프로젝트 Lineage 그래프 조회 |
| GET | `/lineage/timeline/{projectId}` | 활동 타임라인 조회 (페이지네이션) |
| GET | `/lineage/statistics/{projectId}` | Lineage 통계 조회 |
| GET | `/lineage/history/{type}/{id}` | 엔티티 변경 이력 조회 |
| GET | `/lineage/upstream/{type}/{id}` | Upstream 의존성 조회 |
| GET | `/lineage/downstream/{type}/{id}` | Downstream 영향 범위 조회 |
| GET | `/lineage/impact/{type}/{id}` | 영향 분석 결과 조회 |

**Timeline 쿼리 파라미터:**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `aggregateType` | string | 필터: REQUIREMENT, USER_STORY, TASK, SPRINT |
| `since` | ISO 8601 | 시작 날짜 필터 |
| `until` | ISO 8601 | 종료 날짜 필터 |
| `userId` | string | 사용자 필터 |
| `page` | int | 페이지 번호 (0-based) |
| `size` | int | 페이지 크기 (기본: 20) |

### 7.3 응답 형식

**성공 응답**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "proj-001",
    "name": "보험 심사 시스템 구축",
    "status": "IN_PROGRESS"
  },
  "timestamp": "2026-01-16T10:30:00Z"
}
```

**오류 응답**

```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": [
      { "field": "email", "message": "Invalid email format" }
    ]
  },
  "timestamp": "2026-01-16T10:30:00Z"
}
```

---

## 8. 보안 아키텍처

### 8.1 JWT 인증 흐름

```
1. 로그인 요청
   Client → POST /api/auth/login {email, password}

2. 인증 처리
   Backend → BCrypt 비밀번호 검증

3. JWT 발급
   Backend → JWT 생성
   - Header: {alg: "HS256", typ: "JWT"}
   - Payload: {sub: user_id, roles: [...], exp: ...}
   - Signature: HMACSHA256(header + payload, secret)

4. 토큰 반환
   Backend → {accessToken, refreshToken}

5. 후속 요청
   Client → Authorization: Bearer {accessToken}

6. 토큰 검증
   JwtAuthenticationFilter → 서명/만료 검증 → SecurityContext 설정
```

### 8.2 역할 기반 접근 제어 (RBAC)

| 역할 | 설명 | 주요 권한 |
|------|------|----------|
| SPONSOR | 스폰서 | 예산 승인, 최종 의사결정 |
| PMO_HEAD | PMO 책임자 | 전체 프로젝트 관리, 보고서 조회 |
| PM | 프로젝트 관리자 | 프로젝트 생성/수정, WBS 관리 |
| DEVELOPER | 개발자 | 태스크 작업, 백로그 관리 |
| QA | 품질 보증 | 테스트, 이슈 등록 |
| BUSINESS_ANALYST | 비즈니스 분석가 | 요구사항 분석, 백로그 관리 |
| AUDITOR | 감사자 | 감사 로그 조회 |
| ADMIN | 시스템 관리자 | 사용자/권한 관리 |

### 8.3 환경변수 기반 시크릿 관리

```bash
# .env (git에서 제외)
POSTGRES_PASSWORD=secure_db_password
JWT_SECRET=long_random_256bit_key
NEO4J_PASSWORD=secure_neo4j_password
```

---

## 9. 데이터 계보 (Lineage) 아키텍처

### 9.1 개요

PMS-IC는 요구사항 추적성(Requirement Traceability)을 위해 **Outbox Pattern + Redis Streams** 기반의 데이터 계보 시스템을 구현합니다. 이를 통해 Requirement → UserStory → Task 간의 관계를 실시간으로 추적하고, OpenMetadata 및 Neo4j에 동기화합니다.

### 9.2 Lineage 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Backend                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        Service Layer (Same Transaction)                  │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │ │
│  │  │RequirementService│  │ UserStoryService │  │     TaskService      │  │ │
│  │  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘  │ │
│  │           │                     │                        │              │ │
│  │           └─────────────────────┼────────────────────────┘              │ │
│  │                                 │                                        │ │
│  │                                 ▼                                        │ │
│  │                    ┌────────────────────────┐                           │ │
│  │                    │  LineageEventProducer  │                           │ │
│  │                    │    (Transactional)     │                           │ │
│  │                    └───────────┬────────────┘                           │ │
│  └────────────────────────────────┼─────────────────────────────────────────┘ │
│                                   │ save()                                    │
│                                   ▼                                           │
│                    ┌────────────────────────────┐                            │
│                    │   PostgreSQL (outbox_events)│                            │
│                    │   ┌──────────────────────┐ │                            │
│                    │   │ id, event_type,      │ │                            │
│                    │   │ payload (JSONB),     │ │                            │
│                    │   │ status, retry_count  │ │                            │
│                    │   └──────────────────────┘ │                            │
│                    └─────────────┬──────────────┘                            │
└──────────────────────────────────┼───────────────────────────────────────────┘
                                   │ poll (5s interval)
                                   ▼
                    ┌────────────────────────────┐
                    │       OutboxPoller         │
                    │   (@Scheduled Component)   │
                    └───────────┬────────────────┘
                                │ XADD
                                ▼
                    ┌────────────────────────────┐
                    │      Redis Streams         │
                    │    (lineage:events)        │
                    │                            │
                    │  Consumer Groups:          │
                    │  • neo4j-sync             │
                    │  • openmetadata-sync      │
                    └───────────┬────────────────┘
                                │ XREADGROUP
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
        ┌──────────────────┐    ┌──────────────────┐
        │ Neo4j Consumer   │    │ OpenMetadata     │
        │ (Python)         │    │ Consumer (Python)│
        │                  │    │                  │
        │ • Create nodes   │    │ • Create tables  │
        │ • DERIVES rels   │    │ • Lineage edges  │
        │ • BREAKS_DOWN_TO │    │ • API calls      │
        └────────┬─────────┘    └────────┬─────────┘
                 │                       │
                 ▼                       ▼
        ┌──────────────────┐    ┌──────────────────┐
        │     Neo4j        │    │   OpenMetadata   │
        │  (Graph + RAG)   │    │   (Data Catalog) │
        └──────────────────┘    └──────────────────┘
```

### 9.3 Lineage 이벤트 타입

| 이벤트 타입 | 설명 | 페이로드 예시 |
|------------|------|---------------|
| `REQUIREMENT_CREATED` | 요구사항 생성 | `{id, projectId, code, title}` |
| `REQUIREMENT_UPDATED` | 요구사항 수정 | `{id, projectId, changes}` |
| `REQUIREMENT_DELETED` | 요구사항 삭제 | `{id, projectId}` |
| `REQUIREMENT_STORY_LINKED` | 요구사항-스토리 연결 | `{sourceId, targetId, relationshipType: "DERIVES"}` |
| `STORY_CREATED` | 사용자 스토리 생성 | `{id, projectId, title, storyPoints}` |
| `STORY_TASK_LINKED` | 스토리-태스크 연결 | `{sourceId, targetId, relationshipType: "BREAKS_DOWN_TO"}` |
| `TASK_CREATED` | 태스크 생성 | `{id, projectId, title, assigneeId}` |
| `TASK_STATUS_CHANGED` | 태스크 상태 변경 | `{id, oldStatus, newStatus}` |

### 9.4 Neo4j Lineage 스키마

```cypher
// Lineage Node Types
(:Requirement {id, code, title, projectId, status, category})
(:UserStory {id, title, projectId, status, storyPoints, epic})
(:Task {id, title, projectId, status, priority, assigneeId})

// Lineage Relationships
(:Requirement)-[:DERIVES]->(:UserStory)
(:UserStory)-[:BREAKS_DOWN_TO]->(:Task)
(:Requirement)-[:IMPLEMENTED_BY]->(:Task)

// Query: Trace requirement to all implementing tasks
MATCH path = (r:Requirement {id: $reqId})-[:DERIVES*..2]->()-[:BREAKS_DOWN_TO*..2]->(t:Task)
RETURN path
```

### 9.5 Outbox 테이블 스키마

```sql
CREATE TABLE IF NOT EXISTS project.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    last_error TEXT,
    idempotency_key VARCHAR(100) UNIQUE
);

-- Indexes for efficient polling
CREATE INDEX idx_outbox_status_created ON project.outbox_events(status, created_at);
CREATE INDEX idx_outbox_retry ON project.outbox_events(status, retry_count, created_at);
```

### 9.6 설정 (application.yml)

```yaml
lineage:
  outbox:
    batch-size: 100          # Events per poll
    max-retries: 3           # Max retry attempts
    poll-interval: 5000      # 5 seconds
    retry-interval: 60000    # 1 minute
    cleanup-cron: "0 0 2 * * ?"  # Daily at 2 AM
  stream:
    name: lineage:events     # Redis stream name
```

### 9.7 Consumer 실행

```bash
# Start lineage consumers (via docker-compose)
docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml up -d lineage-consumer

# Or run manually
cd openmetadata/ingestion
python lineage_consumer.py --consumer neo4j --group neo4j-sync
python lineage_consumer.py --consumer openmetadata --group openmetadata-sync
```

---

## 10. 배포 및 인프라

### 10.1 Docker Compose 서비스

| 서비스 | 이미지 | 포트 | 역할 |
|--------|--------|------|------|
| postgres | postgres:15-alpine | 5433 | 관계형 데이터베이스 |
| redis | redis:7-alpine | 6379 | 캐시/세션/Streams |
| neo4j | neo4j:5.20-community | 7474, 7687 | 그래프/벡터 DB |
| backend | Spring Boot | 8083 | API 서버 |
| frontend | Vite Dev Server | 5173 | 웹 UI |
| llm-service | Flask + llama-cpp | 8000 | AI 서비스 |
| lineage-consumer | Python | - | Lineage 동기화 |
| openmetadata | openmetadata/server | 8585, 8586 | 데이터 카탈로그 |
| openmetadata-elasticsearch | elasticsearch | 9200 | 검색 엔진 |
| pgadmin | dpage/pgadmin4 | 5050 | DB 관리 도구 |
| redis-commander | rediscommander | 8082 | Redis 관리 도구 |

### 10.2 실행 명령어

```bash
# 전체 서비스 시작
docker-compose up -d

# 특정 서비스만 시작
docker-compose up -d backend frontend

# 로그 확인
docker-compose logs -f backend

# 서비스 중지
docker-compose down

# 볼륨 포함 완전 삭제
docker-compose down -v
```

### 10.3 헬스체크

| 서비스 | 헬스체크 엔드포인트 |
|--------|---------------------|
| Backend | `/actuator/health` |
| LLM Service | `/health` |
| PostgreSQL | `pg_isready` |
| Redis | `redis-cli ping` |
| Neo4j | `http://localhost:7474` |
| OpenMetadata | `/api/v1/system/version` |

### 10.4 볼륨 구성

| 볼륨 | 용도 |
|------|------|
| postgres_data | PostgreSQL 데이터 |
| redis_data | Redis 데이터 |
| neo4j_data | Neo4j 데이터 |
| neo4j_logs | Neo4j 로그 |
| backend_cache | Maven 캐시 |

---

## 부록

### A. 환경 변수 목록

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| SPRING_PROFILES_ACTIVE | Spring 프로파일 | dev |
| POSTGRES_DB | 데이터베이스 이름 | pms_db |
| POSTGRES_USER | DB 사용자 | pms_user |
| POSTGRES_PASSWORD | DB 비밀번호 | pms_password |
| JWT_SECRET | JWT 서명 키 | - |
| AI_SERVICE_URL | LLM 서비스 URL | http://llm-service:8000 |
| MODEL_PATH | LLM 모델 경로 | ./models/google.gemma-3-12b-pt.Q5_K_M.gguf |
| NEO4J_URI | Neo4j 연결 URI | bolt://neo4j:7687 |
| NEO4J_USER | Neo4j 사용자 | neo4j |
| NEO4J_PASSWORD | Neo4j 비밀번호 | pmspassword123 |
| OPENMETADATA_URL | OpenMetadata API URL | `http://openmetadata:8585` |
| OM_JWT_TOKEN | OpenMetadata JWT 토큰 | - |

### B. 포트 매핑

| 포트 | 서비스 | 프로토콜 |
|------|--------|----------|
| 5173 | Frontend | HTTP |
| 8083 | Backend | HTTP |
| 8000 | LLM Service | HTTP |
| 5433 | PostgreSQL | TCP |
| 6379 | Redis | TCP |
| 7474 | Neo4j Browser | HTTP |
| 7687 | Neo4j Bolt | TCP |
| 5050 | PgAdmin | HTTP |
| 8082 | Redis Commander | HTTP |
| 8585 | OpenMetadata | HTTP |
| 8586 | OpenMetadata Admin | HTTP |
| 9200 | Elasticsearch | HTTP |

### C. 기술 결정 사항

| 항목 | 선택 | 이유 |
|------|------|------|
| Backend | Spring Boot 3.2 | 엔터프라이즈급 안정성, 풍부한 생태계 |
| Frontend | React 18 + Vite | 빠른 개발, 컴포넌트 기반 |
| LLM | Gemma 3 12B | 로컬 배포 가능, 한국어 지원 우수 |
| RAG | Neo4j GraphRAG | 벡터 + 그래프 하이브리드 검색 |
| Database | PostgreSQL 15 | ACID 보장, JSON 지원, 스키마 분리 |
| Cache/Streams | Redis 7 | 고성능, 세션 관리, Streams 지원 |
| Embedding | multilingual-e5-large | 다국어 지원, 1024차원 |
| Workflow | LangGraph | 상태 기반 워크플로우, 유연한 라우팅 |
| Event Streaming | Redis Streams | 경량, 기존 인프라 활용, Outbox 패턴 |
| Data Catalog | OpenMetadata | 오픈소스, 데이터 계보 시각화 |

---

**문서 끝**
