# RFP 관리 기능 구현 계획

## 문서 정보
| 항목 | 내용 |
|------|------|
| 작성일 | 2026-01-14 |
| 관련 문서 | RFP_관리_기능_설계서.md |
| 상태 | Draft |

---

## 1. 구현 우선순위

### 1.1 우선순위 기준

| 우선순위 | 기준 | 설명 |
|---------|------|------|
| P0 (Critical) | 핵심 기능 | 다른 기능의 기반이 되는 필수 기능 |
| P1 (High) | 주요 기능 | 사용자 가치가 높은 핵심 기능 |
| P2 (Medium) | 보조 기능 | 사용성 향상을 위한 기능 |
| P3 (Low) | 부가 기능 | 있으면 좋은 기능 |

### 1.2 기능별 우선순위

| 기능 | 우선순위 | 의존성 | 이유 |
|------|---------|--------|------|
| DB 스키마 및 Entity | P0 | - | 모든 기능의 기반 |
| 테넌트 인프라 | P0 | - | 데이터 격리 필수 |
| RFP 파일 업로드 | P0 | Entity | 요구사항 추출의 전제 조건 |
| 요구사항 자동 추출 (RAG) | P0 | RFP 업로드 | 핵심 차별화 기능 |
| 요구사항 CRUD | P1 | Entity | 기본 관리 기능 |
| 요구사항-스프린트 매핑 | P1 | 요구사항 CRUD | 추적 기능의 핵심 |
| 요구사항 진행률 계산 | P1 | 매핑 | 상태 추적 핵심 |
| AI 보고서 생성 | P1 | 진행률 | 사용자 가치 높음 |
| 보고서 버전 관리 | P2 | 보고서 생성 | 이력 관리 |
| 요구사항 의존성 분석 | P2 | 요구사항 CRUD | 고급 분석 기능 |
| 보고서 승인 워크플로우 | P3 | 보고서 생성 | 부가 기능 |

---

## 2. 단계별 구현 상세

### Phase 1: 기반 인프라 구축

#### Week 1: Database & Entity Layer

**Day 1-2: PostgreSQL 스키마**

```sql
-- 1. RFP 테이블 생성 스크립트
-- 파일: src/main/resources/db/migration/V2__create_rfp_tables.sql

-- RFP 테이블
CREATE TABLE IF NOT EXISTS project.rfps (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(36) NOT NULL,
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

-- 요구사항 테이블
CREATE TABLE IF NOT EXISTS project.requirements (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    rfp_id VARCHAR(36),
    project_id VARCHAR(36) NOT NULL,
    requirement_code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(50) DEFAULT 'FUNCTIONAL',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'NOT_STARTED',
    progress INTEGER DEFAULT 0,
    source_text TEXT,
    page_number INTEGER,
    assignee_id VARCHAR(36),
    due_date DATE,
    tenant_id VARCHAR(36) NOT NULL,
    neo4j_node_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    CONSTRAINT fk_req_rfp FOREIGN KEY (rfp_id)
        REFERENCES project.rfps(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_req_assignee FOREIGN KEY (assignee_id)
        REFERENCES auth.users(id) ON DELETE SET NULL
);

-- 인덱스 생성
CREATE INDEX idx_rfp_project ON project.rfps(project_id);
CREATE INDEX idx_rfp_tenant ON project.rfps(tenant_id);
CREATE INDEX idx_req_project ON project.requirements(project_id);
CREATE INDEX idx_req_rfp ON project.requirements(rfp_id);
CREATE INDEX idx_req_assignee ON project.requirements(assignee_id);
CREATE INDEX idx_req_status ON project.requirements(status);
CREATE INDEX idx_req_tenant ON project.requirements(tenant_id);
```

**Day 3-4: Entity 클래스 구현**

구현 파일 목록:
1. `rfp/entity/Rfp.java`
2. `rfp/entity/RfpStatus.java`
3. `rfp/entity/ProcessingStatus.java`
4. `requirement/entity/Requirement.java`
5. `requirement/entity/RequirementCategory.java`
6. `requirement/entity/RequirementStatus.java`
7. `requirement/entity/Priority.java`

**Day 5: 테넌트 인프라**

