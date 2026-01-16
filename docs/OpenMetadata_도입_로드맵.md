# OpenMetadata 도입 로드맵 및 효과성/효율성 분석

## 문서 정보
| 항목 | 내용 |
|------|------|
| 작성일 | 2026-01-17 |
| 버전 | 1.0 |
| 목적 | 스크럼 기반 PMS에 OpenMetadata 도입을 위한 ROI 분석 및 단계별 실행 계획 |
| 상태 | Draft |

---

## 1. 현황 분석

### 1.1 현재 PMS-IC 시스템 현황

```
┌─────────────────────────────────────────────────────────────────────┐
│                        현재 데이터 아키텍처                           │
├─────────────────────────────────────────────────────────────────────┤
│  PostgreSQL (5433)     Neo4j (7687)        Redis (6379)            │
│  ├── auth.*            ├── Document        ├── Session Cache       │
│  ├── project.*         ├── Chunk           └── Rate Limiting       │
│  ├── task.*            ├── Embedding                               │
│  ├── chat.*            └── (project_id 없음 ⚠️)                     │
│  ├── risk.*                                                         │
│  └── report.*                                                       │
│                                                                     │
│  프로젝트 격리 수준: 40-50%                                          │
│  메타데이터 관리: 없음 ❌                                             │
│  데이터 계보(Lineage): 없음 ❌                                        │
│  비즈니스 글로서리: 없음 ❌                                           │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 식별된 문제점

| 영역 | 현재 상태 | 영향도 | OpenMetadata 해결 가능 여부 |
|------|----------|--------|---------------------------|
| **데이터 탐색** | 개발자 지식 의존 | 높음 | ✅ 완전 해결 |
| **스키마 변경 추적** | Git 커밋 로그만 의존 | 중간 | ✅ 자동화 가능 |
| **요구사항-데이터 연결** | 수동/문서 기반 | 높음 | ✅ 태깅으로 해결 |
| **AI 학습데이터 관리** | 파편화 | 높음 | ✅ 카탈로그화 |
| **프로젝트 격리** | 40-50% 수준 | 심각 | ⚠️ 부분 해결 (별도 작업 필요) |
| **팀 온보딩** | 2-3주 소요 | 중간 | ✅ 단축 가능 |

---

## 2. 효과성 분석 (Effectiveness Analysis)

### 2.1 정량적 효과 예측

#### 2.1.1 데이터 탐색 시간 단축

```
현재 상태:
- 새 기능 개발 시 관련 테이블 파악: 평균 2-4시간
- 스키마 변경 시 영향도 분석: 평균 1-2시간
- 신규 팀원 데이터 구조 이해: 2-3주

OpenMetadata 도입 후 예상:
- 새 기능 개발 시 관련 테이블 파악: 15-30분 (85% 단축)
- 스키마 변경 시 영향도 분석: 5-10분 (90% 단축)
- 신규 팀원 데이터 구조 이해: 3-5일 (75% 단축)
```

| 지표 | 현재 | 도입 후 | 개선율 |
|------|------|---------|--------|
| 데이터 탐색 시간/주 | 8시간 | 1.5시간 | **81%** |
| 스키마 변경 분석 | 2시간 | 10분 | **92%** |
| 온보딩 기간 | 15일 | 4일 | **73%** |

#### 2.1.2 스프린트 효율성 향상

사용자 제시 수식 기반 분석:

$$Efficiency = \frac{Sprint\ Velocity \times Data\ Visibility}{Communication\ Overhead}$$

| 변수 | 현재 | 도입 후 | 변화 |
|------|------|---------|------|
| Sprint Velocity | 기준값 (1.0) | 1.15 | +15% |
| Data Visibility | 0.4 | 0.85 | +112% |
| Communication Overhead | 1.0 | 0.6 | -40% |
| **Efficiency** | **0.4** | **1.63** | **+308%** |

#### 2.1.3 요구사항 추적성 향상

```
현재 (RFP 기능 설계서 기준):
┌─────────────────────────────────────────────────────────────┐
│  User Story (PMS)  ──?──>  Data Table  ──?──>  산출물       │
│       ↑                         ↑                 ↑         │
│    연결 없음                연결 없음         연결 없음      │
└─────────────────────────────────────────────────────────────┘

OpenMetadata 도입 후:
┌─────────────────────────────────────────────────────────────┐
│  User Story  ──태그──>  Data Table  ──Lineage──>  산출물    │
│  (PMS)        story_id   (PostgreSQL)   계보 추적  (PDF)    │
│               담당자      Neo4j 노드                         │
│               스프린트                                       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 정성적 효과

| 효과 영역 | 설명 | 비즈니스 가치 |
|----------|------|-------------|
| **지식 자산화** | 개발자 이직 시에도 데이터 지식 보존 | 리스크 감소 |
| **품질 거버넌스** | 데이터 품질 지표 자동 수집 | 신뢰성 향상 |
| **규정 준수** | 보험 데이터 계보 추적 (감사 대응) | 컴플라이언스 |
| **AI 투명성** | RAG 데이터 출처 명확화 | AI 신뢰도 향상 |
| **협업 강화** | 비즈니스-개발 용어 통일 | 커뮤니케이션 개선 |

---

## 3. 효율성 분석 (Efficiency Analysis)

### 3.1 도입 비용 분석

#### 3.1.1 인프라 비용

| 항목 | 최소 사양 | 권장 사양 | 비용 (월) |
|------|----------|----------|----------|
| OpenMetadata Server | 4GB RAM, 2 vCPU | 8GB RAM, 4 vCPU | Docker 내 구동 (추가 비용 없음) |
| Elasticsearch | 4GB RAM | 8GB RAM | Docker 내 구동 |
| MySQL (OM 전용) | 2GB RAM | 4GB RAM | Docker 내 구동 |
| **총 추가 리소스** | **10GB RAM** | **20GB RAM** | **기존 서버 활용** |

