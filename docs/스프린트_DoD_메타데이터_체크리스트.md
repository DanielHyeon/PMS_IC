# 스프린트 완료 정의(DoD) - 메타데이터 체크리스트

## 문서 정보
| 항목 | 내용 |
|------|------|
| 작성일 | 2026-01-17 |
| 버전 | 1.0 |
| 목적 | 스프린트 완료 시 메타데이터 품질 보장을 위한 체크리스트 |

---

## 1. 기존 DoD + 메타데이터 확장

### 1.1 기존 스프린트 완료 정의

- [ ] 모든 유저 스토리의 태스크가 "Done" 상태
- [ ] 코드 리뷰 완료 (최소 1명 승인)
- [ ] 단위 테스트 통과 (커버리지 80% 이상)
- [ ] 통합 테스트 통과
- [ ] 문서화 완료 (API 명세, README 등)
- [ ] 스프린트 데모 완료

### 1.2 메타데이터 DoD 추가 항목 (신규)

#### 📊 데이터베이스 메타데이터

- [ ] **테이블 주석 작성**: 새로 생성된 테이블에 `COMMENT ON TABLE` 추가
- [ ] **컬럼 주석 작성**: 새로 생성된 컬럼에 `COMMENT ON COLUMN` 추가
- [ ] **스토리 ID 태깅**: 관련 테이블 주석에 `@story:` 태그 포함
- [ ] **담당자 정보**: 테이블 주석에 `@owner:` 태그 포함

#### 🔗 데이터 계보(Lineage)

- [ ] **데이터 출처 문서화**: 새 데이터가 어디서 왔는지 기록
- [ ] **영향도 분석 완료**: 스키마 변경 시 영향받는 기능 목록 작성
- [ ] **AI 학습데이터 변경 시**: 버전 태그 부여 및 변경 로그 기록

#### 📖 비즈니스 용어

- [ ] **신규 도메인 용어**: 새로운 비즈니스 용어 사용 시 글로서리에 추가 요청
- [ ] **일관된 명명**: 테이블/컬럼명이 도메인 용어와 일치하는지 확인

---

## 2. 상세 체크리스트 가이드

### 2.1 테이블 주석 표준

```sql
-- 새 테이블 생성 시 필수 주석 형식
COMMENT ON TABLE {schema}.{table_name} IS 
'{테이블 설명}
@story: {STORY-ID-1}, {STORY-ID-2}
@owner: {담당팀/담당자}
@tier: {Tier1|Tier2|Tier3}
@pii: {true|false}
@created: {YYYY-MM-DD}
@sprint: {SPRINT-ID}';

-- 예시
COMMENT ON TABLE project.requirements IS 
'RFP에서 추출된 기능/비기능 요구사항 정보를 저장하는 테이블
@story: STORY-001, STORY-015
@owner: backend-team
@tier: Tier1
@pii: false
@created: 2026-01-17
@sprint: SPRINT-2026-W03';
```

### 2.2 컬럼 주석 표준

```sql
-- 새 컬럼 생성 시 필수 주석 형식
COMMENT ON COLUMN {schema}.{table}.{column} IS 
'{컬럼 설명}
@type: {데이터 유형 설명}
@example: {예시 값}
@validation: {검증 규칙, 있는 경우}';

-- 예시
COMMENT ON COLUMN project.requirements.requirement_code IS 
'요구사항 고유 식별 코드
@type: 문자열 (최대 50자)
@example: REQ-PMS-FUNC-001
@validation: REQ-[A-Z]{3}-[A-Z]{4,5}-[0-9]{3} 형식';

COMMENT ON COLUMN project.requirements.status IS 
'요구사항 진행 상태
@type: ENUM
@values: NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED, CANCELLED
@default: NOT_STARTED';
```

### 2.3 Neo4j 노드 메타데이터 속성

```cypher
-- Neo4j 노드 생성 시 메타데이터 속성 포함
CREATE (n:Requirement {
    // 비즈니스 속성
    id: $id,
    code: $code,
    title: $title,
    description: $description,
    
    // 메타데이터 속성 (언더스코어 접두사)
    _story_id: $storyId,
    _sprint_id: $sprintId,
    _created_by: $createdBy,
    _created_at: datetime(),
    _version: "1.0.0",
    _data_quality_score: 1.0
})
```

---

## 3. 역할별 책임

### 3.1 개발자 (Developer)