구현 파일 목록:
1. `tenant/TenantContext.java` - ThreadLocal 기반 테넌트 컨텍스트
2. `tenant/TenantFilter.java` - JWT에서 테넌트 추출
3. `tenant/TenantAspect.java` - Repository 쿼리 자동 필터링

```java
// TenantContext.java 예시 구조
@Component
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
```

---

### Phase 2: RFP 관리 핵심 기능

#### Week 2: RFP 업로드 및 요구사항 추출

**Day 1-2: RFP 업로드 API**

구현 파일:
1. `rfp/controller/RfpController.java`
2. `rfp/service/RfpService.java`
3. `rfp/service/RfpServiceImpl.java`
4. `rfp/repository/RfpRepository.java`
5. `rfp/dto/RfpDto.java`
6. `rfp/dto/RfpUploadRequest.java`

```java
// RfpController.java 핵심 메서드
@RestController
@RequestMapping("/api/rfp")
@RequiredArgsConstructor
public class RfpController {

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('PM', 'PMO_HEAD', 'BUSINESS_ANALYST')")
    public ResponseEntity<ApiResponse<RfpDto>> uploadRfp(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {
        // 구현
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RfpDto>>> getRfpsByProject(
            @RequestParam("projectId") String projectId) {
        // 구현
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RfpDetailDto>> getRfpById(
            @PathVariable String id) {
        // 구현
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProcessingStatusDto>> getProcessingStatus(
            @PathVariable String id) {
        // 구현
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PM', 'PMO_HEAD')")
    public ResponseEntity<ApiResponse<Void>> deleteRfp(@PathVariable String id) {
        // 구현
    }
}
```

**Day 3-4: LLM Service - 요구사항 추출**

Python 파일:
1. `llm-service/requirement_extractor.py`
2. `llm-service/api/rfp_routes.py`

```python
# requirement_extractor.py 핵심 구조
class RequirementExtractor:
    def __init__(self, llm_client, neo4j_service):
        self.llm = llm_client
        self.neo4j = neo4j_service

    async def extract_from_document(self, file_path: str, rfp_id: str,
                                    project_id: str, tenant_id: str) -> List[dict]:
        """
        문서에서 요구사항 추출 메인 함수
        """
        # 1. 문서 파싱
        text = await self.parse_document(file_path)

        # 2. 청킹
        chunks = self.chunk_text(text, chunk_size=2000, overlap=200)

        # 3. 요구사항 추출
        requirements = []
        for i, chunk in enumerate(chunks):
            extracted = await self.extract_requirements_from_chunk(chunk, i)
            requirements.extend(extracted)

        # 4. 중복 제거 및 ID 부여
        unique_reqs = self.deduplicate_and_assign_ids(requirements, project_id)

        # 5. Neo4j에 저장
        for req in unique_reqs:
            await self.save_to_neo4j(req, rfp_id, tenant_id)

        return unique_reqs

    async def extract_requirements_from_chunk(self, chunk: str,
                                              chunk_index: int) -> List[dict]:
        prompt = f"""
        문서의 다음 부분에서 요구사항을 추출해주세요.
        요구사항은 시스템이 수행해야 하는 기능이나 만족해야 하는 조건입니다.

        문서 내용:
        {chunk}

        JSON 배열 형식으로 응답해주세요:
        [{{
            "title": "요구사항 제목 (50자 이내)",
            "description": "상세 설명",
            "category": "FUNCTIONAL|NON_FUNCTIONAL|UI|INTEGRATION|SECURITY",
            "priority": "HIGH|MEDIUM|LOW",
            "source_text": "원본 텍스트 발췌"
        }}]

        요구사항이 없으면 빈 배열 []을 반환하세요.
        """

        response = await self.llm.generate(prompt)
        return self.parse_json_response(response)
```

**Day 5: Neo4j 연동**

Neo4j 서비스 확장:
1. `llm-service/rag_service_neo4j.py` 수정