현재 시스템 리소스:
```yaml
# docker-compose.yml 기준 현재 메모리 사용량
neo4j: heap 2GB + pagecache 1GB = ~3GB
postgres: 기본 ~512MB
llm-service: GPU 기반 (별도)
기타: ~2GB
─────────────────────────
현재 총: ~6GB
OpenMetadata 추가: +10~20GB
권장 총 메모리: 16~26GB
```

#### 3.1.2 인력 비용

| 단계 | 작업 | 예상 공수 | 담당 |
|------|------|----------|------|
| 인프라 구축 | Docker Compose 확장 | 1일 | DevOps |
| 초기 설정 | 커넥터 설정, 인제스션 | 2일 | Backend 개발자 |
| Python 스크립트 | PMS 연동 자동화 | 3일 | Backend 개발자 |
| 팀 교육 | 사용법, DoD 적용 | 0.5일 | 전체 팀 |
| 파일럿 운영 | 2스프린트 시범 적용 | 4주 (부분 시간) | 전체 팀 |
| **총 초기 투자** | - | **약 6.5일 + 4주 파일럿** | - |

#### 3.1.3 학습 비용

| 역할 | 학습 범위 | 예상 시간 |
|------|----------|----------|
| PM/PMO | 대시보드, 비즈니스 글로서리 | 2시간 |
| 개발자 | 데이터 탐색, 태깅, Lineage | 4시간 |
| BA | 요구사항-데이터 매핑 | 3시간 |
| DevOps | 인제스션 파이프라인 관리 | 6시간 |

### 3.2 ROI 분석

#### 3.2.1 비용-효과 매트릭스

```
             높음
              │
    효과성    │    ★ OpenMetadata
              │    (높은 효과, 중간 비용)
              │
              │────────────────────────
              │
              │
             낮음
              └──────────────────────────
                  낮음              높음
                        비용
```

#### 3.2.2 손익분기점 분석

```
초기 투자:
- 인력 비용: 6.5일 × 8시간 × 시급 = 52시간
- 인프라 비용: 0원 (기존 Docker 환경 활용)

주간 절감 효과:
- 데이터 탐색 시간 절감: 6.5시간/주/개발자
- 팀 규모 4명 가정: 26시간/주

손익분기점:
- 52시간 ÷ 26시간/주 = 2주

ROI (6개월 기준):
- 총 절감: 26시간 × 24주 = 624시간
- 투자: 52시간
- ROI = (624 - 52) / 52 × 100 = 1,100%
```

#### 3.2.3 리스크 대비 효과

| 리스크 시나리오 | 현재 비용 | OM 도입 후 | 절감 |
|----------------|----------|-----------|------|
| 핵심 개발자 이직 | 온보딩 3주 | 온보딩 5일 | 10일 |
| 잘못된 스키마 변경 | 롤백+복구 8시간 | 사전 감지 1시간 | 7시간 |
| AI 오답 원인 분석 | 디버깅 4시간 | Lineage 추적 30분 | 3.5시간 |
| 감사 대응 (보험업) | 문서 작성 16시간 | 자동 리포트 2시간 | 14시간 |

---

## 4. 단계별 실행 로드맵

### 4.1 전체 로드맵 개요

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        OpenMetadata 도입 로드맵                           │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Phase 0          Phase 1           Phase 2          Phase 3             │
│  (즉시)           (1주차)           (2-3주차)        (4주차~)            │
│  ┌─────┐         ┌─────┐           ┌─────┐          ┌─────┐             │
│  │ DoD │ ──────▶ │인프라│ ────────▶│인제션│ ───────▶│연동 │             │
│  │체크 │         │구축  │           │파이프│          │자동화│             │
│  │리스트│         │      │           │라인  │          │      │             │
│  └─────┘         └─────┘           └─────┘          └─────┘             │
│     │               │                 │                │                 │
│     ▼               ▼                 ▼                ▼                 │
│  프로세스        기술 기반          데이터 가시성    완전 자동화          │
│  개선            확보              확보             달성                 │
│                                                                          │
│  비용: 0원       비용: 1일         비용: 2-3일      비용: 2-3일          │
│  효과: 즉시      효과: 기반        효과: 중간       효과: 최대           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Phase 0: DoD 체크리스트 적용 (즉시 적용 가능)

**목표**: 메타데이터 의식 문화 정착

#### 4.2.1 스프린트 완료 정의(DoD) 확장

```markdown
## 기존 DoD
- [ ] 코드 리뷰 완료
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] 문서화 완료

## 메타데이터 DoD 추가 항목
- [ ] 새로 생성된 테이블/컬럼에 설명(Description) 작성
- [ ] 데이터 변경 시 영향받는 기능 목록 문서화
- [ ] 스토리 ID를 관련 테이블 주석에 기록
- [ ] AI 학습 데이터 변경 시 버전 태그 부여
```

#### 4.2.2 SQL 주석 표준 템플릿

```sql
-- OpenMetadata 연동을 위한 PostgreSQL 주석 표준

-- 테이블 주석
COMMENT ON TABLE project.requirements IS 
'RFP에서 추출된 요구사항 정보
@story: STORY-001, STORY-002
@owner: backend-team
@pii: false
@tier: Tier1';

-- 컬럼 주석
COMMENT ON COLUMN project.requirements.requirement_code IS 
'요구사항 고유 코드 (형식: REQ-{프로젝트코드}-{카테고리}-{순번})
@example: REQ-PMS-FUNC-001
@format: REQ-[A-Z]{3}-[A-Z]{4,5}-[0-9]{3}';
```

