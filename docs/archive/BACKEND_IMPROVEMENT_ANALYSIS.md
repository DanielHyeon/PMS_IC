# 백엔드 구조 분석 및 개선점 검토

## 📋 현재 백엔드 구조 개요

### 아키텍처
```
프론트엔드 (React) :5173
    ↓ HTTP/REST
백엔드 (Spring Boot 3.2.1) :8080
    ├─ Modular Monolith 구조
    │   ├─ auth (인증/인가)
    │   ├─ project (프로젝트 관리)
    │   ├─ task (작업 관리)
    │   ├─ chat (AI 챗봇)
    │   ├─ rag (RAG 검색)
    │   ├─ report (리포팅)
    │   └─ risk (리스크 관리)
    ├─ PostgreSQL (데이터베이스)
    ├─ Redis (캐시/세션)
    └─ WebClient → LLM 서비스 (Flask) :8000
        ├─ RAG (ChromaDB)
        └─ Gemma 3 12B 모델
```

### 기술 스택
- **백엔드**: Spring Boot 3.2.1, Java 17
- **데이터베이스**: PostgreSQL 15
- **캐시**: Redis 7
- **보안**: Spring Security + JWT
- **API 문서**: Swagger/OpenAPI 3
- **LLM 서비스**: Flask + llama-cpp-python + LangGraph

---

## ✅ 현재 구조의 장점

1. **모듈화된 구조**: Modular Monolith로 MSA 전환 용이
2. **명확한 책임 분리**: 도메인별 패키지 분리 (auth, project, task, chat 등)
3. **RAG 통합**: 문서 기반 지능형 답변 지원
4. **Fallback 메커니즘**: LLM 서비스 실패 시 Mock 서비스로 전환
5. **LangGraph 워크플로우**: 의도 분류 및 지능형 라우팅

---

## 🔍 주요 개선점

### 1. **외부 서비스 호출 안정성 강화** ⚠️ HIGH PRIORITY

#### 현재 문제점
- `AIChatClient`에서 단순 try-catch로만 처리
- Retry 로직이 설정에만 있고 실제 구현 없음
- Circuit Breaker 패턴 미적용
- 타임아웃 설정이 있으나 WebClient에 미적용

#### 개선 방안
```java
// Spring Cloud Circuit Breaker + Resilience4j 도입
@CircuitBreaker(name = "llm-service", fallbackMethod = "fallbackChat")
@Retry(name = "llm-service")
@TimeLimiter(name = "llm-service")
public ChatResponse chat(...) {
    // WebClient에 타임아웃 설정 추가
}
```

**추천 라이브러리**:
- `resilience4j-spring-boot3` (Circuit Breaker, Retry, Rate Limiter)
- `spring-cloud-starter-circuitbreaker-resilience4j`

---

### 2. **비동기 처리 및 성능 최적화** ⚠️ MEDIUM PRIORITY

#### 현재 문제점
- `AIChatClient.chat()`가 동기 블로킹 호출 (`block()`)
- RAG 검색과 LLM 호출이 순차 처리
- Redis 캐싱이 주석 처리됨 (LocalDateTime 직렬화 문제)

#### 개선 방안
```java
// 비동기 처리
@Async
public CompletableFuture<ChatResponse> chatAsync(...) {
    // RAG 검색과 LLM 호출 병렬 처리
    CompletableFuture<List<String>> ragFuture = 
        CompletableFuture.supplyAsync(() -> ragSearchService.search(...));
    
    // Redis 캐싱 개선 (Jackson 직렬화 설정)
}
```

**추천 설정**:
- `@EnableAsync` 활성화
- `ThreadPoolTaskExecutor` 설정
- Redis 직렬화: Jackson2JsonRedisSerializer 사용

---

### 3. **에러 처리 및 로깅 개선** ⚠️ MEDIUM PRIORITY

#### 현재 문제점
- `RAGSearchService`에서 예외 발생 시 빈 리스트만 반환 (로그만 남김)
- 에러 추적을 위한 상관관계 ID(Correlation ID) 없음
- 구조화된 로깅 미적용