```python
# rag_service_neo4j.py에 추가할 메서드들

async def create_rfp_node(self, rfp_id: str, project_id: str,
                          name: str, tenant_id: str) -> dict:
    """RFP 노드 생성"""
    query = """
    CREATE (r:RFP {
        id: $rfp_id,
        project_id: $project_id,
        name: $name,
        tenant_id: $tenant_id,
        created_at: datetime()
    })
    RETURN r
    """
    return await self.execute_query(query, {
        'rfp_id': rfp_id,
        'project_id': project_id,
        'name': name,
        'tenant_id': tenant_id
    })

async def create_requirement_node(self, requirement: dict,
                                  rfp_id: str, tenant_id: str) -> dict:
    """요구사항 노드 생성 및 RFP와 연결"""
    # 임베딩 생성
    text_for_embedding = f"{requirement['title']} {requirement['description']}"
    embedding = await self.generate_embedding(text_for_embedding)

    query = """
    MATCH (rfp:RFP {id: $rfp_id, tenant_id: $tenant_id})
    CREATE (req:Requirement {
        id: $req_id,
        code: $code,
        title: $title,
        description: $description,
        category: $category,
        priority: $priority,
        status: 'NOT_STARTED',
        progress: 0,
        source_text: $source_text,
        tenant_id: $tenant_id,
        embedding: $embedding,
        created_at: datetime()
    })
    CREATE (rfp)-[:CONTAINS]->(req)
    RETURN req
    """
    return await self.execute_query(query, {
        'rfp_id': rfp_id,
        'req_id': requirement['id'],
        'code': requirement['code'],
        'title': requirement['title'],
        'description': requirement['description'],
        'category': requirement['category'],
        'priority': requirement['priority'],
        'source_text': requirement.get('source_text', ''),
        'tenant_id': tenant_id,
        'embedding': embedding
    })
```

---

### Phase 3: 매핑 및 진행률 관리

#### Week 3: 스프린트 매핑 및 진행률

**Day 1-2: 매핑 테이블 및 API**

DB 마이그레이션:
```sql
-- V3__create_mapping_tables.sql

CREATE TABLE IF NOT EXISTS project.requirement_sprint_mapping (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id VARCHAR(36) NOT NULL,
    sprint_id VARCHAR(36) NOT NULL,
    mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mapped_by VARCHAR(36),
    CONSTRAINT fk_rsm_requirement FOREIGN KEY (requirement_id)
        REFERENCES project.requirements(id) ON DELETE CASCADE,
    CONSTRAINT fk_rsm_sprint FOREIGN KEY (sprint_id)
        REFERENCES task.sprints(id) ON DELETE CASCADE,
    CONSTRAINT uk_req_sprint UNIQUE (requirement_id, sprint_id)
);

CREATE TABLE IF NOT EXISTS project.requirement_task_mapping (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    contribution_weight DECIMAL(3,2) DEFAULT 1.0,
    mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mapped_by VARCHAR(36),
    CONSTRAINT fk_rtm_requirement FOREIGN KEY (requirement_id)
        REFERENCES project.requirements(id) ON DELETE CASCADE,
    CONSTRAINT fk_rtm_task FOREIGN KEY (task_id)
        REFERENCES task.tasks(id) ON DELETE CASCADE,
    CONSTRAINT uk_req_task UNIQUE (requirement_id, task_id)
);
```

구현 파일:
1. `requirement/entity/RequirementSprintMapping.java`
2. `requirement/entity/RequirementTaskMapping.java`
3. `requirement/controller/RequirementMappingController.java`
4. `requirement/service/RequirementMappingService.java`
5. `requirement/repository/RequirementSprintMappingRepository.java`
6. `requirement/repository/RequirementTaskMappingRepository.java`

**Day 3-4: 진행률 자동 계산**

