# LLM 서비스 연동 가이드

## 📋 개요

이 문서는 백엔드 Spring Boot 애플리케이션과 Flask 기반 로컬 LLM 서비스, 그리고 프론트엔드 React 챗봇의 연동 설정 및 테스트 방법을 설명합니다.

## 🏗️ 아키텍처

```
프론트엔드 (React)
    ↓ HTTP
백엔드 (Spring Boot) :8080
    ↓ HTTP
LLM 서비스 (Flask) :8000
    ↓
로컬 모델 (GGUF)
```

## ✅ 수정된 항목

### 1. docker-compose.yml
- **Flask LLM 서비스** 추가 (포트 8000)
- 백엔드가 `llm-service:8000`을 우선 호출하도록 설정
- Mock 서비스를 폴백용으로 변경 (포트 1080)
- Ollama 서비스는 주석 처리 (선택적 사용)

### 2. AIChatClient.java
- `callOllama()` 대신 `callFlaskLLM()` 메서드 사용
- Flask LLM 요청 형식에 맞게 변경:
  ```json
  {
    "message": "사용자 메시지",
    "context": [
      {"role": "user", "content": "이전 메시지"},
      {"role": "assistant", "content": "이전 응답"}
    ]
  }
  ```
- Flask LLM 응답 파싱:
  ```json
  {
    "reply": "AI 응답",
    "confidence": 0.85,
    "suggestions": []
  }
  ```

### 3. application.yml
- `ai.service.url`: `http://localhost:8000` (Flask LLM)
- `ai.service.mock-url`: `http://localhost:1080` (Mock 서비스)

### 4. llm-service/Dockerfile
- `cmake` 패키지 추가 (llama-cpp-python 빌드용)
- `curl` 추가 (헬스체크용)

## 🚀 실행 방법

### 1. 모델 파일 준비

LLM 서비스가 사용할 GGUF 모델 파일을 준비합니다:

```bash
# 프로젝트 루트의 models 디렉토리에 모델 파일 배치
ls -lh /wp/PMS_IC/models/
# 예: LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf
```

### 2. Docker Compose로 전체 시스템 실행

```bash
cd /wp/PMS_IC

# 전체 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f llm-service
docker-compose logs -f backend
```

### 3. 개별 서비스 상태 확인

```bash
# LLM 서비스 헬스체크
curl http://localhost:8000/health

# 백엔드 헬스체크
curl http://localhost:8080/actuator/health

# 프론트엔드 접속
# 브라우저에서 http://localhost:5173
```

## 🧪 테스트 방법

### 1. LLM 서비스 직접 테스트

```bash
curl -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "안녕하세요",
    "context": []
  }'
```

**기대 응답:**
```json
{
  "reply": "네, 프로젝트 관리를 도와드릴 수 있습니다...",
  "confidence": 0.85,
  "suggestions": []
}
```

### 2. 백엔드 API 테스트

먼저 로그인하여 JWT 토큰을 받습니다:

```bash
# 로그인 (올바른 계정 사용)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@insure.com",
    "password": "admin123"
  }' > /tmp/login.json

# 토큰 추출
cat /tmp/login.json | grep -o 'eyJ[^"]*' | head -1 > /tmp/token.txt

# 챗봇 메시지 전송
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $(cat /tmp/token.txt)" \
  -d '{
    "sessionId": null,
    "message": "프로젝트 진행 상황을 알려줘"
  }'
```

**기대 응답:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "sessionId": "uuid-string",
    "reply": "네, 프로젝트 일정 관리를 도와드릴 수 있습니다...",
    "confidence": 0.85,
    "suggestions": []
  }
}
```

### 3. 프론트엔드 테스트

**📱 상세 가이드**: [프론트엔드_챗봇_테스트_가이드.md](./프론트엔드_챗봇_테스트_가이드.md)

**빠른 테스트:**
1. 브라우저에서 `http://localhost:5173` 접속
2. 로그인 (admin@insure.com / admin123)
3. 우측 하단 AI 어시스턴트 아이콘 클릭
4. 추천 질문 또는 직접 메시지 입력
5. AI 응답 확인 (실제 LLM 응답, Mock 아님)

**테스트 시나리오:**
```
1. "안녕하세요" → 인사 응답 확인
2. "프로젝트 일정 관리를 도와줄 수 있나요?" → 구체적 답변 확인
3. "WBS 생성" 버튼 클릭 → 추천 질문 동작 확인
4. 연속 대화로 컨텍스트 유지 확인
```

## ✅ 테스트 결과 확인

### 성공 확인 사항
- ✅ LLM 서비스 정상 실행 (포트 8000)
- ✅ 백엔드 Flask LLM 호출 성공
- ✅ 로컬 GGUF 모델 로딩 완료
- ✅ 실시간 AI 응답 생성
- ✅ 세션 관리 및 데이터베이스 저장

