# AI 기반 PMS RFP 관리 기능 설계서

## 문서 정보
| 항목 | 내용 |
|------|------|
| 작성일 | 2026-01-14 |
| 버전 | 1.0 |
| 상태 | Draft |

---

## 1. 개요

### 1.1 목적
본 문서는 기존 AI 기반 프로젝트 관리 시스템(PMS)에 RFP(Request for Proposal) 관리 기능을 추가하기 위한 기획 및 설계서입니다.

### 1.2 핵심 기능
1. **RFP 업로드 및 Graph RAG 기반 요구사항 자동 추출**
2. **요구사항 ID 자동 부여 및 스프린트 매핑**
3. **요구사항 진행률 및 상태 실시간 업데이트**
4. **프로젝트별 테넌트 관리 (PMS 및 Graph RAG DB)**
5. **담당자별 AI 보고서 생성 (주간보고 자동 생성)**
6. **보고서 버전 관리**

### 1.3 기존 시스템 분석

현재 PMS-IC 프로젝트는 다음과 같은 구조를 가지고 있습니다:

```
기술 스택:
├── Backend: Spring Boot 3.2.1, Java 17
├── Frontend: React + TypeScript, Vite
├── LLM Service: Python Flask, Llama (Gemma 3 12B)
├── Primary DB: PostgreSQL
├── Graph DB: Neo4j (GraphRAG)
└── Cache: Redis
```

**기존 도메인 모델:**
```
Project → Phase → Deliverable
       ↘ Sprint → UserStory → Task
       ↘ KPI
```

**새로운 RFP 도메인 통합:**
```
Project → RFP → Requirement → Sprint/Task 매핑
       ↘ WeeklyReport (담당자별)
```

---

## 2. 기능 요구사항

### 2.1 RFP 관리 기능

#### 2.1.1 RFP 업로드
| 항목 | 설명 |
|------|------|
| 지원 형식 | PDF, DOCX, TXT, MD, XLSX |
| 최대 파일 크기 | 50MB |
| 처리 방식 | 비동기 처리 (백그라운드 작업) |
| 저장소 | 파일시스템 + S3 (추후) |

**업로드 프로세스:**
```
1. 사용자 파일 업로드
2. 파일 유효성 검사
3. 파일 저장 (로컬/S3)
4. Graph RAG 파이프라인 트리거
5. 요구사항 자동 추출
6. 요구사항 ID 자동 부여
7. 사용자에게 결과 알림
```

#### 2.1.2 요구사항 자동 추출
- Graph RAG 엔진이 RFP 문서를 분석하여 요구사항 자동 식별
- 추출된 요구사항은 편집 가능
- 요구사항 간 의존성/관계 자동 매핑

#### 2.1.3 요구사항 ID 체계
```
REQ-{프로젝트코드}-{카테고리}-{순번}

예시:
- REQ-PMS-FUNC-001 (기능 요구사항)
- REQ-PMS-NFUNC-001 (비기능 요구사항)
- REQ-PMS-UI-001 (UI 요구사항)
```

### 2.2 요구사항-스프린트 매핑

#### 2.2.1 매핑 기능
- 스프린트 생성/편집 시 요구사항 목록 표시
- 드래그-앤-드롭 또는 체크박스로 매핑
- 하나의 요구사항 → 여러 스프린트 매핑 가능
- 매핑 시 Graph DB에 엣지(관계) 추가

#### 2.2.2 진행률 자동 계산
```
요구사항 진행률 = (완료된 매핑 태스크 수 / 전체 매핑 태스크 수) × 100%
```

**상태 정의:**
| 상태 | 조건 | 표시 |
|------|------|------|
| NOT_STARTED | 진행률 0% | 회색 |
| IN_PROGRESS | 0% < 진행률 < 100% | 파란색 |
| COMPLETED | 진행률 100% | 녹색 |
| DELAYED | 기한 초과 + 미완료 | 빨간색 |

### 2.3 테넌트 관리

#### 2.3.1 PMS 데이터베이스 테넌트
```sql
-- PostgreSQL: 프로젝트 ID 기반 데이터 격리
WHERE project_id = :currentProjectId
```

#### 2.3.2 Graph RAG DB 테넌트
```cypher
-- Neo4j: Namespace 기반 테넌트 분리
MATCH (n:Requirement {tenant_id: $tenantId})
RETURN n
```

**테넌트 격리 방식:**
- **DB 레벨**: `tenant_id` 컬럼으로 논리적 분리
- **쿼리 레벨**: 모든 쿼리에 테넌트 필터 적용
- **API 레벨**: JWT 토큰에서 테넌트 정보 추출

### 2.4 AI 보고서 생성

#### 2.4.1 주간보고 자동 생성
- 담당자별 맞춤 주간보고 생성
- 버튼 클릭으로 On-Demand 생성
- PDF/텍스트 형식 지원