```java
// RequirementProgressService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementProgressService {

    private final RequirementRepository requirementRepository;
    private final RequirementTaskMappingRepository mappingRepository;
    private final TaskRepository taskRepository;

    /**
     * 요구사항 진행률 재계산
     */
    @Transactional
    public void recalculateProgress(String requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Requirement not found"));

        List<RequirementTaskMapping> mappings =
            mappingRepository.findByRequirementId(requirementId);

        if (mappings.isEmpty()) {
            requirement.setProgress(0);
            requirement.setStatus(RequirementStatus.NOT_STARTED);
        } else {
            // 가중치 적용 진행률 계산
            double totalWeight = 0;
            double completedWeight = 0;

            for (RequirementTaskMapping mapping : mappings) {
                Task task = taskRepository.findById(mapping.getTaskId())
                    .orElse(null);
                if (task != null) {
                    double weight = mapping.getContributionWeight() != null
                        ? mapping.getContributionWeight() : 1.0;
                    totalWeight += weight;

                    if (task.getStatus() == TaskStatus.DONE) {
                        completedWeight += weight;
                    }
                }
            }

            int progress = totalWeight > 0
                ? (int) Math.round((completedWeight / totalWeight) * 100)
                : 0;

            requirement.setProgress(progress);
            requirement.setStatus(calculateStatus(progress, requirement.getDueDate()));
        }

        requirementRepository.save(requirement);

        // Neo4j 동기화
        syncToNeo4j(requirement);
    }

    private RequirementStatus calculateStatus(int progress, LocalDate dueDate) {
        if (progress == 100) {
            return RequirementStatus.COMPLETED;
        } else if (progress > 0) {
            if (dueDate != null && LocalDate.now().isAfter(dueDate)) {
                return RequirementStatus.DELAYED;
            }
            return RequirementStatus.IN_PROGRESS;
        }
        return RequirementStatus.NOT_STARTED;
    }
}
```

**Day 5: 이벤트 기반 업데이트**

```java
// TaskStatusChangeEventListener.java
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskStatusChangeEventListener {

    private final RequirementProgressService progressService;
    private final RequirementTaskMappingRepository mappingRepository;

    @TransactionalEventListener
    public void handleTaskStatusChange(TaskStatusChangedEvent event) {
        log.info("Task status changed: {} -> {}",
            event.getTaskId(), event.getNewStatus());

        // 해당 태스크와 매핑된 모든 요구사항 찾기
        List<RequirementTaskMapping> mappings =
            mappingRepository.findByTaskId(event.getTaskId());

        // 각 요구사항의 진행률 재계산
        for (RequirementTaskMapping mapping : mappings) {
            progressService.recalculateProgress(mapping.getRequirementId());
        }
    }
}
```

---

### Phase 4: AI 보고서 생성

#### Week 4: 보고서 생성 기능

**Day 1-2: 보고서 Entity 및 API**

DB 마이그레이션:
```sql
-- V4__create_report_tables.sql

CREATE TABLE IF NOT EXISTS report.weekly_reports (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(36) NOT NULL,
    assignee_id VARCHAR(36),
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
    approved_at TIMESTAMP,
    CONSTRAINT fk_report_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_assignee FOREIGN KEY (assignee_id)
        REFERENCES auth.users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS report.report_versions (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id VARCHAR(36) NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    change_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    CONSTRAINT fk_rv_report FOREIGN KEY (report_id)
        REFERENCES report.weekly_reports(id) ON DELETE CASCADE,
    CONSTRAINT uk_report_version UNIQUE (report_id, version)
);

CREATE INDEX idx_report_project ON report.weekly_reports(project_id);
CREATE INDEX idx_report_assignee ON report.weekly_reports(assignee_id);
CREATE INDEX idx_report_tenant ON report.weekly_reports(tenant_id);
```

구현 파일:
1. `report/entity/WeeklyReport.java`
2. `report/entity/ReportVersion.java`
3. `report/entity/ReportType.java`
4. `report/entity/ReportStatus.java`
5. `report/controller/WeeklyReportController.java`
6. `report/service/WeeklyReportService.java`
7. `report/repository/WeeklyReportRepository.java`

**Day 3-4: LLM 보고서 생성**