#### 4.2.3 Neo4j 노드 속성 표준

```cypher
-- Neo4j 노드 메타데이터 속성 표준
(:Requirement {
    id: String,
    // ... 기존 속성 ...
    
    // 메타데이터 속성 추가
    _story_id: "STORY-123",           // 연결된 유저 스토리
    _sprint_id: "SPRINT-2026-W03",    // 생성된 스프린트
    _created_by: "developer@pms.com", // 생성자
    _version: "1.0.0",                // 데이터 버전
    _data_quality_score: 0.95         // 품질 점수
})
```

**Phase 0 효과**:
- 비용: 0원 (프로세스 변경만)
- 시간: 2시간 (팀 미팅)
- 효과: 메타데이터 문화 기반 확립

---

### 4.3 Phase 1: OpenMetadata 인프라 구축 (1주차)

**목표**: OpenMetadata 서버 구동 및 기본 설정

#### 4.3.1 Docker Compose 확장

```yaml
# docker-compose.openmetadata.yml
version: '3.8'

services:
  # OpenMetadata 전용 MySQL (기존 PostgreSQL과 별도)
  openmetadata-mysql:
    image: mysql:8.0
    container_name: pms-openmetadata-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${OM_MYSQL_ROOT_PASSWORD:-openmetadata_password}
      MYSQL_DATABASE: openmetadata_db
      MYSQL_USER: openmetadata_user
      MYSQL_PASSWORD: ${OM_MYSQL_PASSWORD:-openmetadata_password}
    volumes:
      - openmetadata_mysql_data:/var/lib/mysql
    networks:
      - pms-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Elasticsearch (메타데이터 검색용)
  openmetadata-elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.15
    container_name: pms-openmetadata-elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - xpack.security.enabled=false
    volumes:
      - openmetadata_es_data:/usr/share/elasticsearch/data
    networks:
      - pms-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  # OpenMetadata Server
  openmetadata-server:
    image: openmetadata/server:1.3.1
    container_name: pms-openmetadata-server
    environment:
      # Database Configuration
      DB_HOST: openmetadata-mysql
      DB_PORT: 3306
      DB_USER: openmetadata_user
      DB_USER_PASSWORD: ${OM_MYSQL_PASSWORD:-openmetadata_password}
      DB_DRIVER_CLASS: com.mysql.cj.jdbc.Driver
      DB_SCHEME: mysql
      DB_USE_SSL: "false"
      OM_DATABASE: openmetadata_db
      
      # Elasticsearch Configuration
      ELASTICSEARCH_HOST: openmetadata-elasticsearch
      ELASTICSEARCH_PORT: 9200
      ELASTICSEARCH_SCHEME: http
      
      # Authentication
      AUTHENTICATION_PROVIDER: basic
      AUTHENTICATION_PUBLIC_KEYS: '["/openmetadata/conf/public_key.der"]'
      AUTHENTICATION_AUTHORITY: "http://localhost:8585/api/v1/system/config/jwks"
      AUTHENTICATION_CLIENT_ID: "openmetadata"
      
      # Airflow (Optional - 나중에 활성화)
      # AIRFLOW_HOST: openmetadata-airflow
      
      # Server Configuration
      SERVER_HOST_API_URL: http://localhost:8585/api
      
    ports:
      - "8585:8585"
    depends_on:
      openmetadata-mysql:
        condition: service_healthy
      openmetadata-elasticsearch:
        condition: service_healthy
    networks:
      - pms-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8585/api/v1/system/version"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s

  # OpenMetadata Ingestion (Python 기반 데이터 수집)
  openmetadata-ingestion:
    image: openmetadata/ingestion:1.3.1
    container_name: pms-openmetadata-ingestion
    environment:
      AIRFLOW__CORE__EXECUTOR: LocalExecutor
      AIRFLOW__DATABASE__SQL_ALCHEMY_CONN: mysql+pymysql://openmetadata_user:${OM_MYSQL_PASSWORD:-openmetadata_password}@openmetadata-mysql:3306/openmetadata_db
      OPENMETADATA_SERVER_HOST: openmetadata-server
      OPENMETADATA_SERVER_PORT: 8585
    depends_on:
      openmetadata-server:
        condition: service_healthy
    volumes:
      - openmetadata_ingestion_data:/opt/airflow
      - ./openmetadata/ingestion:/opt/airflow/dags  # 커스텀 인제스션 스크립트
    networks:
      - pms-network
    ports:
      - "8586:8080"

volumes:
  openmetadata_mysql_data:
    driver: local
  openmetadata_es_data:
    driver: local
  openmetadata_ingestion_data:
    driver: local
```

#### 4.3.2 시작 스크립트

```bash
#!/bin/bash
# scripts/start-openmetadata.sh

echo "🚀 Starting OpenMetadata stack..."

# 기존 PMS 서비스가 실행 중인지 확인
if ! docker-compose ps | grep -q "pms-postgres.*Up"; then
    echo "⚠️  PMS core services not running. Starting them first..."
    docker-compose up -d postgres redis neo4j
    sleep 30
fi

# OpenMetadata 스택 시작
docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml up -d \
    openmetadata-mysql \
    openmetadata-elasticsearch \
    openmetadata-server \
    openmetadata-ingestion

echo "⏳ Waiting for OpenMetadata to be ready..."
sleep 60

# Health check
if curl -s http://localhost:8585/api/v1/system/version | grep -q "version"; then
    echo "✅ OpenMetadata is ready!"
    echo "📊 Access UI: http://localhost:8585"
    echo "🔑 Default credentials: admin / admin"
else
    echo "❌ OpenMetadata failed to start. Check logs:"
    echo "   docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml logs openmetadata-server"
fi
```