**보고서 포함 내용:**
```
1. 이번 주 완료 항목
2. 진행 중 항목 및 진행률
3. 다음 주 계획
4. 이슈 및 리스크
5. 요구사항 추적 현황
```

#### 2.4.2 보고서 버전 관리
- 생성된 보고서 히스토리 저장
- 버전별 비교 기능
- 보고서 승인 워크플로우 (선택적)

---

## 3. 시스템 아키텍처

### 3.1 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │RFP Upload│ │Requirement│ │ Sprint   │ │  Weekly Report   │   │
│  │  Page    │ │  Board    │ │ Mapping  │ │   Generator      │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘   │
└───────┼────────────┼────────────┼────────────────┼──────────────┘
        │            │            │                │
        ▼            ▼            ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │   RFP    │ │Requirement│ │ Mapping  │ │     Report       │   │
│  │ Service  │ │  Service  │ │ Service  │ │     Service      │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘   │
│       │            │            │                │              │
│       ▼            ▼            ▼                ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              RAG Service Layer (통합)                    │   │
│  └──────────────────────────┬──────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│  PostgreSQL  │    │    Neo4j     │    │  LLM Service     │
│  (Primary)   │    │ (Graph RAG)  │    │  (Python Flask)  │
│              │    │              │    │                  │
│ - RFP        │    │ - Requirement│    │ - 요구사항 추출  │
│ - Requirement│    │   Nodes      │    │ - 보고서 생성    │
│ - Report     │    │ - Relations  │    │ - RAG 처리       │
│ - Mapping    │    │ - Embeddings │    │                  │
└──────────────┘    └──────────────┘    └──────────────────┘
```

### 3.2 데이터 흐름

```
[RFP 업로드 흐름]
User → Upload RFP → Backend API → File Storage
                                → LLM Service (요구사항 추출)
                                → Neo4j (Graph 저장)
                                → PostgreSQL (메타데이터 저장)
                                → User (결과 알림)

[보고서 생성 흐름]
User → Generate Report → Backend API
                      → Neo4j Query (담당자 데이터)
                      → PostgreSQL Query (태스크 진행률)
                      → LLM Service (보고서 생성)
                      → PostgreSQL (보고서 저장)
                      → User (보고서 다운로드)
```

---

## 4. 데이터베이스 설계

### 4.1 PostgreSQL 스키마

#### 4.1.1 RFP 테이블
```sql
CREATE TABLE project.rfps (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(36) NOT NULL REFERENCES project.projects(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    file_type VARCHAR(50),
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'UPLOADED',
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),

    CONSTRAINT fk_rfp_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE
);

-- Status: UPLOADED, PROCESSING, COMPLETED, FAILED
-- Processing Status: PENDING, EXTRACTING, INDEXING, COMPLETED, FAILED
```

#### 4.1.2 요구사항 테이블
```sql
CREATE TABLE project.requirements (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    rfp_id VARCHAR(36) REFERENCES project.rfps(id),
    project_id VARCHAR(36) NOT NULL REFERENCES project.projects(id),
    requirement_code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(50) DEFAULT 'FUNCTIONAL',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'NOT_STARTED',
    progress INTEGER DEFAULT 0,
    source_text TEXT,
    page_number INTEGER,
    assignee_id VARCHAR(36) REFERENCES auth.users(id),
    due_date DATE,
    tenant_id VARCHAR(36) NOT NULL,
    neo4j_node_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),

    CONSTRAINT fk_req_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE
);

-- Category: FUNCTIONAL, NON_FUNCTIONAL, UI, INTEGRATION, SECURITY
-- Priority: CRITICAL, HIGH, MEDIUM, LOW
-- Status: NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED, CANCELLED
```

#### 4.1.3 요구사항-스프린트 매핑 테이블
```sql
CREATE TABLE project.requirement_sprint_mapping (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id VARCHAR(36) NOT NULL REFERENCES project.requirements(id),
    sprint_id VARCHAR(36) NOT NULL REFERENCES task.sprints(id),
    mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mapped_by VARCHAR(36),

    CONSTRAINT uk_req_sprint UNIQUE (requirement_id, sprint_id)
);
```

#### 4.1.4 요구사항-태스크 매핑 테이블
```sql
CREATE TABLE project.requirement_task_mapping (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id VARCHAR(36) NOT NULL REFERENCES project.requirements(id),
    task_id VARCHAR(36) NOT NULL REFERENCES task.tasks(id),
    contribution_weight DECIMAL(3,2) DEFAULT 1.0,
    mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mapped_by VARCHAR(36),

    CONSTRAINT uk_req_task UNIQUE (requirement_id, task_id)
);
```

#### 4.1.5 AI 보고서 테이블
```sql
CREATE TABLE report.weekly_reports (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(36) NOT NULL REFERENCES project.projects(id),
    assignee_id VARCHAR(36) REFERENCES auth.users(id),
    report_type VARCHAR(50) DEFAULT 'WEEKLY',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    report_period_start DATE,
    report_period_end DATE,
    version INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'DRAFT',
    format VARCHAR(20) DEFAULT 'TEXT',
    file_path VARCHAR(500),
    tenant_id VARCHAR(36) NOT NULL,
    generated_by_ai BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    approved_by VARCHAR(36),
    approved_at TIMESTAMP
);

