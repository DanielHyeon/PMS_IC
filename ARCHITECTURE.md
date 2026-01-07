# PMS Insurance Claims - 상세 아키텍처 문서

## 📑 목차

1. [시스템 개요](#시스템-개요)
2. [전체 아키텍처](#전체-아키텍처)
3. [컴포넌트 상세](#컴포넌트-상세)
4. [데이터 모델](#데이터-모델)
5. [API 설계](#api-설계)
6. [보안 아키텍처](#보안-아키텍처)
7. [배포 아키텍처](#배포-아키텍처)
8. [성능 최적화](#성능-최적화)

---

## 시스템 개요

### 시스템 목적

PMS Insurance Claims는 보험 심사 프로젝트의 전주기 관리를 위한 AI 통합 플랫폼입니다. Neo4j GraphRAG 기반의 지능형 챗봇을 통해 프로젝트 관리 의사결정을 지원합니다.

### 핵심 설계 원칙

- **마이크로서비스 지향**: 각 서비스는 독립적으로 배포 및 확장 가능
- **AI 우선**: LLM과 RAG를 핵심 기능으로 통합
- **보안 강화**: JWT 기반 인증, 환경변수 기반 시크릿 관리
- **확장성**: 컨테이너 기반 수평 확장 지원
- **관찰성**: 구조화된 로깅, 헬스체크, 메트릭 수집

### 기술 결정 사항

| 항목 | 선택 | 이유 |
|------|------|------|
| Backend | Spring Boot | 엔터프라이즈급 안정성, 풍부한 생태계 |
| Frontend | React | 컴포넌트 기반, 대규모 커뮤니티 |
| LLM | Gemma 3 12B | 로컬 배포 가능, 한국어 지원 우수 |
| RAG | Neo4j GraphRAG | 벡터 + 그래프 하이브리드 검색 |
| Database | PostgreSQL | ACID 보장, JSON 지원 |
| Cache | Redis | 고성능, 세션 관리 지원 |

---

## 전체 아키텍처

### 논리적 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   Web UI    │  │   Mobile    │  │   API CLI   │     │
│  │  (React)    │  │ (Future)    │  │  (Future)   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/REST
┌────────────────────▼────────────────────────────────────┐
│                  Application Layer                       │
│  ┌─────────────────────────────────────────────────┐    │
│  │          Spring Boot Backend                    │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐     │    │
│  │  │ Project  │  │   Risk   │  │   Chat   │     │    │
│  │  │ Service  │  │ Service  │  │ Service  │     │    │
│  │  └──────────┘  └──────────┘  └──────────┘     │    │
│  └─────────────────────────────────────────────────┘    │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼─────┐ ┌───▼────┐ ┌─────▼──────┐
│ Data Layer  │ │ Cache  │ │ AI Service │
│             │ │ Layer  │ │            │
│ ┌─────────┐ │ │        │ │ ┌────────┐ │
│ │Postgres │ │ │ Redis  │ │ │ LLM    │ │
│ └─────────┘ │ │        │ │ │ +RAG   │ │
│             │ └────────┘ │ └────────┘ │
│ ┌─────────┐ │            │ ┌────────┐ │
│ │ Neo4j   │ │            │ │ GPU    │ │
│ └─────────┘ │            │ └────────┘ │
└─────────────┘            └────────────┘
```

### 물리적 아키텍처 (Docker Compose)

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Host                           │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Docker Network: pms-network            │  │
│  │                                                   │  │
│  │  ┌────────────┐  ┌────────────┐  ┌───────────┐  │  │
│  │  │  frontend  │  │  backend   │  │llm-service│  │  │
│  │  │  :5173     │  │  :8080     │  │  :8000    │  │  │
│  │  └─────┬──────┘  └─────┬──────┘  └─────┬─────┘  │  │
│  │        │                │                │        │  │
│  │  ┌─────▼──────┐  ┌─────▼──────┐  ┌─────▼─────┐  │  │
│  │  │  postgres  │  │   redis    │  │   neo4j   │  │  │
│  │  │   :5433    │  │   :6379    │  │   :7687   │  │  │
│  │  └────────────┘  └────────────┘  └───────────┘  │  │
│  │                                                   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Docker Volumes (Persistent)            │  │
│  │  • postgres_data                                 │  │
│  │  • redis_data                                    │  │
│  │  • neo4j_data                                    │  │
│  │  • models (host mount)                           │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 컴포넌트 상세

### 1. Frontend (React SPA)

**책임**: 사용자 인터페이스, 클라이언트 상태 관리

**주요 구성요소**:

```
src/
├── components/          # 재사용 가능한 UI 컴포넌트
│   ├── common/         # 공통 컴포넌트 (Button, Input, Modal)
│   ├── project/        # 프로젝트 관련 컴포넌트
│   ├── risk/           # 리스크 관련 컴포넌트
│   └── chat/           # 챗봇 UI 컴포넌트
├── pages/              # 페이지 컴포넌트
│   ├── Dashboard.jsx
│   ├── ProjectList.jsx
│   └── ChatPage.jsx
├── services/           # API 통신 레이어
│   ├── api.js         # Axios 인스턴스
│   ├── projectService.js
│   └── chatService.js
├── contexts/           # React Context (전역 상태)
│   ├── AuthContext.jsx
│   └── ThemeContext.jsx
└── utils/              # 유틸리티 함수
    ├── dateFormatter.js
    └── validators.js
```

**통신 패턴**:
- REST API: Axios를 통한 HTTP 통신
- WebSocket: 실시간 채팅 (향후 구현)
- 인증: JWT 토큰을 Authorization 헤더에 포함

### 2. Backend (Spring Boot)

**책임**: 비즈니스 로직, 데이터 접근, API 제공

**레이어 아키텍처**:

```
com.insuretech.pms/
├── auth/                      # 인증/인가 모듈
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   └── AuthService.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   └── dto/
│       ├── LoginRequest.java
│       └── TokenResponse.java
│
├── project/                   # 프로젝트 관리 모듈
│   ├── controller/
│   │   └── ProjectController.java
│   ├── service/
│   │   └── ProjectService.java
│   ├── repository/
│   │   └── ProjectRepository.java
│   ├── domain/
│   │   ├── Project.java
│   │   └── Task.java
│   └── dto/
│       ├── ProjectRequest.java
│       └── ProjectResponse.java
│
├── risk/                      # 리스크 관리 모듈
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── domain/
│
├── chat/                      # AI 챗봇 모듈
│   ├── controller/
│   │   └── ChatController.java
│   ├── service/
│   │   ├── AIChatClient.java  # LLM Service 통신
│   │   └── ChatSessionService.java
│   ├── repository/
│   │   └── ChatMessageRepository.java
│   └── domain/
│       ├── ChatSession.java
│       └── ChatMessage.java
│
└── common/                    # 공통 모듈
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── RedisConfig.java
    │   └── WebClientConfig.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── CustomException.java
    └── dto/
        └── ApiResponse.java
```

**핵심 설계 패턴**:

1. **Layered Architecture**: Controller → Service → Repository
2. **Dependency Injection**: Spring IoC 컨테이너 활용
3. **DTO Pattern**: 계층 간 데이터 전송 객체 사용
4. **Repository Pattern**: JPA 기반 데이터 접근 추상화

**주요 설정**:

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/pms_db
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
  redis:
    host: redis
    port: 6379

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24시간

ai:
  team:
    api-url: http://llm-service:8000
```

### 3. LLM Service (Flask + Python)

**책임**: AI 추론, RAG 검색, 문서 파싱

**아키텍처**:

```
llm-service/
├── app.py                      # Flask 애플리케이션
├── chat_workflow.py            # LangGraph 워크플로우
├── rag_service_neo4j.py        # Neo4j RAG 서비스
├── document_parser.py          # MinerU 문서 파서
├── pdf_ocr_pipeline.py         # PDF OCR 파이프라인
└── load_ragdata_pdfs_neo4j.py  # RAG 데이터 로더
```

**LangGraph 워크플로우**:

```python
# 워크플로우 구조
StateGraph:
  start → classify_intent → route_by_intent
                ↓
        ┌───────┼───────┐
        │       │       │
    casual   general  pms_query
        │       │       │
        └───────┼───────┘
                ↓
        perform_rag (조건부)
                ↓
        generate_response
                ↓
        post_process
                ↓
              end
```

**RAG 파이프라인**:

```
1. 문서 입력 (PDF)
   ↓
2. MinerU2.5 파싱
   - OCR 처리
   - 레이아웃 분석
   - 테이블/이미지 추출
   ↓
3. 청킹 (Chunking)
   - 의미 단위 분할
   - 컨텍스트 윈도우 최적화
   ↓
4. 임베딩 생성
   - multilingual-e5-large
   - 1024차원 벡터
   ↓
5. Neo4j 저장
   - Document 노드
   - Chunk 노드
   - 관계: HAS_CHUNK, NEXT_CHUNK
   - 벡터 인덱스 생성
```

**검색 전략**:

```python
# 하이브리드 검색
def search(query, top_k=3):
    # 1. 벡터 유사도 검색
    vector_results = neo4j.vector_search(
        query_embedding,
        similarity="cosine",
        limit=top_k
    )

    # 2. 그래프 확장 (선택적)
    if use_graph_expansion:
        expanded_results = expand_via_relationships(
            vector_results,
            max_depth=1
        )

    # 3. 재랭킹
    reranked = rerank_by_relevance(
        expanded_results,
        query
    )

    return reranked[:top_k]
```

### 4. 데이터베이스

#### PostgreSQL (관계형 데이터)

**주요 테이블**:

```sql
-- 사용자 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 프로젝트 테이블
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    budget DECIMAL(15, 2),
    status VARCHAR(50) NOT NULL,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 리스크 테이블
CREATE TABLE risks (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    probability INTEGER CHECK (probability BETWEEN 1 AND 5),
    impact INTEGER CHECK (impact BETWEEN 1 AND 5),
    mitigation_plan TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 채팅 세션 테이블
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 채팅 메시지 테이블
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES chat_sessions(id),
    role VARCHAR(20) NOT NULL,  -- 'user' or 'assistant'
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Neo4j (그래프 + 벡터 데이터)

**노드 및 관계**:

```cypher
// Document 노드
CREATE (:Document {
    doc_id: "ragdata_project_management",
    title: "프로젝트 관리 가이드",
    file_name: "project_management.pdf",
    category: "reference_document",
    created_at: "2026-01-07T00:00:00Z"
})

// Chunk 노드
CREATE (:Chunk {
    chunk_id: "chunk_001",
    content: "프로젝트 관리는...",
    position: 0,
    embedding: [0.123, 0.456, ...],  // 1024차원 벡터
    metadata: {
        page: 1,
        section: "Introduction"
    }
})

// 관계
(:Document)-[:HAS_CHUNK]->(:Chunk)
(:Chunk)-[:NEXT_CHUNK]->(:Chunk)
(:Chunk)-[:RELATED_TO]->(:Chunk)

// 벡터 인덱스
CREATE VECTOR INDEX chunk_embeddings
FOR (c:Chunk) ON c.embedding
OPTIONS {
    indexConfig: {
        `vector.dimensions`: 1024,
        `vector.similarity_function`: 'cosine'
    }
}
```

#### Redis (캐시 + 세션)

**사용 사례**:

```
1. 세션 스토어
   Key: "session:{session_id}"
   Value: JSON (user_id, roles, etc.)
   TTL: 24시간

2. API 응답 캐시
   Key: "cache:project:{project_id}"
   Value: JSON
   TTL: 5분

3. Rate Limiting
   Key: "rate_limit:{user_id}:{endpoint}"
   Value: Counter
   TTL: 1분
```

---

## API 설계

### RESTful API 규칙

**기본 URL**: `http://localhost:8080/api`

**엔드포인트 네이밍**:

```
리소스        메서드   엔드포인트                     설명
--------------------------------------------------------------
인증          POST    /auth/login                   로그인
             POST    /auth/logout                  로그아웃
             POST    /auth/refresh                 토큰 갱신

프로젝트       GET     /projects                     목록 조회
             POST    /projects                     생성
             GET     /projects/{id}                상세 조회
             PUT     /projects/{id}                수정
             DELETE  /projects/{id}                삭제

리스크        GET     /projects/{id}/risks          목록 조회
             POST    /projects/{id}/risks          생성
             PUT     /risks/{id}                   수정
             DELETE  /risks/{id}                   삭제

채팅          GET     /chat/sessions                세션 목록
             POST    /chat/sessions                세션 생성
             POST    /chat/message                 메시지 전송
             GET     /chat/sessions/{id}/messages  메시지 조회
```

### API 응답 형식

**성공 응답**:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "보험 심사 프로젝트",
    "status": "IN_PROGRESS"
  },
  "timestamp": "2026-01-07T10:30:00Z"
}
```

**오류 응답**:

```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ]
  },
  "timestamp": "2026-01-07T10:30:00Z"
}
```

---

## 보안 아키텍처

### 인증 흐름 (JWT)

```
1. 사용자 로그인
   Client → POST /api/auth/login {email, password}

2. 인증 처리
   Backend → 비밀번호 검증 (BCrypt)

3. JWT 발급
   Backend → JWT 생성 (secret key로 서명)
   - Header: {alg: "HS256", typ: "JWT"}
   - Payload: {sub: user_id, roles: [...], exp: ...}
   - Signature: HMACSHA256(header + payload, secret)

4. 토큰 반환
   Backend → {accessToken, refreshToken}

5. 후속 요청
   Client → Authorization: Bearer {accessToken}

6. 토큰 검증
   Backend → JwtAuthenticationFilter
   - 서명 검증
   - 만료 시간 확인
   - SecurityContext에 인증 정보 설정
```

### 보안 설정

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf().disable()  // REST API는 CSRF 불필요
            .sessionManagement()
                .sessionCreationPolicy(STATELESS)  // JWT 사용
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 환경변수 기반 시크릿 관리

```bash
# .env 파일
POSTGRES_PASSWORD=secure_db_password
JWT_SECRET=long_random_256bit_key
NEO4J_PASSWORD=secure_neo4j_password

# docker-compose.yml
environment:
  SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
```

---

## 배포 아키텍처

### 개발 환경

```bash
docker-compose up -d
```

- Hot reload 지원 (Frontend: Vite, Backend: DevTools)
- 디버깅 포트 노출
- 상세한 로깅

### 프로덕션 환경

```bash
docker-compose -f docker-compose.yml \
               -f docker-compose.prod.yml up -d
```

**주요 차이점**:

| 항목 | 개발 | 프로덕션 |
|------|------|----------|
| 이미지 | 개발용 Dockerfile | 최적화된 multi-stage build |
| 로깅 | DEBUG 레벨 | WARN/ERROR 레벨 |
| 리소스 제한 | 없음 | CPU/메모리 제한 설정 |
| 재시작 정책 | no | always |
| SSL/TLS | 없음 | Nginx SSL 터미네이션 |
| 모니터링 | 기본 | Prometheus + Grafana |

### 확장 전략

**수평 확장 (Scale Out)**:

```bash
# Backend 인스턴스 3개로 확장
docker-compose up -d --scale backend=3

# Nginx 로드 밸런서 설정
upstream backend {
    server backend_1:8080;
    server backend_2:8080;
    server backend_3:8080;
}
```

**수직 확장 (Scale Up)**:

```yaml
# docker-compose.yml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
```

---

## 성능 최적화

### 캐싱 전략

**다층 캐싱**:

```
1. 브라우저 캐시
   - 정적 리소스 (JS, CSS, 이미지)
   - Cache-Control: max-age=31536000

2. Nginx 캐시
   - API 응답 캐싱 (GET 요청)
   - proxy_cache

3. Redis 캐시
   - 자주 조회되는 데이터
   - TTL 기반 만료

4. JPA 2차 캐시
   - 엔티티 캐싱
   - Hibernate 캐시
```

### 데이터베이스 최적화

```sql
-- 인덱스 설정
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_risks_project_id ON risks(project_id);
CREATE INDEX idx_chat_messages_session_id
    ON chat_messages(session_id);

-- 쿼리 최적화
-- N+1 문제 해결: @EntityGraph 사용
@EntityGraph(attributePaths = {"risks", "tasks"})
Project findByIdWithDetails(Long id);
```

### LLM 응답 최적화

```python
# 스트리밍 응답 (향후 구현)
@app.route("/api/chat/stream", methods=["POST"])
def chat_stream():
    def generate():
        for token in llm.stream(prompt):
            yield f"data: {json.dumps({'token': token})}\n\n"

    return Response(generate(), mimetype='text/event-stream')

# 배치 임베딩
embeddings = embedding_model.encode(
    texts,
    batch_size=32,
    show_progress_bar=False
)
```

---

## 모니터링 및 관찰성

### 로깅

**구조화된 로깅**:

```java
@Slf4j
public class ProjectService {
    public Project createProject(ProjectRequest request) {
        log.info("Creating project: name={}, userId={}",
            request.getName(),
            getCurrentUserId());

        try {
            // ... 비즈니스 로직
            log.info("Project created successfully: id={}",
                project.getId());
        } catch (Exception e) {
            log.error("Failed to create project", e);
            throw e;
        }
    }
}
```

### 헬스체크

```yaml
# docker-compose.yml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### 메트릭 (향후 구현)

```yaml
# Prometheus 메트릭 수집
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 향후 개선 사항

### 단기 (3개월)

- [ ] WebSocket 기반 실시간 알림
- [ ] 스트리밍 LLM 응답
- [ ] API Rate Limiting
- [ ] E2E 테스트 추가

### 중기 (6개월)

- [ ] Kubernetes 배포
- [ ] CI/CD 파이프라인 (GitHub Actions)
- [ ] Prometheus + Grafana 모니터링
- [ ] 멀티테넌시 지원

### 장기 (12개월)

- [ ] 마이크로서비스 분리
- [ ] Event-Driven Architecture (Kafka)
- [ ] GraphQL API
- [ ] 모바일 앱 (React Native)

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-01-07
**작성자**: PMS Insurance Claims Team