### 주의사항
- **로그인 계정**: `admin@insure.com` (admin@pms.com이 아님)
- **Redis 캐싱**: LocalDateTime 직렬화 문제로 임시 비활성화
- **KV 캐시**: 각 요청마다 `model.reset()`으로 초기화
- **모델 버전**: llama-cpp-python 0.3.16 사용 (LFM2 아키텍처 지원)

## 📊 로그 확인 및 디버깅

### LLM 서비스 로그

```bash
# 실시간 로그 확인
docker-compose logs -f llm-service

# 주요 로그 메시지:
# - "Loading model from ..." : 모델 로딩 시작
# - "Model loaded successfully" : 모델 로딩 완료
# - "Error processing chat request" : 요청 처리 실패
```

### 백엔드 로그

```bash
# 실시간 로그 확인
docker-compose logs -f backend

# 주요 로그 메시지:
# - "Flask LLM service call failed" : LLM 서비스 호출 실패
# - "Mock AI service call failed" : Mock 서비스 폴백 실패
```

### 서비스 연결 테스트

```bash
# 백엔드 컨테이너에서 LLM 서비스 접근 테스트
docker exec -it pms-backend curl http://llm-service:8000/health
```

## ⚠️ 주의사항

### 1. 모델 파일 크기
- GGUF 모델 파일은 크기가 클 수 있습니다 (수 GB)
- 충분한 디스크 공간 확보 필요
- Docker 볼륨 마운트로 모델 공유

### 2. 메모리 요구사항
- LLM 서비스는 모델 크기에 따라 RAM 사용량이 높습니다
- 최소 8GB RAM 권장 (모델에 따라 다름)
- Docker Desktop의 메모리 할당 확인

### 3. 첫 실행 시간
- 모델 로딩에 시간이 걸립니다 (수십 초 ~ 수 분)
- 헬스체크 `start_period: 60s` 설정으로 충분한 시간 제공
- 백엔드가 LLM 서비스를 기다리도록 `depends_on` 설정

### 4. 타임아웃 설정
- LLM 추론은 시간이 걸릴 수 있습니다
- 백엔드: `ai.service.timeout: 30000` (30초)
- 프론트엔드: `AbortSignal.timeout(10000)` (10초)
- 필요시 타임아웃 값 조정

## 🔧 문제 해결

### 문제 1: LLM 서비스가 시작되지 않음

```bash
# 로그 확인
docker-compose logs llm-service

# 주요 원인:
# - 모델 파일이 없음 → models 디렉토리 확인
# - 메모리 부족 → Docker 메모리 할당 증가
# - 의존성 설치 실패 → Dockerfile 빌드 로그 확인
```

### 문제 2: 백엔드에서 LLM 서비스 호출 실패

```bash
# 네트워크 연결 테스트
docker exec -it pms-backend curl http://llm-service:8000/health

# 해결 방법:
# - LLM 서비스가 실행 중인지 확인
# - docker-compose.yml의 네트워크 설정 확인
# - 방화벽 설정 확인
```

### 문제 3: 프론트엔드에서 응답이 없음

```bash
# 백엔드 API 직접 테스트
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"sessionId":null,"message":"테스트"}'

# 해결 방법:
# - 브라우저 개발자 도구의 Network 탭 확인
# - CORS 설정 확인
# - JWT 토큰 유효성 확인
```

## 📈 성능 최적화

### 1. 모델 선택
- 더 작은 모델 사용 (2B ~ 7B 파라미터)
- 양자화된 모델 사용 (Q4, Q5, Q6)

### 2. 컨텍스트 길이 조절
- `n_ctx` 값 조정 (app.py:36)
- 최근 메시지만 컨텍스트로 전달 (ChatService.java:61)

### 3. Redis 캐싱
- 자주 묻는 질문은 Redis에서 빠르게 응답
- 캐시 TTL 조정

### 4. GPU 활용 (선택사항)
- CUDA 지원 Docker 이미지 사용
- llama-cpp-python GPU 빌드

## 🔐 보안 고려사항

1. **JWT Secret 변경**: 프로덕션에서 강력한 시크릿 키 사용
2. **API 인증**: 모든 API 엔드포인트에 인증 적용
3. **입력 검증**: 사용자 입력 검증 및 새니타이징
4. **Rate Limiting**: API 호출 제한 설정

## 📝 추가 개선 사항

### 단기
- [ ] 에러 핸들링 강화
- [ ] 로깅 개선 (구조화된 로그)
- [ ] 성능 모니터링 (Prometheus + Grafana)

### 중기
- [ ] 스트리밍 응답 지원
- [ ] 다중 모델 지원
- [ ] A/B 테스트 기능

### 장기
- [ ] GPU 가속 지원
- [ ] 모델 핫 스왑 기능
- [ ] 분산 처리 (다중 LLM 인스턴스)

## 📞 지원

문제가 발생하면 다음을 확인하세요:
1. 로그 파일 검토
2. 헬스체크 엔드포인트 확인
3. 네트워크 연결 테스트
4. 리소스 사용량 모니터링