```python
# report_generator.py

class WeeklyReportGenerator:
    def __init__(self, llm_client, neo4j_service, db_service):
        self.llm = llm_client
        self.neo4j = neo4j_service
        self.db = db_service

    async def generate_report(self, project_id: str, assignee_id: str,
                              period_start: str, period_end: str,
                              tenant_id: str) -> dict:
        """
        AI 주간보고 생성
        """
        # 1. 데이터 수집
        data = await self.collect_report_data(
            project_id, assignee_id, period_start, period_end, tenant_id
        )

        # 2. 사용자 정보
        user = await self.db.get_user(assignee_id) if assignee_id else None
        user_name = user['name'] if user else '전체 팀'

        # 3. 프로젝트 정보
        project = await self.db.get_project(project_id)

        # 4. AI 보고서 생성
        prompt = self.build_report_prompt(data, user_name, project, period_start, period_end)
        report_content = await self.llm.generate(prompt)

        return {
            'title': f"주간보고_{user_name}_{period_end}",
            'content': report_content,
            'summary': self.generate_summary(data)
        }

    async def collect_report_data(self, project_id, assignee_id,
                                  start_date, end_date, tenant_id):
        """
        보고서 생성에 필요한 데이터 수집
        """
        # Neo4j에서 요구사항 및 태스크 데이터 조회
        query = """
        MATCH (r:Requirement {project_id: $project_id, tenant_id: $tenant_id})
        WHERE ($assignee_id IS NULL OR r.assignee_id = $assignee_id)
        OPTIONAL MATCH (r)-[:IMPLEMENTED_BY]->(t:Task)
        WHERE t.updated_at >= datetime($start_date)
          AND t.updated_at <= datetime($end_date)
        WITH r, collect(t) as tasks
        RETURN {
            requirement_id: r.id,
            requirement_code: r.code,
            requirement_title: r.title,
            status: r.status,
            progress: r.progress,
            tasks: [task IN tasks | {
                id: task.id,
                title: task.title,
                status: task.status,
                updated_at: task.updated_at
            }]
        } as data
        """

        return await self.neo4j.execute_query(query, {
            'project_id': project_id,
            'assignee_id': assignee_id,
            'tenant_id': tenant_id,
            'start_date': start_date,
            'end_date': end_date
        })

    def build_report_prompt(self, data, user_name, project, start_date, end_date):
        return f"""
        다음 데이터를 바탕으로 전문적인 주간 업무 보고서를 작성해주세요.

        [기본 정보]
        - 프로젝트: {project['name']}
        - 담당자: {user_name}
        - 보고 기간: {start_date} ~ {end_date}

        [주간 실적 데이터]
        {json.dumps(data, ensure_ascii=False, indent=2)}

        [보고서 형식]
        ## 1. 금주 완료 사항
        - 완료된 요구사항/태스크를 구체적으로 나열
        - 각 항목의 성과와 결과물 기술

        ## 2. 진행 중인 업무
        - 현재 진행 중인 요구사항 목록
        - 각 항목의 진행률과 예상 완료일

        ## 3. 차주 계획
        - 다음 주 진행 예정 업무
        - 목표 및 마일스톤

        ## 4. 이슈 및 건의사항
        - 업무 진행 중 발생한 이슈 (있는 경우)
        - 지원 필요 사항 (있는 경우)

        전문적이고 간결하게 작성하되, 구체적인 수치와 사실에 기반해주세요.
        """
```

**Day 5: 버전 관리 및 다운로드**

```java
// WeeklyReportServiceImpl.java 일부

@Override
@Transactional
public ReportDto regenerateReport(String id) {
    WeeklyReport existing = reportRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

    // 기존 버전 저장
    ReportVersion version = ReportVersion.builder()
        .reportId(id)
        .version(existing.getVersion())
        .content(existing.getContent())
        .changeSummary("새 버전 생성으로 인한 이전 버전 보관")
        .createdBy(getCurrentUserId())
        .build();
    reportVersionRepository.save(version);

    // 새 보고서 생성
    GenerateReportRequest request = GenerateReportRequest.builder()
        .projectId(existing.getProjectId())
        .assigneeId(existing.getAssigneeId())
        .periodStart(existing.getReportPeriodStart())
        .periodEnd(existing.getReportPeriodEnd())
        .format(existing.getFormat())
        .build();

    String newContent = llmClient.generateReport(request);

    // 버전 업데이트
    existing.setContent(newContent);
    existing.setVersion(existing.getVersion() + 1);
    existing.setUpdatedAt(LocalDateTime.now());

    return ReportDto.from(reportRepository.save(existing));
}

@Override
public byte[] downloadReport(String id) {
    WeeklyReport report = reportRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

    if (report.getFormat() == ReportFormat.PDF) {
        return generatePdf(report);
    }
    return report.getContent().getBytes(StandardCharsets.UTF_8);
}

private byte[] generatePdf(WeeklyReport report) {
    // PDF 생성 로직 (iText 또는 OpenPDF 사용)
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        // 제목
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        document.add(new Paragraph(report.getTitle(), titleFont));
        document.add(new Paragraph("\n"));

        // 본문
        Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        document.add(new Paragraph(report.getContent(), contentFont));

        document.close();
        return out.toByteArray();
    } catch (Exception e) {
        throw new RuntimeException("PDF 생성 실패", e);
    }
}
```

