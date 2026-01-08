# 변경 이력 - 2026-01-08

## 🔧 주요 수정 사항

### 1. AI 챗봇 Mock 모드 문제 해결

#### 문제 증상
- 챗봇 사용 시 "현재 Mock 모드로 동작 중입니다" 메시지 표시
- 실제 LLM 모델이 로드되지 않고 Mock 서버로 Fallback

#### 근본 원인 분석
1. **Volume Mount 중복 문제**
   - `docker-compose.yml`의 llm-service에 중복 volume mount 설정
   - `./models:/app/models` 마운트가 실제 모델이 있는 `llm-service/models` 디렉토리를 덮어씀
   - 결과: 컨테이너 내부에서 모델 파일에 접근 불가

2. **환경 변수 불일치**
   - Java 코드: `@Value("${ai.service.url}")`
   - Docker Compose: `AI_TEAM_API_URL` 설정
   - Spring Boot의 환경 변수 자동 변환 규칙 불일치

#### 해결 방법

##### A. docker-compose.yml 수정

**변경 전:**
```yaml
llm-service:
  volumes:
    - ./models:/app/models      # ❌ 문제 발생
    - ./llm-service:/app
```

**변경 후:**
```yaml
llm-service:
  volumes:
    - ./llm-service:/app        # ✅ 단일 마운트로 해결
```

**변경 전:**
```yaml
backend:
  environment:
    AI_TEAM_API_URL: ${AI_TEAM_API_URL:-http://llm-service:8000}
    AI_TEAM_MOCK_URL: http://ai-service:1080
    AI_TEAM_MODEL: ${AI_TEAM_MODEL:-google.gemma-3-12b-pt.Q5_K_M.gguf}
```

**변경 후:**
```yaml
backend:
  environment:
    AI_SERVICE_URL: ${AI_SERVICE_URL:-http://llm-service:8000}
    AI_SERVICE_MOCK_URL: http://mockserver:1080
    AI_SERVICE_MODEL: ${AI_SERVICE_MODEL:-google.gemma-3-12b-pt.Q5_K_M.gguf}
```

##### B. 적용 방법
```bash
# 컨테이너 재생성 (restart로는 volume mount 변경 미적용)
docker compose up -d --force-recreate llm-service
docker compose up -d --force-recreate backend
```

#### 검증
```bash
# 1. LLM 서비스 헬스 체크
curl http://localhost:8000/health
# 예상: {"model_loaded": true, "rag_service_loaded": true, "status": "healthy"}

# 2. 챗봇 테스트
curl -X POST http://localhost:8083/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "프로젝트 관리에 대해 알려주세요"}'
# 예상: RAG 기반 실제 답변 (Mock 메시지 없음)
```

---

### 2. 포트 변경: 8080 → 8083

#### 변경 이유
- 포트 8080이 다른 프로세스(Node.js)와 충돌
- 반복적인 "Address already in use" 오류 발생

#### 변경된 파일

1. **docker-compose.yml**
   ```yaml
   backend:
     ports:
       - "8083:8080"  # 8080:8080 → 8083:8080
   ```

2. **PMS_IC_FrontEnd_v1.2/.env**
   ```
   VITE_API_URL=http://localhost:8083/api
   ```

3. **PMS_IC_FrontEnd_v1.2/src/services/api.ts**
   ```typescript
   const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8083/api';
   ```

4. **PMS_IC_FrontEnd_v1.2/src/app/components/Settings.tsx**
   ```typescript
   const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8083/api';
   ```

---

### 3. Security 설정 수정

#### 파일: `SecurityConfig.java`

**변경 내용:**
```java
.requestMatchers(
    "/api/auth/login",
    "/api/auth/refresh",
    "/api/chat/**",    // ✅ 추가
    "/actuator/**",
    // ...
).permitAll()
```

**이유:**
- 챗봇 API가 인증 없이 접근 가능해야 함
- Guest 사용자 지원

---

### 4. Guest 사용자 지원 추가

#### 파일: `ChatService.java`

**변경 내용:**
```java
@Transactional
public ChatResponse sendMessage(ChatRequest request) {
    User currentUser;
    String userId;
    try {
        currentUser = authService.getCurrentUser();
        userId = currentUser.getId();
    } catch (Exception e) {
        // ✅ 인증되지 않은 사용자는 guest로 처리
        currentUser = null;
        userId = "guest";
        log.info("Processing chat request for unauthenticated user (guest)");
    }
    // ...
}
```