| 책임 항목 | 설명 | 필수 여부 |
|----------|------|----------|
| 테이블 주석 작성 | 새 테이블 생성 시 COMMENT 문 실행 | **필수** |
| 컬럼 주석 작성 | 새 컬럼 추가 시 COMMENT 문 실행 | **필수** |
| 스토리 ID 태깅 | 주석에 @story 태그 포함 | **필수** |
| 스키마 변경 영향도 | 변경 시 영향받는 코드 목록 작성 | 필수 |
| 마이그레이션 스크립트 | 주석 포함된 마이그레이션 작성 | **필수** |

### 3.2 BA/기획자 (Business Analyst)

| 책임 항목 | 설명 | 필수 여부 |
|----------|------|----------|
| 비즈니스 용어 정의 | 새 도메인 용어 글로서리 추가 요청 | 필수 |
| 요구사항-데이터 매핑 | 요구사항과 관련 테이블 연결 문서화 | **필수** |
| 데이터 정의 검토 | 테이블/컬럼 설명의 비즈니스 정확성 검토 | 권장 |

### 3.3 PM/PMO

| 책임 항목 | 설명 | 필수 여부 |
|----------|------|----------|
| DoD 체크리스트 확인 | 스프린트 리뷰 전 체크리스트 검토 | **필수** |
| 메타데이터 커버리지 모니터링 | 주간 커버리지 지표 확인 | 권장 |
| 글로서리 승인 | 새 비즈니스 용어 추가 승인 | 필수 |

### 3.4 글로서리 가디언 (선택적 역할)

| 책임 항목 | 설명 | 필수 여부 |
|----------|------|----------|
| 용어 일관성 감수 | 보험 도메인 용어의 정확성 검토 | 필수 |
| 글로서리 유지보수 | 용어 정의 최신화 및 중복 제거 | 필수 |
| 교육 자료 제공 | 신규 팀원 대상 용어 교육 | 권장 |

---

## 4. 마이그레이션 스크립트 템플릿

### 4.1 Flyway/Liquibase 마이그레이션 예시

```sql
-- V{버전}__create_{기능}_tables.sql
-- 스프린트: SPRINT-2026-W03
-- 스토리: STORY-001, STORY-002
-- 작성자: developer@pms.com
-- 작성일: 2026-01-17

-- ============================================
-- 테이블 생성
-- ============================================

CREATE TABLE project.requirements (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(36) NOT NULL,
    rfp_id VARCHAR(36),
    requirement_code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(50) DEFAULT 'FUNCTIONAL',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'NOT_STARTED',
    progress INTEGER DEFAULT 0,
    assignee_id VARCHAR(36),
    due_date DATE,
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 인덱스 생성
-- ============================================

CREATE INDEX idx_requirements_project_id ON project.requirements(project_id);
CREATE INDEX idx_requirements_status ON project.requirements(status);
CREATE INDEX idx_requirements_assignee ON project.requirements(assignee_id);

-- ============================================
-- 테이블 주석 (메타데이터 DoD 필수)
-- ============================================

COMMENT ON TABLE project.requirements IS 
'RFP에서 AI가 자동 추출한 기능/비기능 요구사항 정보
@story: STORY-001, STORY-002
@owner: backend-team
@tier: Tier1
@pii: false
@created: 2026-01-17
@sprint: SPRINT-2026-W03';

-- ============================================
-- 컬럼 주석 (메타데이터 DoD 필수)
-- ============================================

COMMENT ON COLUMN project.requirements.id IS 
'요구사항 고유 식별자 (UUID)
@type: UUID
@example: 550e8400-e29b-41d4-a716-446655440000';

COMMENT ON COLUMN project.requirements.requirement_code IS 
'요구사항 코드 (사용자 표시용)
@type: 문자열
@format: REQ-{프로젝트코드}-{카테고리}-{순번}
@example: REQ-PMS-FUNC-001';

COMMENT ON COLUMN project.requirements.category IS 
'요구사항 카테고리
@type: ENUM
@values: FUNCTIONAL, NON_FUNCTIONAL, UI, INTEGRATION, SECURITY
@default: FUNCTIONAL';

COMMENT ON COLUMN project.requirements.priority IS 
'우선순위
@type: ENUM
@values: CRITICAL, HIGH, MEDIUM, LOW
@default: MEDIUM';

COMMENT ON COLUMN project.requirements.status IS 
'진행 상태
@type: ENUM
@values: NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED, CANCELLED
@default: NOT_STARTED';

COMMENT ON COLUMN project.requirements.progress IS 
'진행률 (0-100)
@type: INTEGER
@range: 0-100
@calculation: (완료 태스크 / 전체 태스크) * 100';

COMMENT ON COLUMN project.requirements.tenant_id IS 
'테넌트 식별자 (멀티테넌시 격리용)
@type: UUID
@reference: project.projects.id와 동일';
```

---

## 5. 체크리스트 자동화