#### 4.3.3 하드웨어 요구사항

| 환경 | 총 RAM | CPU | 디스크 |
|------|--------|-----|--------|
| **개발/테스트** | 16GB | 4 cores | 50GB |
| **스테이징** | 24GB | 6 cores | 100GB |
| **프로덕션** | 32GB+ | 8+ cores | 200GB+ |

현재 시스템 + OpenMetadata 예상 리소스:
```
현재 PMS 스택:        ~6GB RAM
OpenMetadata 추가:    +10GB RAM
──────────────────────────────
권장 총 RAM:          16GB (개발), 24GB (운영)
```

---

### 4.4 Phase 2: 데이터 인제스션 파이프라인 (2-3주차)

**목표**: PostgreSQL, Neo4j 메타데이터 자동 수집

#### 4.4.1 PostgreSQL 커넥터 설정

```yaml
# openmetadata/ingestion/postgres_ingestion.yaml
source:
  type: postgres
  serviceName: pms-postgres
  serviceConnection:
    config:
      type: Postgres
      username: ${POSTGRES_USER}
      password: ${POSTGRES_PASSWORD}
      hostPort: postgres:5432
      database: pms_db
      # 스키마 필터 (프로젝트 관련 스키마만)
      schemaFilterPattern:
        includes:
          - auth
          - project
          - task
          - chat
          - report
          - risk
  sourceConfig:
    config:
      type: DatabaseMetadata
      markDeletedTables: true
      includeTables: true
      includeViews: true
      # 테이블 주석 자동 수집
      includeComments: true
      # 프로파일링 (데이터 통계)
      generateSampleData: true

sink:
  type: metadata-rest
  config: {}

workflowConfig:
  openMetadataServerConfig:
    hostPort: http://openmetadata-server:8585/api
    authProvider: openmetadata
    securityConfig:
      jwtToken: ${OM_JWT_TOKEN}
```

#### 4.4.2 Neo4j 커넥터 (커스텀)

OpenMetadata는 Neo4j 네이티브 커넥터가 없으므로 커스텀 인제스션 스크립트 작성:

```python
# openmetadata/ingestion/neo4j_custom_ingestion.py
"""
Neo4j 메타데이터를 OpenMetadata로 인제스션하는 커스텀 스크립트
"""

from neo4j import GraphDatabase
from metadata.ingestion.api.source import Source, SourceStatus
from metadata.generated.schema.entity.data.database import Database
from metadata.generated.schema.entity.data.databaseSchema import DatabaseSchema
from metadata.generated.schema.entity.data.table import Table, Column
from metadata.generated.schema.type.entityReference import EntityReference
import os

class Neo4jMetadataSource:
    def __init__(self):
        self.driver = GraphDatabase.driver(
            os.getenv("NEO4J_URI", "bolt://neo4j:7687"),
            auth=(
                os.getenv("NEO4J_USER", "neo4j"),
                os.getenv("NEO4J_PASSWORD", "pmspassword123")
            )
        )
        
    def get_node_labels(self) -> list:
        """모든 노드 레이블(=테이블에 해당) 조회"""
        with self.driver.session() as session:
            result = session.run("CALL db.labels()")
            return [record["label"] for record in result]
    
    def get_node_properties(self, label: str) -> list:
        """특정 레이블의 속성(=컬럼에 해당) 조회"""
        with self.driver.session() as session:
            query = f"""
            MATCH (n:{label})
            WITH n LIMIT 100
            UNWIND keys(n) as key
            RETURN DISTINCT key as property,
                   apoc.meta.type(n[key]) as type
            """
            result = session.run(query)
            return [{"name": r["property"], "type": r["type"]} for r in result]
    
    def get_relationships(self) -> list:
        """관계 타입(=테이블 간 FK에 해당) 조회"""
        with self.driver.session() as session:
            result = session.run("CALL db.relationshipTypes()")
            return [record["relationshipType"] for record in result]
    
    def get_lineage_info(self, label: str) -> dict:
        """노드 간 계보 정보 추출"""
        with self.driver.session() as session:
            # 예: Document -> Chunk -> Embedding 관계
            query = f"""
            MATCH (source:{label})-[r]->(target)
            RETURN DISTINCT type(r) as relationship,
                   labels(target)[0] as target_label
            """
            result = session.run(query)
            return {
                "downstream": [
                    {"relationship": r["relationship"], "target": r["target_label"]}
                    for r in result
                ]
            }
    
    def generate_openmetadata_payload(self) -> dict:
        """OpenMetadata API 형식으로 변환"""
        labels = self.get_node_labels()
        tables = []
        
        for label in labels:
            properties = self.get_node_properties(label)
            lineage = self.get_lineage_info(label)
            
            table = {
                "name": label,
                "displayName": f"Neo4j:{label}",
                "description": f"Graph node label: {label}",
                "tableType": "Regular",
                "columns": [
                    {
                        "name": prop["name"],
                        "dataType": self._map_neo4j_type(prop["type"]),
                        "description": f"Property: {prop['name']}"
                    }
                    for prop in properties
                ],
                "tags": [
                    {"tagFQN": "GraphDB.Neo4j"},
                    {"tagFQN": f"PMS.{label}"}
                ],
                "customProperties": {
                    "graphDatabase": "Neo4j",
                    "nodeLabel": label,
                    "lineage": lineage
                }
            }
            tables.append(table)
        
        return {"tables": tables}
    
    def _map_neo4j_type(self, neo4j_type: str) -> str:
        """Neo4j 타입을 OpenMetadata 타입으로 매핑"""
        type_mapping = {
            "STRING": "STRING",
            "INTEGER": "INT",
            "FLOAT": "FLOAT",
            "BOOLEAN": "BOOLEAN",
            "LIST": "ARRAY",
            "DATE": "DATE",
            "DATETIME": "DATETIME"
        }
        return type_mapping.get(neo4j_type, "STRING")


# 실행 예제
if __name__ == "__main__":
    source = Neo4jMetadataSource()
    payload = source.generate_openmetadata_payload()
    
    # OpenMetadata API로 전송
    import requests
    
    om_url = os.getenv("OPENMETADATA_URL", "http://openmetadata-server:8585")
    token = os.getenv("OM_JWT_TOKEN")
    
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    for table in payload["tables"]:
        response = requests.post(
            f"{om_url}/api/v1/tables",
            json=table,
            headers=headers
        )
        print(f"Created table {table['name']}: {response.status_code}")
```