#### 개선 방안
```java
// Correlation ID 추가
@Slf4j
public class ChatService {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    public ChatResponse sendMessage(ChatRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        
        try {
            // 로깅에 correlationId 자동 포함
            log.info("Processing chat request: {}", request.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
```

**추천 라이브러리**:
- `micrometer-tracing` (분산 추적)
- `logback-json-classic` (구조화된 로깅)

---

### 4. **데이터베이스 쿼리 최적화** ⚠️ MEDIUM PRIORITY

#### 현재 문제점
- `ChatService.getRecentMessages()`에서 중복 쿼리 실행
- N+1 쿼리 문제 가능성 (JPA 연관관계)
- 인덱스 최적화 미확인

#### 개선 방안
```java
// 쿼리 최적화
@Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
List<ChatMessage> findRecentMessages(@Param("sessionId") String sessionId, Pageable pageable);

// 페이징 적용
List<ChatMessage> recentMessages = chatMessageRepository
    .findRecentMessages(sessionId, PageRequest.of(0, 10));
```

**추천 설정**:
- JPA 쿼리 로깅 활성화 (`show-sql: true` in dev)
- `@EntityGraph` 사용 (연관관계 페치 최적화)
- 데이터베이스 인덱스 추가 (session_id, created_at)

---

### 5. **테스트 커버리지 향상** ⚠️ LOW PRIORITY

#### 현재 문제점
- 통합 테스트 부족
- 외부 서비스 Mock 테스트 미흡
- WebClient 테스트용 Mock Server 미사용

#### 개선 방안
```java
// MockWebServer 사용
@SpringBootTest
class AIChatClientTest {
    @Autowired
    private MockWebServer mockWebServer;
    
    @Test
    void testChatWithRetry() {
        // Mock 응답 설정 및 테스트
    }
}
```

**추천 라이브러리**:
- `okhttp-mockwebserver` (WebClient 테스트)
- `testcontainers` (PostgreSQL, Redis 통합 테스트)

---

### 6. **모니터링 및 관찰성 강화** ⚠️ MEDIUM PRIORITY

#### 현재 문제점
- Actuator는 있으나 메트릭 수집 미흡
- LLM 서비스 응답 시간 추적 없음
- 비즈니스 메트릭 부족 (채팅 성공률, RAG 검색 성공률 등)

#### 개선 방안
```java
// Micrometer 메트릭 추가
@Service
public class ChatService {
    private final Counter chatRequestCounter;
    private final Timer chatResponseTimer;
    
    public ChatResponse sendMessage(...) {
        return chatResponseTimer.recordCallable(() -> {
            chatRequestCounter.increment();
            // 처리 로직
        });
    }
}
```

**추천 설정**:
- Prometheus 메트릭 수집
- Grafana 대시보드 구성
- 분산 추적 (Jaeger/Zipkin)

---

### 7. **보안 강화** ⚠️ HIGH PRIORITY

#### 현재 문제점
- JWT Secret이 기본값 (프로덕션 위험)
- API Rate Limiting 없음
- 입력 검증 부족 (ChatRequest 메시지 길이 제한 등)

#### 개선 방안
```java
// Rate Limiting 추가
@RateLimiter(name = "chat-api")
@PostMapping("/message")
public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
    // @Valid로 입력 검증
}

// ChatRequest DTO에 검증 추가
public class ChatRequest {
    @NotBlank
    @Size(min = 1, max = 2000)
    private String message;
}
```

**추천 라이브러리**:
- `resilience4j-ratelimiter` (Rate Limiting)
- `spring-boot-starter-validation` (입력 검증)

---

## 🤖 MCP (Model Context Protocol) 적용 검토

### MCP란?
**Model Context Protocol**은 AI 모델이 외부 도구와 리소스에 안전하게 접근할 수 있도록 하는 표준 프로토콜입니다. Cursor, Claude Desktop 등에서 사용되며, AI가 파일 시스템, 데이터베이스, API 등을 안전하게 호출할 수 있게 합니다.

### 현재 프로젝트에 MCP 적용 가능성

#### ✅ 적용 가능한 시나리오

1. **LLM 서비스와 백엔드 통합 강화**
   - 현재: HTTP REST API로 통신
   - MCP 적용: LLM이 직접 프로젝트 데이터, 작업 상태 등을 조회/수정
   - 장점: 더 자연스러운 대화형 인터페이스, Tool Calling 지원