-- Report Type: WEEKLY, MONTHLY, SPRINT, CUSTOM
-- Status: DRAFT, PENDING_APPROVAL, APPROVED, REJECTED
-- Format: TEXT, PDF, HTML
```

#### 4.1.6 보고서 버전 히스토리 테이블
```sql
CREATE TABLE report.report_versions (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id VARCHAR(36) NOT NULL REFERENCES report.weekly_reports(id),
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    change_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),

    CONSTRAINT uk_report_version UNIQUE (report_id, version)
);
```

### 4.2 Neo4j 그래프 스키마

#### 4.2.1 노드 타입
```cypher
// RFP 노드
(:RFP {
    id: String,
    project_id: String,
    tenant_id: String,
    name: String,
    embedding: List<Float>
})

// 요구사항 노드
(:Requirement {
    id: String,
    code: String,
    title: String,
    description: String,
    category: String,
    priority: String,
    status: String,
    progress: Integer,
    tenant_id: String,
    embedding: List<Float>
})

// 스프린트 노드 (동기화)
(:Sprint {
    id: String,
    project_id: String,
    name: String,
    tenant_id: String
})

// 태스크 노드 (동기화)
(:Task {
    id: String,
    sprint_id: String,
    title: String,
    status: String,
    tenant_id: String
})
```

#### 4.2.2 관계 타입
```cypher
// RFP → 요구사항 관계
(:RFP)-[:CONTAINS]->(:Requirement)

// 요구사항 간 의존성
(:Requirement)-[:DEPENDS_ON]->(:Requirement)

// 요구사항 → 스프린트 매핑
(:Requirement)-[:MAPPED_TO {mapped_at: DateTime}]->(:Sprint)

// 요구사항 → 태스크 매핑
(:Requirement)-[:IMPLEMENTED_BY {contribution: Float}]->(:Task)

// 요구사항 간 유사도 관계
(:Requirement)-[:SIMILAR_TO {score: Float}]->(:Requirement)
```

---

## 5. API 설계

### 5.1 RFP API

```yaml
# RFP 업로드
POST /api/rfp/upload
  Headers:
    Authorization: Bearer {token}
    Content-Type: multipart/form-data
  Body:
    file: File
    projectId: String
    name: String (optional)
    description: String (optional)
  Response:
    201 Created:
      id: String
      status: "PROCESSING"
      message: "RFP 업로드 완료. 요구사항 추출 중..."

# RFP 목록 조회
GET /api/rfp?projectId={projectId}
  Response:
    200 OK:
      data: [RfpDto]

# RFP 상세 조회
GET /api/rfp/{id}
  Response:
    200 OK:
      data: RfpDetailDto (요구사항 목록 포함)

# RFP 삭제
DELETE /api/rfp/{id}
  Response:
    204 No Content

# RFP 처리 상태 조회
GET /api/rfp/{id}/status
  Response:
    200 OK:
      status: "COMPLETED"
      requirementCount: 15
      processingTime: "2m 30s"
```

### 5.2 요구사항 API

```yaml
# 요구사항 목록 조회
GET /api/requirements?projectId={projectId}&status={status}
  Response:
    200 OK:
      data: [RequirementDto]
      totalCount: Integer
      statusSummary: {NOT_STARTED: 5, IN_PROGRESS: 8, COMPLETED: 2}

# 요구사항 상세 조회
GET /api/requirements/{id}
  Response:
    200 OK:
      data: RequirementDetailDto (매핑된 스프린트/태스크 포함)

# 요구사항 수정
PUT /api/requirements/{id}
  Body:
    title: String
    description: String
    category: String
    priority: String
    assigneeId: String
    dueDate: Date
  Response:
    200 OK

# 요구사항 삭제
DELETE /api/requirements/{id}
  Response:
    204 No Content

# 요구사항 수동 추가 (RFP 없이)
POST /api/requirements
  Body:
    projectId: String
    title: String
    description: String
    category: String
    priority: String
  Response:
    201 Created

# 요구사항 일괄 업데이트
PATCH /api/requirements/bulk
  Body:
    ids: [String]
    updates: {status?: String, assigneeId?: String}
  Response:
    200 OK
```

### 5.3 매핑 API

```yaml
# 요구사항-스프린트 매핑
POST /api/mapping/requirement-sprint
  Body:
    requirementId: String
    sprintId: String
  Response:
    201 Created

# 요구사항-스프린트 매핑 해제
DELETE /api/mapping/requirement-sprint
  Body:
    requirementId: String
    sprintId: String
  Response:
    204 No Content

# 요구사항-태스크 매핑
POST /api/mapping/requirement-task
  Body:
    requirementId: String
    taskId: String
    contributionWeight: Float (optional, default 1.0)
  Response:
    201 Created