#### 4.4.3 인제스션 스케줄

```yaml
# 인제스션 스케줄 설정
schedules:
  postgres_daily:
    cron: "0 2 * * *"  # 매일 새벽 2시
    source: postgres_ingestion.yaml
    
  neo4j_daily:
    cron: "0 3 * * *"  # 매일 새벽 3시
    source: neo4j_custom_ingestion.py
    
  on_schema_change:
    trigger: webhook  # CI/CD 파이프라인에서 트리거
    source: postgres_ingestion.yaml
```

---

### 4.5 Phase 3: PMS 연동 자동화 (4주차~)

**목표**: 스크럼 데이터와 메타데이터 자동 연결

#### 4.5.1 PMS-OpenMetadata 연동 Python 스크립트

```python
# llm-service/openmetadata_integration.py
"""
PMS 스크럼 데이터를 OpenMetadata에 자동 태깅하는 통합 스크립트
"""

import os
import requests
from dataclasses import dataclass
from typing import List, Optional
import psycopg2
from datetime import datetime


@dataclass
class UserStory:
    id: str
    project_id: str
    title: str
    status: str
    sprint_id: Optional[str]
    assignee_id: Optional[str]


@dataclass
class Task:
    id: str
    title: str
    status: str
    story_id: Optional[str]
    assignee_id: Optional[str]


class PMSOpenMetadataIntegration:
    """PMS와 OpenMetadata 간 데이터 동기화"""
    
    def __init__(self):
        # PMS PostgreSQL 연결
        self.pms_conn = psycopg2.connect(
            host=os.getenv("POSTGRES_HOST", "postgres"),
            port=os.getenv("POSTGRES_PORT", "5432"),
            database=os.getenv("POSTGRES_DB", "pms_db"),
            user=os.getenv("POSTGRES_USER", "pms_user"),
            password=os.getenv("POSTGRES_PASSWORD", "pms_password")
        )
        
        # OpenMetadata API 설정
        self.om_base_url = os.getenv("OPENMETADATA_URL", "http://openmetadata-server:8585")
        self.om_token = os.getenv("OM_JWT_TOKEN")
        self.om_headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }
    
    # =========================================
    # 1. 유저 스토리 → 테이블 태깅
    # =========================================
    
    def get_active_user_stories(self, project_id: str) -> List[UserStory]:
        """활성 상태의 유저 스토리 조회"""
        cursor = self.pms_conn.cursor()
        cursor.execute("""
            SELECT id, project_id, title, status, sprint_id, assignee_id
            FROM task.user_stories
            WHERE project_id = %s
              AND status NOT IN ('DONE', 'CANCELLED')
        """, (project_id,))
        
        return [
            UserStory(
                id=row[0], project_id=row[1], title=row[2],
                status=row[3], sprint_id=row[4], assignee_id=row[5]
            )
            for row in cursor.fetchall()
        ]
    
    def tag_table_with_story(self, table_fqn: str, story: UserStory):
        """
        테이블에 유저 스토리 정보 태깅
        
        Args:
            table_fqn: 테이블 FQN (예: pms-postgres.pms_db.project.requirements)
            story: 유저 스토리 정보
        """
        # 태그 생성/조회
        tag_fqn = f"PMS.Story.{story.id}"
        
        # 태그가 없으면 생성
        self._ensure_tag_exists(tag_fqn, story.title)
        
        # 테이블에 태그 추가
        response = requests.put(
            f"{self.om_base_url}/api/v1/tables/name/{table_fqn}/tags",
            json=[{
                "tagFQN": tag_fqn,
                "labelType": "Manual",
                "state": "Confirmed"
            }],
            headers=self.om_headers
        )
        
        if response.status_code == 200:
            print(f"✅ Tagged {table_fqn} with {tag_fqn}")
        else:
            print(f"❌ Failed to tag: {response.text}")
    
    def _ensure_tag_exists(self, tag_fqn: str, description: str):
        """태그가 존재하지 않으면 생성"""
        # PMS 태그 카테고리 확인/생성
        category_name = "PMS"
        self._ensure_tag_category(category_name)
        
        # 스토리 하위 카테고리 확인/생성
        story_category = "Story"
        
        # 태그 생성
        tag_name = tag_fqn.split(".")[-1]
        tag_data = {
            "name": tag_name,
            "description": description,
            "classification": category_name
        }
        
        response = requests.post(
            f"{self.om_base_url}/api/v1/tags",
            json=tag_data,
            headers=self.om_headers
        )
        
        if response.status_code in [200, 201, 409]:  # 409 = 이미 존재
            return True
        return False
    
    def _ensure_tag_category(self, category_name: str):
        """태그 분류 확인/생성"""
        classification_data = {
            "name": category_name,
            "description": "PMS 프로젝트 관리 메타데이터"
        }
        
        requests.put(
            f"{self.om_base_url}/api/v1/classifications",
            json=classification_data,
            headers=self.om_headers
        )
    
    # =========================================
    # 2. 스프린트 → 테이블 커스텀 속성
    # =========================================
    
    def add_sprint_metadata(self, table_fqn: str, sprint_id: str):
        """테이블에 스프린트 메타데이터 추가"""
        cursor = self.pms_conn.cursor()
        cursor.execute("""
            SELECT id, name, status, start_date, end_date
            FROM task.sprints
            WHERE id = %s
        """, (sprint_id,))
        
        sprint = cursor.fetchone()
        if not sprint:
            return
        
        # 커스텀 속성 업데이트
        extension_data = {
            "sprint_id": sprint[0],
            "sprint_name": sprint[1],
            "sprint_status": sprint[2],
            "sprint_start": str(sprint[3]) if sprint[3] else None,
            "sprint_end": str(sprint[4]) if sprint[4] else None,
            "last_synced": datetime.now().isoformat()
        }
        
        response = requests.patch(
            f"{self.om_base_url}/api/v1/tables/name/{table_fqn}",
            json={"extension": extension_data},
            headers=self.om_headers
        )
        
        if response.status_code == 200:
            print(f"✅ Added sprint metadata to {table_fqn}")
    
    # =========================================
    # 3. 담당자 → 테이블 소유자 설정
    # =========================================
    
    def set_table_owner(self, table_fqn: str, assignee_id: str):
        """테이블 소유자를 담당 개발자로 설정"""
        cursor = self.pms_conn.cursor()
        cursor.execute("""
            SELECT id, email, full_name
            FROM auth.users
            WHERE id = %s
        """, (assignee_id,))
        
        user = cursor.fetchone()
        if not user:
            return
        
        # OpenMetadata에 사용자가 있는지 확인
        user_email = user[1]
        om_user = self._get_or_create_om_user(user_email, user[2])
        
        if om_user:
            # 테이블 소유자 설정
            response = requests.patch(
                f"{self.om_base_url}/api/v1/tables/name/{table_fqn}",
                json={"owner": {"id": om_user["id"], "type": "user"}},
                headers=self.om_headers
            )
            
            if response.status_code == 200:
                print(f"✅ Set owner of {table_fqn} to {user_email}")
    
    def _get_or_create_om_user(self, email: str, display_name: str) -> Optional[dict]:
        """OpenMetadata 사용자 조회/생성"""
        # 조회
        response = requests.get(
            f"{self.om_base_url}/api/v1/users/name/{email.replace('@', '_at_')}",
            headers=self.om_headers
        )
        
        if response.status_code == 200:
            return response.json()
        
        # 생성
        user_data = {
            "name": email.replace("@", "_at_"),
            "email": email,
            "displayName": display_name
        }
        
        response = requests.post(
            f"{self.om_base_url}/api/v1/users",
            json=user_data,
            headers=self.om_headers
        )
        
        if response.status_code in [200, 201]:
            return response.json()
        return None
    
    # =========================================
    # 4. 비즈니스 글로서리 자동 생성
    # =========================================
    
    def sync_domain_glossary(self):
        """PMS 도메인 용어를 비즈니스 글로서리로 동기화"""
        
        # 보험 도메인 용어 정의 (추후 DB 테이블로 관리)
        glossary_terms = [
            {
                "name": "KCD_CODE",
                "displayName": "한국표준질병사인분류",
                "description": "Korean Standard Classification of Diseases. 의료 보험 심사에서 질병/상해를 분류하는 표준 코드 체계.",
                "synonyms": ["KCD", "질병코드", "상병코드"],
                "relatedTerms": ["ICD-10", "보험심사"]
            },
            {
                "name": "RFP",
                "displayName": "제안요청서",
                "description": "Request for Proposal. 프로젝트 발주 시 요구사항을 정의한 공식 문서.",
                "synonyms": ["제안요청서", "요구사항문서"],
                "relatedTerms": ["요구사항", "스프린트"]
            },
            {
                "name": "REQUIREMENT",
                "displayName": "요구사항",
                "description": "RFP에서 추출된 개별 기능/비기능 요구사항. 스프린트에 매핑되어 태스크로 분해됨.",
                "synonyms": ["기능요구", "스펙"],
                "relatedTerms": ["RFP", "태스크", "스프린트"]
            },
            {
                "name": "SPRINT",
                "displayName": "스프린트",
                "description": "2-4주 단위의 개발 반복 주기. 백로그에서 선택된 작업을 완료하는 타임박스.",
                "synonyms": ["이터레이션", "반복주기"],
                "relatedTerms": ["백로그", "스크럼", "유저스토리"]
            }
        ]
        
        # 글로서리 생성
        glossary_name = "PMS_Insurance_Domain"
        self._ensure_glossary(glossary_name, "PMS 보험 심사 도메인 용어집")
        
        for term in glossary_terms:
            self._create_glossary_term(glossary_name, term)
    
    def _ensure_glossary(self, name: str, description: str):
        """글로서리 확인/생성"""
        glossary_data = {
            "name": name,
            "displayName": "PMS 보험 도메인 용어집",
            "description": description
        }
        
        response = requests.put(
            f"{self.om_base_url}/api/v1/glossaries",
            json=glossary_data,
            headers=self.om_headers
        )
        return response.status_code in [200, 201]
    
    def _create_glossary_term(self, glossary_name: str, term: dict):
        """글로서리 용어 생성"""
        term_data = {
            "name": term["name"],
            "displayName": term["displayName"],
            "description": term["description"],
            "synonyms": term.get("synonyms", []),
            "glossary": glossary_name
        }
        
        response = requests.put(
            f"{self.om_base_url}/api/v1/glossaryTerms",
            json=term_data,
            headers=self.om_headers
        )
        
        if response.status_code in [200, 201]:
            print(f"✅ Created glossary term: {term['name']}")
    
    # =========================================
    # 5. 전체 동기화 실행
    # =========================================
    
    def full_sync(self, project_id: str):
        """
        프로젝트의 전체 메타데이터 동기화
        
        CI/CD 파이프라인 또는 스프린트 종료 시 실행
        """
        print(f"🔄 Starting full sync for project: {project_id}")
        
        # 1. 비즈니스 글로서리 동기화
        print("📖 Syncing domain glossary...")
        self.sync_domain_glossary()
        
        # 2. 유저 스토리 태깅
        print("🏷️ Tagging tables with user stories...")
        stories = self.get_active_user_stories(project_id)
        
        for story in stories:
            # 스토리와 관련된 테이블 찾기 (주석 기반 또는 매핑 테이블)
            related_tables = self._find_related_tables(story.id)
            
            for table_fqn in related_tables:
                self.tag_table_with_story(table_fqn, story)
                
                if story.sprint_id:
                    self.add_sprint_metadata(table_fqn, story.sprint_id)
                
                if story.assignee_id:
                    self.set_table_owner(table_fqn, story.assignee_id)
        
        print("✅ Full sync completed!")
    
    def _find_related_tables(self, story_id: str) -> List[str]:
        """
        스토리와 관련된 테이블 FQN 조회
        
        방법 1: 테이블 주석에서 @story 태그 파싱
        방법 2: 별도의 매핑 테이블 사용
        """
        cursor = self.pms_conn.cursor()
        
        # 방법: 테이블 주석에서 스토리 ID 검색
        cursor.execute("""
            SELECT 
                table_schema || '.' || table_name as table_fqn
            FROM information_schema.tables t
            JOIN pg_description d ON d.objoid = (
                SELECT oid FROM pg_class WHERE relname = t.table_name
            )
            WHERE d.description LIKE %s
              AND table_schema IN ('auth', 'project', 'task', 'chat', 'report')
        """, (f'%@story:%{story_id}%',))
        
        return [
            f"pms-postgres.pms_db.{row[0]}"
            for row in cursor.fetchall()
        ]


# CLI 실행
if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="PMS-OpenMetadata Integration")
    parser.add_argument("--project-id", required=True, help="Project ID to sync")
    parser.add_argument("--action", choices=["full", "glossary", "stories"], default="full")
    
    args = parser.parse_args()
    
    integration = PMSOpenMetadataIntegration()
    
    if args.action == "full":
        integration.full_sync(args.project_id)
    elif args.action == "glossary":
        integration.sync_domain_glossary()
    elif args.action == "stories":
        stories = integration.get_active_user_stories(args.project_id)
        print(f"Found {len(stories)} active stories")
```