---

### 5. 문서 업데이트

#### 수정된 문서
- ✅ **README.md**: 포트 정보, 모델 경로 수정
- ✅ **README_DOCKER.md**: 전체 구조도, 포트 테이블, 서비스 정보 업데이트
- ✅ **실행가이드.md**: 접속 URL 업데이트
- ✅ **TROUBLESHOOTING.md**: 새로 작성 (Mock 모드 해결 방법 포함)

#### 주요 변경 사항
1. Backend 포트: `8080` → `8083`
2. 모델 경로: `./models` → `llm-service/models`
3. Neo4j, MockServer 정보 추가
4. 환경 변수명 정정: `AI_TEAM_*` → `AI_SERVICE_*`

---

## 📊 현재 포트 할당

| 서비스 | 포트 | 설명 |
|--------|------|------|
| Frontend | 5173 | React + Vite |
| **Backend** | **8083** | Spring Boot API (변경됨) |
| LLM Service | 8000 | Flask + LLM + RAG |
| Neo4j Browser | 7474 | Graph DB UI |
| Neo4j Bolt | 7687 | Graph DB Protocol |
| PostgreSQL | 5432 | RDBMS |
| Redis | 6379 | Cache |
| Redis Commander | 8081 | Redis GUI |
| PgAdmin | 5050 | PostgreSQL GUI |
| MockServer | 1080 | AI Fallback Mock |

---

## 🎯 테스트 결과

### AI 챗봇 테스트
```bash
$ curl -X POST http://localhost:8083/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "프로젝트 관리에 대해 알려주세요"}'

{
  "success": true,
  "data": {
    "reply": "안녕하세요, 프로젝트 관리에 대해 말씀드릴게요. 문서에서 강조하는 바와 같이...",
    "confidence": 0.85,
    "suggestions": []
  }
}
```
✅ **성공**: Mock 메시지 없이 실제 LLM 응답 생성

### RAG 동작 확인
```bash
$ docker logs pms-llm-service --tail 20 | grep "RAG"

INFO:chat_workflow:  ✅ Final RAG results: 5 documents
INFO:chat_workflow:💬 Generating response with 5 RAG docs
```
✅ **성공**: 5개 문서 검색 후 답변 생성

### 서비스 Health Check
```bash
$ curl http://localhost:8000/health
{
  "status": "healthy",
  "model_loaded": true,
  "rag_service_loaded": true,
  "chat_workflow_loaded": true
}
```
✅ **성공**: 모든 서비스 정상 로드

---

## 🔍 주요 학습 내용

### 1. Docker Volume Mount 우선순위
- 더 구체적인 경로가 상위 경로를 덮어씀
- `./models:/app/models`가 `./llm-service:/app`의 하위 디렉토리를 override

### 2. Spring Boot 환경 변수 변환
- `AI_SERVICE_URL` → `ai.service.url` (자동 변환)
- `AI_TEAM_API_URL` → `ai.team.api.url` (다른 속성)

### 3. Docker Compose 설정 적용
- `restart`: 환경 변수만 재로드
- `up -d --force-recreate`: Volume mount 등 모든 설정 재적용

---

## 📝 권장 사항

### 개발 환경 설정 시
1. ✅ `docker compose up -d --force-recreate` 사용 (재시작 대신)
2. ✅ 환경 변수는 Java 속성 명명 규칙과 일치시키기
3. ✅ Volume mount는 최소한으로 유지
4. ✅ 포트 충돌 시 즉시 변경

### 트러블슈팅 시
1. 헬스 체크 엔드포인트 먼저 확인
2. 컨테이너 내부 파일 시스템 검증 (`docker exec ls`)
3. 환경 변수 실제 값 확인 (`docker exec env`)
4. 로그에서 연결 오류 패턴 찾기

---

## 🚀 다음 단계

### 추가 개선 사항
- [ ] 프로덕션 환경 설정 문서화
- [ ] CI/CD 파이프라인 구축
- [ ] 모니터링 대시보드 추가
- [ ] 백업/복구 프로시저 문서화

---

## 📞 문의

문제가 발생하면 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)를 참조하세요.
