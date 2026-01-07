# 백엔드 개선 및 프로젝트 DB 조회 기능 구현 요약

## ✅ 구현 완료 사항

### Phase 1: 안정성 강화

#### 1. Resilience4j 의존성 추가
- `resilience4j-spring-boot3` (Circuit Breaker, Retry)
- `resilience4j-reactor` (Reactive 지원)

#### 2. Circuit Breaker + Retry 구현
- **AIChatClient**에 `@CircuitBreaker`, `@Retry`, `@TimeLimiter` 적용
- LLM 서비스 호출 실패 시 자동으로 Mock 서비스로 Fallback
- 설정 파일(`application.yml`)에 Resilience4j 설정 추가

#### 3. WebClient 타임아웃 설정
- `WebClientConfig`에 타임아웃 설정 추가 (30초)
- 전용 `aiServiceWebClient` Bean 생성
- `RestTemplate`에도 타임아웃 설정 적용

#### 4. Correlation ID 추가
- `ChatService`와 `AIChatClient`에 Correlation ID 생성 및 로깅
- MDC(Mapped Diagnostic Context)를 사용한 분산 추적 지원
- 모든 로그에 `correlationId` 포함

#### 5. 입력 검증 강화
- `ChatRequest` DTO에 `@NotBlank`, `@Size` 검증 추가
- `ChatController`에 `@Valid` 어노테이션 추가
- 메시지 길이 제한: 1자 이상 2000자 이하

---

### Phase 2: 프로젝트 DB 조회 기능

#### 1. ProjectDataService 구현
- **프로젝트 관련 질문 감지**: 키워드 기반 자동 감지
- **프로젝트 데이터 조회**: 전체 프로젝트 또는 특정 프로젝트 조회
- **프로젝트 정보 포맷팅**: LLM이 이해하기 쉬운 텍스트 형식으로 변환
- **프로젝트 ID 추출**: 메시지에서 프로젝트 ID 자동 추출

#### 2. ChatService 통합
- 프로젝트 관련 질문 감지 시 자동으로 프로젝트 데이터 조회
- 프로젝트 데이터를 LLM 컨텍스트에 자동 추가
- RAG 검색 결과와 함께 프로젝트 데이터 전달

#### 3. AIChatClient 확장
- 프로젝트 데이터를 받아서 LLM 요청에 포함하는 메서드 오버로드 추가
- 프로젝트 데이터를 `retrieved_docs`의 첫 번째 항목으로 추가

---

## 📋 주요 변경 파일

### 의존성 추가
- `pom.xml`: Resilience4j 의존성 추가

### 설정 파일
- `application.yml`: Resilience4j 설정 추가 (Circuit Breaker, Retry, TimeLimiter)

### 새로운 서비스
- `ProjectDataService.java`: 프로젝트 데이터 조회 및 질문 감지

### 수정된 파일
- `WebClientConfig.java`: 타임아웃 설정 및 전용 WebClient Bean 추가
- `ChatRequest.java`: 입력 검증 어노테이션 추가
- `ChatController.java`: `@Valid` 어노테이션 추가
- `ChatService.java`: Correlation ID 및 프로젝트 데이터 조회 로직 추가
- `AIChatClient.java`: Circuit Breaker, Retry, Correlation ID, 프로젝트 데이터 지원 추가

---

## 🎯 사용 예시

### 프로젝트 관련 질문 예시

사용자가 다음과 같은 질문을 하면:

1. **"프로젝트 현황 알려줘"**
   - 자동으로 모든 프로젝트 데이터를 조회
   - LLM이 프로젝트 정보를 기반으로 답변 생성

2. **"PROJ-001 프로젝트 정보 알려줘"**
   - 프로젝트 ID를 자동 추출
   - 해당 프로젝트의 상세 정보를 조회하여 답변

3. **"프로젝트 진행률이 어떻게 되나요?"**
   - 모든 프로젝트의 진행률 정보를 포함하여 답변

### 지원하는 키워드

- 프로젝트, project
- 프로젝트 현황, 프로젝트 상태, 프로젝트 진행
- 진행률, progress
- 예산, budget
- 시작일, 종료일
- 단계, phase
- 등등...

---

## 🔧 설정 값

### Resilience4j 설정 (application.yml)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      llm-service:
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
  retry:
    instances:
      llm-service:
        maxAttempts: 3
        waitDuration: 1s
  timelimiter:
    instances:
      llm-service:
        timeoutDuration: 30s
```

### AI 서비스 타임아웃

- WebClient 타임아웃: 30초
- RestTemplate 타임아웃: 30초 (설정값)

---

## 📊 동작 흐름

```
1. 사용자 질문 입력
   ↓
2. ChatService.sendMessage()
   - Correlation ID 생성
   - 세션 조회/생성
   - 메시지 저장
   ↓
3. 프로젝트 관련 질문인지 확인
   - ProjectDataService.isProjectRelatedQuery()
   ↓
4. 프로젝트 데이터 조회 (질문인 경우)
   - ProjectDataService.getAllProjectsSummary()
   또는
   - ProjectDataService.getProjectSummary(projectId)
   ↓
5. AIChatClient.chat()
   - Circuit Breaker로 보호
   - Retry 자동 재시도
   - 프로젝트 데이터를 LLM 컨텍스트에 추가
   ↓
6. LLM 서비스 호출
   - RAG 검색 결과 + 프로젝트 데이터 전달
   ↓
7. LLM 응답 생성
   - 프로젝트 데이터를 기반으로 정확한 답변 생성
   ↓
8. 응답 저장 및 반환
```

---

## 🚀 다음 단계 (선택사항)

1. **캐싱 추가**: 프로젝트 데이터 조회 결과를 Redis에 캐싱
2. **비동기 처리**: 프로젝트 데이터 조회를 비동기로 처리
3. **더 정교한 질문 감지**: NLP 기반 의도 분류 (현재는 키워드 기반)
4. **작업(Task) 데이터 조회**: 프로젝트뿐만 아니라 작업 정보도 조회
5. **단계(Phase) 데이터 조회**: 프로젝트 단계별 상세 정보 조회

---

## ⚠️ 주의사항

1. **프로젝트 데이터 크기**: 프로젝트가 많을 경우 LLM 컨텍스트가 커질 수 있음
   - 해결: 페이징 또는 요약 정보만 전달

2. **성능**: 프로젝트 데이터 조회가 추가되므로 응답 시간이 약간 증가할 수 있음
   - 해결: 캐싱 또는 비동기 처리

3. **보안**: 프로젝트 데이터 접근 권한 확인 필요
   - 현재는 사용자 인증만 확인
   - 향후 프로젝트별 접근 권한 검증 추가 권장

---

**구현 완료일**: 2024년
**구현자**: AI Assistant