2. **RAG 시스템 확장**
   - 현재: 문서 검색만 지원
   - MCP 적용: LLM이 문서 인덱싱, 업데이트, 삭제 등을 직접 수행
   - 장점: "이 문서를 업데이트해줘" 같은 자연어 명령 지원

3. **프로젝트 관리 자동화**
   - 현재: 사용자가 수동으로 작업 생성/수정
   - MCP 적용: LLM이 "프로젝트 A의 작업을 생성해줘" 같은 명령 처리
   - 장점: AI 어시스턴트가 실제 작업 수행 가능

#### ⚠️ 적용 시 고려사항

1. **보안 이슈**
   - MCP는 AI에게 시스템 접근 권한을 부여하므로 매우 신중해야 함
   - 권한 범위 제한 필수 (읽기 전용, 특정 리소스만 등)
   - 사용자 인증/인가와 연동 필요

2. **아키텍처 변경**
   - 현재: Spring Boot → Flask LLM (단방향)
   - MCP 적용: 양방향 통신 필요 (LLM → Backend Tool Calling)
   - MCP 서버 구현 필요 (Python 또는 Java)

3. **복잡도 증가**
   - Tool Calling 로직 구현
   - 에러 처리 및 롤백 메커니즘
   - 사용자 확인 절차 (위험한 작업의 경우)

#### 📊 적용 우선순위 평가

| 시나리오 | 우선순위 | 난이도 | 효과 | 추천 여부 |
|---------|---------|--------|------|----------|
| RAG 시스템 확장 | 중 | 중 | 높음 | ✅ 추천 |
| 프로젝트 관리 자동화 | 낮 | 높음 | 매우 높음 | ⚠️ 장기 검토 |
| LLM-Backend 직접 통합 | 낮 | 높음 | 중 | ❌ 불필요 |

### 결론: MCP 적용 권장사항

**현재 단계에서는 MCP 적용을 권장하지 않습니다.**

**이유**:
1. 현재 아키텍처로도 충분히 기능적
2. 보안 리스크가 큼 (프로덕션 환경)
3. 구현 복잡도 대비 효과가 명확하지 않음

**대신 추천하는 개선**:
1. **Tool Calling 지원** (MCP 없이)
   - LangGraph에서 Tool Calling 노드 추가
   - 백엔드에 Tool API 엔드포인트 제공
   - 예: `{"tool": "get_project_status", "project_id": "123"}`

2. **점진적 개선**
   - 먼저 위의 개선점들(Retry, Circuit Breaker, 비동기 등) 적용
   - 안정성 확보 후 고급 기능 검토

**향후 MCP 검토 시점**:
- 사용자 요구사항이 명확해질 때
- AI 어시스턴트가 실제 작업 수행이 필요할 때
- 보안 및 권한 관리 체계가 완성되었을 때

---

## 📝 우선순위별 개선 로드맵

### Phase 1: 안정성 강화 (1-2주)
1. ✅ Circuit Breaker + Retry 구현
2. ✅ WebClient 타임아웃 설정
3. ✅ 에러 처리 개선 (Correlation ID)
4. ✅ 입력 검증 강화

### Phase 2: 성능 최적화 (2-3주)
1. ✅ 비동기 처리 도입
2. ✅ Redis 캐싱 복구 및 개선
3. ✅ 데이터베이스 쿼리 최적화
4. ✅ 모니터링 메트릭 추가

### Phase 3: 고급 기능 (3-4주)
1. ⚠️ Tool Calling 지원 (MCP 대안)
2. ⚠️ Rate Limiting 구현
3. ⚠️ 통합 테스트 작성
4. ⚠️ 문서화 개선

---

## 🔗 참고 자료

- [Resilience4j 공식 문서](https://resilience4j.readme.io/)
- [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker)
- [Model Context Protocol (MCP) 공식 사이트](https://modelcontextprotocol.io/)
- [LangGraph Tool Calling 가이드](https://langchain-ai.github.io/langgraph/tutorials/tool-use/)

---

**작성일**: 2024년
**검토자**: AI Assistant
**다음 검토 예정일**: 개선 사항 적용 후