#### 4.5.2 CI/CD 파이프라인 통합

```yaml
# .github/workflows/metadata-sync.yml
name: Metadata Sync

on:
  push:
    paths:
      - 'docker/postgres/init/**'
      - 'PMS_IC_BackEnd_v1.2/**/entity/**'
  workflow_dispatch:
    inputs:
      project_id:
        description: 'Project ID to sync'
        required: true

jobs:
  sync-metadata:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      
      - name: Install dependencies
        run: |
          pip install psycopg2-binary requests
      
      - name: Trigger PostgreSQL Ingestion
        run: |
          curl -X POST \
            "${{ secrets.OPENMETADATA_URL }}/api/v1/services/ingestionPipelines/trigger" \
            -H "Authorization: Bearer ${{ secrets.OM_JWT_TOKEN }}" \
            -H "Content-Type: application/json" \
            -d '{"name": "pms-postgres_metadata"}'
      
      - name: Sync PMS Metadata
        env:
          POSTGRES_HOST: ${{ secrets.POSTGRES_HOST }}
          POSTGRES_PASSWORD: ${{ secrets.POSTGRES_PASSWORD }}
          OPENMETADATA_URL: ${{ secrets.OPENMETADATA_URL }}
          OM_JWT_TOKEN: ${{ secrets.OM_JWT_TOKEN }}
        run: |
          python llm-service/openmetadata_integration.py \
            --project-id ${{ github.event.inputs.project_id || 'default' }} \
            --action full
```