# 스프린트별 매핑된 요구사항 조회
GET /api/mapping/sprint/{sprintId}/requirements
  Response:
    200 OK:
      data: [RequirementDto]

# 요구사항별 매핑된 태스크 조회
GET /api/mapping/requirement/{requirementId}/tasks
  Response:
    200 OK:
      data: [TaskDto]
```

### 5.4 보고서 API

```yaml
# AI 주간보고 생성
POST /api/reports/weekly/generate
  Body:
    projectId: String
    assigneeId: String (optional, 전체 or 특정 담당자)
    periodStart: Date
    periodEnd: Date
    format: "TEXT" | "PDF"
  Response:
    201 Created:
      id: String
      status: "GENERATING"

# 보고서 목록 조회
GET /api/reports?projectId={projectId}&type={type}&assigneeId={assigneeId}
  Response:
    200 OK:
      data: [ReportDto]

# 보고서 상세 조회
GET /api/reports/{id}
  Response:
    200 OK:
      data: ReportDetailDto

# 보고서 다운로드
GET /api/reports/{id}/download
  Response:
    200 OK:
      Content-Type: application/pdf | text/plain
      Content-Disposition: attachment

# 보고서 버전 히스토리 조회
GET /api/reports/{id}/versions
  Response:
    200 OK:
      data: [ReportVersionDto]

# 보고서 승인
POST /api/reports/{id}/approve
  Body:
    comment: String (optional)
  Response:
    200 OK

# 보고서 재생성 (새 버전)
POST /api/reports/{id}/regenerate
  Response:
    201 Created:
      newVersion: Integer
```

---

## 6. 상세 컴포넌트 설계

### 6.1 Backend 컴포넌트

#### 6.1.1 Entity 클래스

```java
// RFP Entity
@Entity
@Table(name = "rfps", schema = "project")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Rfp extends BaseEntity {
    @Id
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    private RfpStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    private ProcessingStatus processingStatus;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @OneToMany(mappedBy = "rfp", cascade = CascadeType.ALL)
    private List<Requirement> requirements = new ArrayList<>();
}

// Requirement Entity
@Entity
@Table(name = "requirements", schema = "project")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Requirement extends BaseEntity {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfp_id")
    private Rfp rfp;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "requirement_code", unique = true, nullable = false)
    private String requirementCode;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private RequirementCategory category;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private RequirementStatus status;

    @Column
    private Integer progress;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "assignee_id")
    private String assigneeId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "neo4j_node_id")
    private String neo4jNodeId;

    @ManyToMany
    @JoinTable(
        name = "requirement_sprint_mapping",
        schema = "project",
        joinColumns = @JoinColumn(name = "requirement_id"),
        inverseJoinColumns = @JoinColumn(name = "sprint_id")
    )
    private Set<Sprint> sprints = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "requirement_task_mapping",
        schema = "project",
        joinColumns = @JoinColumn(name = "requirement_id"),
        inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private Set<Task> tasks = new HashSet<>();
}
```

#### 6.1.2 Service 구조

```java
// RFP Service Interface
public interface RfpService {
    RfpDto uploadRfp(MultipartFile file, String projectId, String name, String description);
    List<RfpDto> getRfpsByProject(String projectId);
    RfpDetailDto getRfpById(String id);
    void deleteRfp(String id);
    ProcessingStatusDto getProcessingStatus(String id);
}

// Requirement Service Interface
public interface RequirementService {
    List<RequirementDto> getRequirementsByProject(String projectId, RequirementStatus status);
    RequirementDetailDto getRequirementById(String id);
    RequirementDto createRequirement(CreateRequirementRequest request);
    RequirementDto updateRequirement(String id, UpdateRequirementRequest request);
    void deleteRequirement(String id);
    void bulkUpdateRequirements(List<String> ids, BulkUpdateRequest updates);
    void updateRequirementProgress(String id);
}

// Mapping Service Interface
public interface RequirementMappingService {
    void mapRequirementToSprint(String requirementId, String sprintId);
    void unmapRequirementFromSprint(String requirementId, String sprintId);
    void mapRequirementToTask(String requirementId, String taskId, Float weight);
    List<RequirementDto> getRequirementsBySprint(String sprintId);
    List<TaskDto> getTasksByRequirement(String requirementId);
}