---

### Phase 5: Frontend 개발

#### Week 5-6: React 컴포넌트

**주요 컴포넌트 목록:**

| 컴포넌트 | 파일 | 설명 |
|---------|------|------|
| RfpUpload | `rfp/RfpUpload.tsx` | 드래그앤드롭 파일 업로드 |
| RfpList | `rfp/RfpList.tsx` | RFP 목록 테이블 |
| RequirementBoard | `requirement/RequirementBoard.tsx` | 칸반 스타일 보드 |
| RequirementCard | `requirement/RequirementCard.tsx` | 요구사항 카드 |
| RequirementMapping | `requirement/RequirementMapping.tsx` | 매핑 UI |
| ReportGenerator | `report/ReportGenerator.tsx` | 보고서 생성 폼 |
| ReportViewer | `report/ReportViewer.tsx` | 보고서 뷰어 |

**API 서비스 파일:**
```typescript
// services/rfpService.ts
export const rfpService = {
  uploadRfp: (file: File, projectId: string, name?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', projectId);
    if (name) formData.append('name', name);

    return api.post<RfpDto>('/api/rfp/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },

  getRfpsByProject: (projectId: string) =>
    api.get<RfpDto[]>(`/api/rfp?projectId=${projectId}`),

  getRfpById: (id: string) =>
    api.get<RfpDetailDto>(`/api/rfp/${id}`),

  getProcessingStatus: (id: string) =>
    api.get<ProcessingStatusDto>(`/api/rfp/${id}/status`),

  deleteRfp: (id: string) =>
    api.delete(`/api/rfp/${id}`)
};

// services/requirementService.ts
export const requirementService = {
  getRequirements: (projectId: string, status?: string) =>
    api.get<RequirementDto[]>(`/api/requirements`, { params: { projectId, status } }),

  createRequirement: (data: CreateRequirementRequest) =>
    api.post<RequirementDto>('/api/requirements', data),

  updateRequirement: (id: string, data: UpdateRequirementRequest) =>
    api.put<RequirementDto>(`/api/requirements/${id}`, data),

  mapToSprint: (requirementId: string, sprintId: string) =>
    api.post('/api/mapping/requirement-sprint', { requirementId, sprintId }),

  mapToTask: (requirementId: string, taskId: string, weight?: number) =>
    api.post('/api/mapping/requirement-task', { requirementId, taskId, contributionWeight: weight })
};

// services/reportService.ts
export const reportService = {
  generateReport: (request: GenerateReportRequest) =>
    api.post<ReportDto>('/api/reports/weekly/generate', request),

  getReports: (params: { projectId: string; type?: string; assigneeId?: string }) =>
    api.get<ReportDto[]>('/api/reports', { params }),

  downloadReport: (id: string) =>
    api.get(`/api/reports/${id}/download`, { responseType: 'blob' }),

  getVersionHistory: (id: string) =>
    api.get<ReportVersionDto[]>(`/api/reports/${id}/versions`)
};
```

---

### Phase 6: 테스트 및 통합

#### Week 7: 테스트

**테스트 범위:**

| 테스트 유형 | 대상 | 도구 |
|------------|------|------|
| 단위 테스트 | Service, Repository | JUnit 5, Mockito |
| 통합 테스트 | API Endpoints | Spring Boot Test |
| E2E 테스트 | 전체 플로우 | Cypress |
| 성능 테스트 | API 응답 시간, 동시성 | JMeter |

**테스트 시나리오:**