### 5.1 PR 템플릿에 체크리스트 추가

```markdown
<!-- .github/PULL_REQUEST_TEMPLATE.md -->

## 변경 사항
- 

## 메타데이터 체크리스트

### 데이터베이스 변경 시
- [ ] 새 테이블에 COMMENT ON TABLE 추가
- [ ] 새 컬럼에 COMMENT ON COLUMN 추가
- [ ] @story 태그로 유저 스토리 연결
- [ ] @owner 태그로 담당팀 명시
- [ ] 영향도 분석 완료 (영향받는 기능: )

### Neo4j 변경 시
- [ ] 새 노드에 메타데이터 속성 추가 (_story_id, _sprint_id 등)
- [ ] 버전 속성 업데이트

### 변경 없음
- [ ] 이 PR은 데이터베이스 스키마 변경이 없습니다
```

### 5.2 CI/CD 자동 검증

```yaml
# .github/workflows/metadata-check.yml
name: Metadata Check

on:
  pull_request:
    paths:
      - 'docker/postgres/init/**'
      - '**/migration/**'
      - '**/entity/**'

jobs:
  check-comments:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Check SQL Comments
        run: |
          # 새 CREATE TABLE 문에 COMMENT가 있는지 확인
          for file in $(git diff --name-only origin/main -- '*.sql'); do
            if grep -q "CREATE TABLE" "$file"; then
              if ! grep -q "COMMENT ON TABLE" "$file"; then
                echo "❌ Missing COMMENT ON TABLE in $file"
                exit 1
              fi
              if ! grep -q "@story:" "$file"; then
                echo "⚠️ Missing @story tag in $file"
              fi
            fi
          done
          echo "✅ Metadata comments check passed"
```

---

## 6. 측정 지표

### 6.1 메타데이터 커버리지 지표

| 지표 | 계산 방법 | 목표 |
|------|----------|------|
| 테이블 설명 커버리지 | (설명 있는 테이블 / 전체 테이블) × 100 | **90%** |
| 컬럼 설명 커버리지 | (설명 있는 컬럼 / 전체 컬럼) × 100 | **80%** |
| 스토리 태깅 비율 | (태그된 테이블 / 변경된 테이블) × 100 | **100%** |

### 6.2 스프린트별 리포트

```sql
-- 메타데이터 커버리지 조회 쿼리
WITH table_stats AS (
    SELECT 
        n.nspname as schema_name,
        c.relname as table_name,
        d.description IS NOT NULL as has_description,
        d.description LIKE '%@story:%' as has_story_tag,
        d.description LIKE '%@owner:%' as has_owner_tag
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
    WHERE c.relkind = 'r'
      AND n.nspname IN ('auth', 'project', 'task', 'chat', 'report', 'risk')
)
SELECT 
    COUNT(*) as total_tables,
    SUM(CASE WHEN has_description THEN 1 ELSE 0 END) as tables_with_description,
    SUM(CASE WHEN has_story_tag THEN 1 ELSE 0 END) as tables_with_story_tag,
    SUM(CASE WHEN has_owner_tag THEN 1 ELSE 0 END) as tables_with_owner_tag,
    ROUND(100.0 * SUM(CASE WHEN has_description THEN 1 ELSE 0 END) / COUNT(*), 1) as description_coverage_pct,
    ROUND(100.0 * SUM(CASE WHEN has_story_tag THEN 1 ELSE 0 END) / COUNT(*), 1) as story_tag_coverage_pct
FROM table_stats;
```

---

## 7. FAQ

### Q1: 기존 테이블에도 주석을 추가해야 하나요?
**A**: 점진적으로 추가하세요. 새 스프린트에서 수정하는 테이블에 우선 적용하고, 별도 "메타데이터 정비" 태스크로 기존 테이블을 처리하세요.

### Q2: 모든 컬럼에 주석이 필요한가요?
**A**: id, created_at, updated_at 등 자명한 컬럼은 생략 가능합니다. 비즈니스 로직과 관련된 컬럼에 집중하세요.

### Q3: @story 태그에 여러 스토리를 연결해도 되나요?
**A**: 네, 쉼표로 구분하여 여러 스토리 ID를 기록할 수 있습니다: `@story: STORY-001, STORY-002`

### Q4: OpenMetadata가 없어도 이 체크리스트를 적용할 수 있나요?
**A**: 네, 이 체크리스트는 표준 SQL 주석을 사용하므로 OpenMetadata 없이도 적용 가능합니다. 나중에 OpenMetadata를 도입하면 자동으로 메타데이터를 수집할 수 있습니다.

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-17 | AI Assistant | 초안 작성 |