---

### 4.6 Phase 4: 고급 기능 (선택적, 6주차~)

**목표**: 메타데이터 봇, 알림 연동

#### 4.6.1 Slack/Teams 알림 봇

```python
# llm-service/metadata_bot.py
"""
메타데이터 변경 알림 봇

스키마 변경 시 Slack/Teams로 알림 발송
"""

import os
import requests
from datetime import datetime, timedelta


class MetadataChangeBot:
    def __init__(self):
        self.om_url = os.getenv("OPENMETADATA_URL")
        self.om_token = os.getenv("OM_JWT_TOKEN")
        self.slack_webhook = os.getenv("SLACK_WEBHOOK_URL")
        
    def check_recent_changes(self, hours: int = 24):
        """최근 N시간 내 스키마 변경 조회"""
        since = datetime.now() - timedelta(hours=hours)
        
        response = requests.get(
            f"{self.om_url}/api/v1/feed",
            params={
                "entityType": "table",
                "after": since.isoformat()
            },
            headers={
                "Authorization": f"Bearer {self.om_token}"
            }
        )
        
        if response.status_code == 200:
            return response.json().get("data", [])
        return []
    
    def send_slack_notification(self, changes: list):
        """Slack으로 변경 알림 발송"""
        if not changes:
            return
        
        # 변경 내용 포맷팅
        blocks = [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "text": "📊 데이터 스키마 변경 알림"
                }
            },
            {"type": "divider"}
        ]
        
        for change in changes[:10]:  # 최대 10개
            entity_name = change.get("entityRef", {}).get("name", "Unknown")
            change_type = change.get("changeType", "Unknown")
            user = change.get("updatedBy", "Unknown")
            
            # 연결된 스토리 ID 찾기
            story_ids = self._find_linked_stories(entity_name)
            story_text = f"\n📋 연결 스토리: {', '.join(story_ids)}" if story_ids else ""
            
            blocks.append({
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*{entity_name}*\n변경 유형: {change_type}\n담당자: {user}{story_text}"
                }
            })
        
        # Slack 전송
        requests.post(
            self.slack_webhook,
            json={"blocks": blocks}
        )
    
    def _find_linked_stories(self, table_name: str) -> list:
        """테이블에 연결된 스토리 ID 조회"""
        response = requests.get(
            f"{self.om_url}/api/v1/tables/name/pms-postgres.pms_db.{table_name}",
            headers={"Authorization": f"Bearer {self.om_token}"}
        )
        
        if response.status_code == 200:
            tags = response.json().get("tags", [])
            return [
                tag["tagFQN"].split(".")[-1]
                for tag in tags
                if tag["tagFQN"].startswith("PMS.Story.")
            ]
        return []


# 스케줄러로 실행 (cron 또는 Airflow)
if __name__ == "__main__":
    bot = MetadataChangeBot()
    changes = bot.check_recent_changes(hours=24)
    bot.send_slack_notification(changes)
```