// Report Service Interface
public interface WeeklyReportService {
    ReportDto generateWeeklyReport(GenerateReportRequest request);
    List<ReportDto> getReports(String projectId, String type, String assigneeId);
    ReportDetailDto getReportById(String id);
    byte[] downloadReport(String id);
    List<ReportVersionDto> getReportVersions(String id);
    void approveReport(String id, String comment);
    ReportDto regenerateReport(String id);
}
```

### 6.2 Frontend 컴포넌트

#### 6.2.1 페이지 구조

```
src/app/components/
├── rfp/
│   ├── RfpUpload.tsx           # RFP 파일 업로드 컴포넌트
│   ├── RfpList.tsx             # RFP 목록 페이지
│   ├── RfpDetail.tsx           # RFP 상세 (요구사항 목록)
│   └── RfpProcessingStatus.tsx # 처리 상태 표시
│
├── requirement/
│   ├── RequirementBoard.tsx    # 요구사항 관리 보드 (칸반 스타일)
│   ├── RequirementCard.tsx     # 요구사항 카드 컴포넌트
│   ├── RequirementForm.tsx     # 요구사항 추가/편집 폼
│   ├── RequirementDetail.tsx   # 요구사항 상세 모달
│   └── RequirementMapping.tsx  # 스프린트/태스크 매핑 UI
│
├── report/
│   ├── ReportGenerator.tsx     # AI 보고서 생성 버튼/폼
│   ├── ReportList.tsx          # 보고서 목록
│   ├── ReportViewer.tsx        # 보고서 뷰어
│   └── ReportVersionHistory.tsx # 버전 히스토리
│
└── shared/
    ├── ProgressIndicator.tsx   # 진행률 표시 컴포넌트
    ├── StatusBadge.tsx         # 상태 뱃지
    └── MappingDropzone.tsx     # 드래그앤드롭 매핑 영역
```

#### 6.2.2 주요 UI 화면

**RFP 업로드 화면:**
```
┌─────────────────────────────────────────────────────────┐
│  RFP 관리                                      [+ 업로드] │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐   │
│  │                                                  │   │
│  │     📄 파일을 드래그하거나 클릭하여 업로드       │   │
│  │        PDF, DOCX, TXT, MD 지원                  │   │
│  │                                                  │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  업로드된 RFP 목록                                       │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 📋 PMS_요구사항_v2.pdf    ✅ 완료    15개 요구사항 │  │
│  │ 📋 추가_기능_스펙.docx   🔄 처리중   --           │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**요구사항 보드 화면:**
```
┌─────────────────────────────────────────────────────────────────┐
│  요구사항 관리           필터: [전체 ▼] [높음 ▼]    🔍 검색     │
├─────────────────────────────────────────────────────────────────┤
│  미시작 (5)      │   진행중 (8)       │   완료 (2)             │
│  ───────────     │   ────────────     │   ──────────           │
│  ┌──────────┐   │   ┌──────────┐    │   ┌──────────┐         │
│  │REQ-001   │   │   │REQ-003   │    │   │REQ-002   │         │
│  │로그인 기능│   │   │대시보드  │    │   │회원가입  │         │
│  │🔴 높음   │   │   │🟡 중간   │    │   │✅ 100%  │         │
│  │ ░░░░░ 0%│   │   │████░ 60% │    │   │█████    │         │
│  └──────────┘   │   └──────────┘    │   └──────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

**보고서 생성 화면:**
```
┌─────────────────────────────────────────────────────────┐
│  AI 주간보고 생성                                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  프로젝트: [PMS 개발 프로젝트 ▼]                        │
│                                                         │
│  담당자:   [○ 전체  ● 특정 담당자 ▼]                   │
│                                                         │
│  기간:     2026-01-06 ~ 2026-01-12                     │
│                                                         │
│  형식:     [● TEXT  ○ PDF]                             │
│                                                         │
│            [    🤖 AI 보고서 생성    ]                  │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  최근 생성된 보고서                                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 📄 주간보고_Week2_김개발.txt   v3   2026-01-12   │  │
│  │ 📄 주간보고_Week2_이기획.pdf   v1   2026-01-12   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 7. Graph RAG 연동 설계

### 7.1 요구사항 추출 파이프라인

```python
# LLM Service - requirement_extractor.py

class RequirementExtractor:
    def __init__(self, llm_model, neo4j_service):
        self.llm = llm_model
        self.neo4j = neo4j_service

    async def extract_requirements(self, document_text: str, rfp_id: str, tenant_id: str):
        """
        RFP 문서에서 요구사항 추출
        """
        # 1. 문서 청킹
        chunks = self.chunk_document(document_text)

        # 2. LLM을 통한 요구사항 추출
        extraction_prompt = """
        다음 문서에서 요구사항을 추출해주세요.
        각 요구사항에 대해 다음 정보를 JSON 형식으로 제공해주세요:
        - title: 요구사항 제목
        - description: 상세 설명
        - category: FUNCTIONAL/NON_FUNCTIONAL/UI/INTEGRATION/SECURITY
        - priority: CRITICAL/HIGH/MEDIUM/LOW (추정)
        - source_text: 원본 텍스트

        문서 내용:
        {chunk}
        """

        all_requirements = []
        for chunk in chunks:
            result = await self.llm.generate(
                extraction_prompt.format(chunk=chunk)
            )
            requirements = self.parse_requirements(result)
            all_requirements.extend(requirements)

        # 3. 중복 제거 및 병합
        merged_requirements = self.merge_duplicates(all_requirements)

        # 4. Neo4j에 저장
        for req in merged_requirements:
            await self.neo4j.create_requirement_node(req, rfp_id, tenant_id)

        # 5. 요구사항 간 관계 분석 및 저장
        await self.analyze_dependencies(merged_requirements, tenant_id)

        return merged_requirements

    async def analyze_dependencies(self, requirements, tenant_id):
        """
        요구사항 간 의존성 분석 및 관계 생성
        """
        dependency_prompt = """
        다음 요구사항들 간의 의존성을 분석해주세요.
        어떤 요구사항이 다른 요구사항에 의존하는지 파악해주세요.

        요구사항 목록:
        {requirements}

        JSON 형식으로 의존성 관계를 출력해주세요:
        [{"from": "REQ-001", "to": "REQ-002", "type": "DEPENDS_ON"}]
        """

        result = await self.llm.generate(
            dependency_prompt.format(requirements=json.dumps(requirements))
        )

        dependencies = json.loads(result)
        for dep in dependencies:
            await self.neo4j.create_dependency_edge(
                dep['from'], dep['to'], dep['type'], tenant_id
            )
```