```gherkin
Feature: RFP 업로드 및 요구사항 추출

  Scenario: PDF RFP 업로드 및 요구사항 자동 추출
    Given 사용자가 PM 권한으로 로그인되어 있음
    And 프로젝트 "PMS 개발"이 존재함
    When RFP 파일 "requirements.pdf"를 업로드함
    Then 업로드 상태가 "PROCESSING"으로 표시됨
    And 처리 완료 후 요구사항 목록이 생성됨
    And 각 요구사항에 ID가 자동 부여됨

  Scenario: 요구사항-스프린트 매핑
    Given 요구사항 "REQ-PMS-FUNC-001"이 존재함
    And 스프린트 "Sprint 1"이 존재함
    When 요구사항을 스프린트에 매핑함
    Then 매핑 관계가 저장됨
    And 요구사항 상세에서 매핑된 스프린트가 표시됨

  Scenario: AI 주간보고 생성
    Given 담당자가 할당된 요구사항이 존재함
    And 해당 요구사항에 매핑된 완료 태스크가 있음
    When "주간보고 생성" 버튼을 클릭함
    Then AI가 주간보고를 생성함
    And 보고서에 완료 항목이 포함됨
```

---

## 3. 리소스 및 의존성

### 3.1 기술 의존성

**Backend (Java):**
```xml
<!-- pom.xml 추가 의존성 -->
<dependencies>
    <!-- PDF 생성 -->
    <dependency>
        <groupId>com.itextpdf</groupId>
        <artifactId>itextpdf</artifactId>
        <version>5.5.13.3</version>
    </dependency>

    <!-- 문서 파싱 (DOCX) -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>
</dependencies>
```

**LLM Service (Python):**
```txt
# requirements.txt 추가
python-docx==1.1.0
pdfplumber==0.10.3
openpyxl==3.1.2
```

**Frontend (React):**
```json
// package.json 추가 의존성
{
  "dependencies": {
    "react-dropzone": "^14.2.3",
    "react-beautiful-dnd": "^13.1.1",
    "@tanstack/react-query": "^5.0.0"
  }
}
```

### 3.2 환경 설정

**docker-compose.yml 추가:**
```yaml
services:
  # 기존 서비스들...

  # 파일 스토리지 (선택적)
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minio_user
      MINIO_ROOT_PASSWORD: minio_password
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data

volumes:
  minio_data:
```

---

## 4. 위험 요소 및 대응

| 위험 | 영향도 | 가능성 | 대응 방안 |
|------|--------|--------|----------|
| LLM 응답 품질 저하 | 높음 | 중간 | 프롬프트 튜닝, 후처리 검증 로직 |
| 대용량 RFP 처리 지연 | 중간 | 높음 | 청킹 최적화, 비동기 처리 |
| Neo4j 동기화 실패 | 높음 | 낮음 | 재시도 로직, 불일치 감지 배치 |
| 테넌트 데이터 누출 | 매우 높음 | 낮음 | 쿼리 레벨 검증, 보안 테스트 |

---

## 5. 체크리스트

### 5.1 Phase 1 완료 기준

- [ ] PostgreSQL 스키마 생성 완료
- [ ] Entity 클래스 구현 완료
- [ ] Repository 인터페이스 구현 완료
- [ ] 테넌트 컨텍스트 구현 완료
- [ ] 단위 테스트 작성 완료

### 5.2 Phase 2 완료 기준

- [ ] RFP 업로드 API 구현 완료
- [ ] 파일 저장 로직 구현 완료
- [ ] 요구사항 추출 LLM 연동 완료
- [ ] Neo4j 노드 생성 완료
- [ ] 요구사항 CRUD API 완료

### 5.3 Phase 3 완료 기준

- [ ] 매핑 API 구현 완료
- [ ] 진행률 계산 로직 구현 완료
- [ ] 이벤트 리스너 구현 완료
- [ ] Neo4j 동기화 완료

### 5.4 Phase 4 완료 기준

- [ ] 보고서 Entity/API 구현 완료
- [ ] LLM 보고서 생성 완료
- [ ] 버전 관리 구현 완료
- [ ] PDF 다운로드 구현 완료

### 5.5 Phase 5 완료 기준

- [ ] RFP 업로드 UI 완료
- [ ] 요구사항 보드 UI 완료
- [ ] 매핑 UI 완료
- [ ] 보고서 생성 UI 완료
- [ ] E2E 테스트 완료

### 5.6 Phase 6 완료 기준

- [ ] 전체 통합 테스트 완료
- [ ] 성능 테스트 완료
- [ ] 문서화 완료
- [ ] 배포 준비 완료

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-14 | AI Assistant | 초안 작성 |