#### 4.6.2 일일 스탠드업 통합 알림

```python
# 일일 스탠드업 시간에 발송되는 요약 알림

DAILY_STANDUP_MESSAGE = """
## 📊 데이터 메타데이터 일일 요약

### 어제 변경된 테이블
{changed_tables}

### 영향받는 유저 스토리
{affected_stories}

### 주의 필요 항목
{warnings}

---
🔗 [OpenMetadata에서 상세 보기]({om_url}/explore/tables)
"""
```

---

## 5. 측정 및 평가 체계

### 5.1 핵심 성과 지표 (KPI)

| 지표 | 측정 방법 | 목표값 | 측정 주기 |
|------|----------|--------|----------|
| **데이터 탐색 시간** | 개발자 설문 | 80% 단축 | 스프린트마다 |
| **온보딩 기간** | 신규 멤버 첫 PR까지 시간 | 5일 이내 | 신규 입사 시 |
| **스키마 변경 사고** | 롤백 발생 건수 | 월 0건 | 월간 |
| **메타데이터 커버리지** | 설명 있는 테이블/컬럼 비율 | 90% | 주간 |
| **태깅 완료율** | 스토리 ID 태그 비율 | 100% | 스프린트마다 |

### 5.2 스프린트 회고 체크리스트

```markdown
## OpenMetadata 도입 효과 회고 (스프린트 N)

### 정량적 평가
- [ ] 이번 스프린트 데이터 탐색에 소요된 시간: ___ 시간
- [ ] 새로 생성된 테이블 수: ___
- [ ] 메타데이터 설명 작성 완료: ___% 
- [ ] 스토리 ID 태깅 완료: ___개

### 정성적 평가
- [ ] OpenMetadata가 작업에 도움이 되었나요? (1-5)
- [ ] 개선이 필요한 부분이 있나요?
- [ ] 추가로 필요한 메타데이터 정보가 있나요?

### 다음 스프린트 액션 아이템
- [ ] ...
```

### 5.3 6개월 후 전면 확대 결정 기준

| 평가 항목 | 성공 기준 | 현재 상태 |
|----------|----------|----------|
| ROI | 500% 이상 | 측정 예정 |
| 팀 만족도 | 4.0/5.0 이상 | 측정 예정 |
| 메타데이터 커버리지 | 80% 이상 | 측정 예정 |
| 사고 감소율 | 50% 이상 | 측정 예정 |

**전면 확대 시 범위**:
- Neo4j GraphRAG 데이터 전체 카탈로그화
- LangGraph 워크플로우 계보 추적
- AI 학습 데이터셋 버전 관리

---

## 6. 리스크 및 완화 방안

| 리스크 | 영향도 | 발생 확률 | 완화 방안 |
|--------|--------|----------|----------|
| 초기 학습 부담 | 중간 | 높음 | Phase 0(DoD)로 점진적 적응 |
| 인프라 리소스 부족 | 높음 | 중간 | 최소 사양으로 시작, 모니터링 |
| 자동화 스크립트 오류 | 중간 | 중간 | 단계별 검증, 롤백 계획 |
| 팀 저항 | 높음 | 낮음 | 효과 시연, 작은 성공 경험 |
| OpenMetadata 버전 업그레이드 | 낮음 | 낮음 | LTS 버전 사용, 업그레이드 가이드 참조 |

---

## 7. 다음 단계

### 즉시 실행 가능 (이번 주)
1. ✅ DoD 체크리스트 팀 공유 및 적용
2. ✅ 테이블 주석 표준 템플릿 배포

### 1주차
3. Docker Compose OpenMetadata 확장 파일 적용
4. 기본 인제스션 테스트

### 2-3주차
5. PostgreSQL/Neo4j 인제스션 파이프라인 구축
6. 비즈니스 글로서리 초기 데이터 입력

### 4주차 이후
7. PMS 연동 Python 스크립트 배포
8. CI/CD 파이프라인 통합
9. 파일럿 운영 시작

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-17 | AI Assistant | 초안 작성 |