### 7.2 Neo4j 서비스 확장

```python
# LLM Service - rag_service_neo4j.py 확장

class RAGServiceNeo4j:
    # ... 기존 코드 ...

    async def create_requirement_node(self, requirement: dict, rfp_id: str, tenant_id: str):
        """
        요구사항 노드 생성
        """
        # 임베딩 생성
        embedding = await self.embed_text(
            f"{requirement['title']} {requirement['description']}"
        )

        query = """
        CREATE (r:Requirement {
            id: $id,
            code: $code,
            title: $title,
            description: $description,
            category: $category,
            priority: $priority,
            status: 'NOT_STARTED',
            progress: 0,
            source_text: $source_text,
            tenant_id: $tenant_id,
            embedding: $embedding
        })
        WITH r
        MATCH (rfp:RFP {id: $rfp_id, tenant_id: $tenant_id})
        CREATE (rfp)-[:CONTAINS]->(r)
        RETURN r
        """

        return await self.execute_query(query, {
            'id': requirement['id'],
            'code': requirement['code'],
            'title': requirement['title'],
            'description': requirement['description'],
            'category': requirement['category'],
            'priority': requirement['priority'],
            'source_text': requirement.get('source_text', ''),
            'rfp_id': rfp_id,
            'tenant_id': tenant_id,
            'embedding': embedding
        })

    async def map_requirement_to_sprint(self, req_id: str, sprint_id: str, tenant_id: str):
        """
        요구사항-스프린트 매핑
        """
        query = """
        MATCH (r:Requirement {id: $req_id, tenant_id: $tenant_id})
        MATCH (s:Sprint {id: $sprint_id, tenant_id: $tenant_id})
        MERGE (r)-[m:MAPPED_TO]->(s)
        SET m.mapped_at = datetime()
        RETURN r, m, s
        """
        return await self.execute_query(query, {
            'req_id': req_id,
            'sprint_id': sprint_id,
            'tenant_id': tenant_id
        })

    async def get_requirement_progress_data(self, req_id: str, tenant_id: str):
        """
        요구사항 진행률 계산을 위한 데이터 조회
        """
        query = """
        MATCH (r:Requirement {id: $req_id, tenant_id: $tenant_id})
        OPTIONAL MATCH (r)-[:IMPLEMENTED_BY]->(t:Task)
        WITH r, collect(t) as tasks
        RETURN r.id as requirement_id,
               size(tasks) as total_tasks,
               size([t IN tasks WHERE t.status = 'DONE']) as completed_tasks
        """
        return await self.execute_query(query, {
            'req_id': req_id,
            'tenant_id': tenant_id
        })

    async def get_assignee_weekly_data(self, assignee_id: str, tenant_id: str,
                                       start_date: str, end_date: str):
        """
        담당자별 주간 데이터 조회 (보고서 생성용)
        """
        query = """
        MATCH (r:Requirement {assignee_id: $assignee_id, tenant_id: $tenant_id})
        OPTIONAL MATCH (r)-[:IMPLEMENTED_BY]->(t:Task)
        WHERE t.updated_at >= datetime($start_date)
          AND t.updated_at <= datetime($end_date)
        WITH r, collect(t) as tasks,
             size([t IN collect(t) WHERE t.status = 'DONE']) as completed_count
        RETURN {
            requirement_id: r.id,
            requirement_code: r.code,
            requirement_title: r.title,
            requirement_status: r.status,
            progress: r.progress,
            total_tasks: size(tasks),
            completed_tasks: completed_count,
            tasks: [t IN tasks | {
                id: t.id,
                title: t.title,
                status: t.status
            }]
        } as weekly_data
        """
        return await self.execute_query(query, {
            'assignee_id': assignee_id,
            'tenant_id': tenant_id,
            'start_date': start_date,
            'end_date': end_date
        })
```

### 7.3 보고서 생성 서비스

```python
# LLM Service - report_generator.py

class WeeklyReportGenerator:
    def __init__(self, llm_model, neo4j_service):
        self.llm = llm_model
        self.neo4j = neo4j_service

    async def generate_weekly_report(self, assignee_id: str, tenant_id: str,
                                     period_start: str, period_end: str) -> dict:
        """
        AI 주간보고 생성
        """
        # 1. Neo4j에서 주간 데이터 조회
        weekly_data = await self.neo4j.get_assignee_weekly_data(
            assignee_id, tenant_id, period_start, period_end
        )

        # 2. 사용자 정보 조회
        user_info = await self.get_user_info(assignee_id)

        # 3. LLM을 통한 보고서 생성
        report_prompt = f"""
        다음 데이터를 바탕으로 주간 업무 보고서를 작성해주세요.

        담당자: {user_info['name']}
        보고 기간: {period_start} ~ {period_end}

        주간 실적 데이터:
        {json.dumps(weekly_data, ensure_ascii=False, indent=2)}

        보고서 형식:
        1. 금주 완료 사항
           - 완료된 요구사항과 태스크를 구체적으로 나열

        2. 진행 중인 업무
           - 현재 진행 중인 요구사항과 진행률
           - 각 항목의 예상 완료 시점

        3. 차주 계획
           - 다음 주에 진행할 업무 목록

        4. 이슈 및 건의사항
           - 업무 진행 중 발생한 이슈
           - 필요한 지원 사항

        전문적이고 간결하게 작성해주세요.
        """

        report_content = await self.llm.generate(report_prompt)

        return {
            'content': report_content,
            'data_summary': {
                'total_requirements': len(weekly_data),
                'completed_tasks': sum(d['completed_tasks'] for d in weekly_data),
                'in_progress_tasks': sum(d['total_tasks'] - d['completed_tasks']
                                        for d in weekly_data)
            }
        }
```

---

## 8. 구현 계획

### 8.1 단계별 구현 계획

#### Phase 1: 기반 인프라 (1주차)

| 태스크 | 설명 | 산출물 |
|--------|------|--------|
| DB 스키마 생성 | PostgreSQL 테이블 생성 | migration scripts |
| Entity 클래스 구현 | RFP, Requirement, Report 엔티티 | Java Entity 클래스 |
| Repository 구현 | JPA Repository 인터페이스 | Repository 인터페이스 |
| 테넌트 인프라 | TenantContext, Filter 구현 | Tenant 관련 클래스 |

#### Phase 2: RFP 관리 기능 (2주차)

| 태스크 | 설명 | 산출물 |
|--------|------|--------|
| RFP 업로드 API | 파일 업로드 엔드포인트 | Controller, Service |
| 요구사항 추출 | LLM 연동 추출 로직 | Python 서비스 |
| Neo4j 연동 | 그래프 노드 생성 로직 | Neo4j 서비스 확장 |
| 요구사항 CRUD | 요구사항 관리 API | API 엔드포인트 |

#### Phase 3: 매핑 및 진행률 (3주차)

| 태스크 | 설명 | 산출물 |
|--------|------|--------|
| 스프린트 매핑 API | 매핑 CRUD | Controller, Service |
| 태스크 매핑 API | 태스크-요구사항 연결 | Controller, Service |
| 진행률 계산 | 자동 진행률 업데이트 | Event Handler |
| 상태 관리 | 상태 자동 전환 로직 | Service 로직 |

#### Phase 4: AI 보고서 생성 (4주차)

| 태스크 | 설명 | 산출물 |
|--------|------|--------|
| 보고서 생성 API | 주간보고 생성 엔드포인트 | Controller, Service |
| LLM 보고서 생성 | AI 보고서 생성 로직 | Python 서비스 |
| 버전 관리 | 보고서 버전 히스토리 | Service 로직 |
| 다운로드 기능 | PDF/텍스트 다운로드 | File Service |

#### Phase 5: Frontend 개발 (5-6주차)

| 태스크 | 설명 | 산출물 |
|--------|------|--------|
| RFP 업로드 UI | 파일 업로드 컴포넌트 | React 컴포넌트 |
| 요구사항 보드 | 칸반 스타일 관리 화면 | React 컴포넌트 |
| 매핑 UI | 드래그앤드롭 매핑 | React 컴포넌트 |
| 보고서 UI | 생성/뷰어/히스토리 | React 컴포넌트 |

#### Phase 6: 테스트 및 통합 (7주차)

| 태스크 | 설명 | 산출물 |
|--------|------|--------|
| 단위 테스트 | Service/Repository 테스트 | JUnit 테스트 |
| 통합 테스트 | API 통합 테스트 | 통합 테스트 |
| E2E 테스트 | Frontend E2E | Cypress 테스트 |
| 성능 테스트 | 부하 테스트 | 성능 보고서 |

### 8.2 파일 구조 (구현 예정)

```
PMS_IC_BackEnd_v1.2/src/main/java/com/insuretech/pms/
├── rfp/
│   ├── controller/
│   │   └── RfpController.java
│   ├── service/
│   │   ├── RfpService.java
│   │   └── RfpServiceImpl.java
│   ├── repository/
│   │   └── RfpRepository.java
│   ├── entity/
│   │   ├── Rfp.java
│   │   └── RfpStatus.java
│   └── dto/
│       ├── RfpDto.java
│       ├── RfpDetailDto.java
│       └── CreateRfpRequest.java
│
├── requirement/
│   ├── controller/
│   │   ├── RequirementController.java
│   │   └── RequirementMappingController.java
│   ├── service/
│   │   ├── RequirementService.java
│   │   ├── RequirementServiceImpl.java
│   │   ├── RequirementMappingService.java
│   │   └── RequirementProgressService.java
│   ├── repository/
│   │   ├── RequirementRepository.java
│   │   └── RequirementMappingRepository.java
│   ├── entity/
│   │   ├── Requirement.java
│   │   ├── RequirementCategory.java
│   │   ├── RequirementStatus.java
│   │   ├── RequirementSprintMapping.java
│   │   └── RequirementTaskMapping.java
│   └── dto/
│       ├── RequirementDto.java
│       ├── RequirementDetailDto.java
│       └── MappingRequest.java
│
├── report/
│   ├── controller/
│   │   └── WeeklyReportController.java
│   ├── service/
│   │   ├── WeeklyReportService.java
│   │   ├── WeeklyReportServiceImpl.java
│   │   └── ReportGenerationService.java
│   ├── repository/
│   │   ├── WeeklyReportRepository.java
│   │   └── ReportVersionRepository.java
│   ├── entity/
│   │   ├── WeeklyReport.java
│   │   ├── ReportVersion.java
│   │   └── ReportStatus.java
│   └── dto/
│       ├── ReportDto.java
│       ├── GenerateReportRequest.java
│       └── ReportVersionDto.java
│
└── tenant/
    ├── TenantContext.java
    ├── TenantFilter.java
    └── TenantInterceptor.java

llm-service/
├── requirement_extractor.py      # 요구사항 추출 서비스
├── report_generator.py           # 보고서 생성 서비스
├── rag_service_neo4j.py          # Neo4j 서비스 확장
└── api/
    ├── rfp_routes.py             # RFP 관련 API 라우트
    └── report_routes.py          # 보고서 관련 API 라우트

PMS_IC_FrontEnd_v1.2/src/app/components/
├── rfp/
│   ├── RfpUpload.tsx
│   ├── RfpList.tsx
│   └── RfpDetail.tsx
├── requirement/
│   ├── RequirementBoard.tsx
│   ├── RequirementCard.tsx
│   └── RequirementMapping.tsx
└── report/
    ├── ReportGenerator.tsx
    ├── ReportList.tsx
    └── ReportViewer.tsx
```

---

## 9. 기술적 고려사항

### 9.1 성능 최적화

| 영역 | 전략 |
|------|------|
| RFP 처리 | 비동기 처리 (Kafka/Redis Queue) |
| 진행률 계산 | 이벤트 기반 업데이트 + 캐싱 |
| 보고서 생성 | 백그라운드 작업 + 상태 폴링 |
| Graph 쿼리 | 인덱스 최적화 + 페이지네이션 |

### 9.2 보안 고려사항

| 영역 | 대책 |
|------|------|
| 파일 업로드 | 파일 타입 검증, 악성코드 스캔 |
| 테넌트 격리 | 쿼리 레벨 필터 + API 검증 |
| 인증/인가 | JWT + Role-based Access Control |
| 데이터 암호화 | 민감 정보 암호화 저장 |

### 9.3 확장성 고려사항

| 영역 | 전략 |
|------|------|
| 대용량 RFP | 청킹 + 분산 처리 |
| 다중 프로젝트 | 테넌트 기반 샤딩 |
| LLM 부하 | 큐 기반 rate limiting |
| 스토리지 | S3 + CDN 연동 |

---

## 10. 부록

### 10.1 Enum 정의

```java
public enum RfpStatus {
    UPLOADED, PROCESSING, COMPLETED, FAILED
}

public enum ProcessingStatus {
    PENDING, EXTRACTING, INDEXING, COMPLETED, FAILED
}

public enum RequirementCategory {
    FUNCTIONAL, NON_FUNCTIONAL, UI, INTEGRATION, SECURITY
}

public enum RequirementStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED, CANCELLED
}

public enum Priority {
    CRITICAL, HIGH, MEDIUM, LOW
}

public enum ReportType {
    WEEKLY, MONTHLY, SPRINT, CUSTOM
}

public enum ReportStatus {
    DRAFT, PENDING_APPROVAL, APPROVED, REJECTED
}

public enum ReportFormat {
    TEXT, PDF, HTML
}
```

### 10.2 API 응답 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 완료 |
| 204 | 삭제 완료 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복 등) |
| 500 | 서버 오류 |

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-14 | AI Assistant | 초안 작성 |
