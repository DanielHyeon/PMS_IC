# PMS 트러블슈팅 가이드

## 📋 목차

- [AI 챗봇 관련 문제](#ai-챗봇-관련-문제)
- [Docker 컨테이너 문제](#docker-컨테이너-문제)
- [데이터베이스 연결 문제](#데이터베이스-연결-문제)
- [포트 충돌 문제](#포트-충돌-문제)
- [성능 문제](#성능-문제)

---

## AI 챗봇 관련 문제

### 문제: "Mock 모드로 동작 중입니다" 메시지가 표시됨

**증상:**
```json
{
  "reply": "안녕하세요! PMS AI 어시스턴트입니다. 현재 Mock 모드로 동작 중입니다..."
}
```

**원인:**
1. LLM 서비스가 실행되지 않음
2. LLM 모델 파일이 없음
3. Backend가 잘못된 URL로 연결 시도
4. 환경 변수 설정 오류

**해결 방법:**

#### 1단계: LLM 서비스 상태 확인
```bash
# 컨테이너 실행 확인
docker ps | grep pms-llm-service

# 헬스 체크
curl http://localhost:8000/health

# 예상 응답:
# {
#   "status": "healthy",
#   "model_loaded": true,
#   "rag_service_loaded": true,
#   "chat_workflow_loaded": true
# }
```

#### 2단계: 모델 파일 확인
```bash
# 호스트에서 모델 파일 확인
ls -lh llm-service/models/

# 컨테이너 내부에서 확인
docker exec pms-llm-service ls -lh /app/models/

# 모델 파일이 없으면 다운로드 필요:
# - LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf (2GB)
# - google.gemma-3-12b-pt.Q5_K_M.gguf (7.9GB)
```

#### 3단계: Backend 환경 변수 확인
```bash
# 환경 변수 확인
docker exec pms-backend env | grep AI_SERVICE

# 예상 출력:
# AI_SERVICE_URL=http://llm-service:8000
# AI_SERVICE_MOCK_URL=http://mockserver:1080
```

**환경 변수가 잘못된 경우:**
```bash
# docker-compose.yml 수정
# backend 섹션에서:
# AI_SERVICE_URL: http://llm-service:8000  # ✅ 올바름
# AI_TEAM_API_URL: ...                     # ❌ 잘못됨

# 수정 후 재시작
docker compose up -d --force-recreate backend
```

#### 4단계: Volume Mount 확인
```bash
# docker-compose.yml에서 llm-service volume 확인
# 올바른 설정:
# volumes:
#   - ./llm-service:/app

# 잘못된 설정 (이전 버전):
# volumes:
#   - ./models:/app/models  # ❌ 이 줄이 있으면 제거
#   - ./llm-service:/app
```

#### 5단계: Backend 로그 확인
```bash
# 연결 실패 로그 확인
docker logs pms-backend 2>&1 | grep -i "ai service\|falling back"

# Connection refused 오류가 있다면:
# - llm-service가 실행 중인지 확인
# - 네트워크 설정 확인 (pms-network)
# - 환경 변수 확인
```

---

### 문제: LLM 응답이 너무 느림

**증상:**
- 챗봇 응답이 30초 이상 소요
- 타임아웃 발생

**해결 방법:**

```bash
# GPU 사용 확인
docker exec pms-llm-service nvidia-smi

# GPU 메모리 부족 시 레이어 수 조정
# docker-compose.override.yml:
# llm-service:
#   environment:
#     LLM_N_GPU_LAYERS: 30  # 50에서 30으로 감소

# 더 작은 모델 사용
# LFM2-2.6B (2GB) 대신 MinerU2.5 (483MB)
```

---

### 문제: RAG 검색이 작동하지 않음

**증상:**
- 문서 내용과 관련 없는 답변
- "문서를 찾을 수 없습니다" 메시지

**해결 방법:**

```bash
# Neo4j에 데이터가 있는지 확인
docker exec -it pms-neo4j cypher-shell -u neo4j -p pmspassword123

# Cypher 쿼리 실행:
MATCH (d:Document) RETURN count(d);
MATCH (c:Chunk) RETURN count(c);

# 데이터가 0이면 인덱싱 필요:
# 1. PDF 파일을 ragdata 폴더에 추가
cp your-documents.pdf llm-service/ragdata/

# 2. 인덱싱 실행
docker exec pms-llm-service python3 /app/load_ragdata_pdfs_neo4j.py --ragdata-dir /app/ragdata

# 3. 인덱스 확인
docker logs pms-llm-service | grep "indexed"
```

---

## Docker 컨테이너 문제

### 문제: 컨테이너가 시작 직후 종료됨

```bash
# 상태 확인
docker compose ps

# 종료된 컨테이너 로그 확인
docker compose logs backend

# 자주 발생하는 원인:
# 1. 데이터베이스 연결 실패
# 2. 환경 변수 누락
# 3. 포트 충돌
# 4. 메모리 부족
```

**해결:**
```bash
# 1. 의존성 순서대로 시작
docker compose up -d postgres redis neo4j
sleep 10
docker compose up -d backend
docker compose up -d llm-service
docker compose up -d frontend

# 2. 강제 재생성
docker compose down
docker compose up -d --force-recreate

# 3. 이미지 재빌드
docker compose build --no-cache backend
docker compose up -d backend
```

---

### 문제: Backend가 PostgreSQL에 연결할 수 없음

**증상:**
```
java.net.UnknownHostException: postgres
```

**해결:**
```bash
# 네트워크 확인
docker network ls | grep pms

# docker-compose.override.yml 확인
# backend 섹션에 networks가 있는지 확인:
# services:
#   backend:
#     networks:
#       - pms-network

# 추가 후 재시작
docker compose up -d --force-recreate backend
```

---

## 포트 충돌 문제

### 포트 8083이 이미 사용 중

```bash
# 사용 중인 프로세스 확인 (Linux)
lsof -i :8083

# 프로세스 종료
kill -9 <PID>

# 또는 다른 포트 사용
# docker-compose.yml에서:
# backend:
#   ports:
#     - "8084:8080"  # 8084로 변경

# 프론트엔드 .env 파일도 업데이트:
# VITE_API_URL=http://localhost:8084/api
```

---

## 데이터베이스 연결 문제

### PostgreSQL 연결 테스트

```bash
# 데이터베이스 준비 확인
docker exec pms-postgres pg_isready -U pms_user

# 수동 연결 테스트
docker exec -it pms-postgres psql -U pms_user -d pms_db

# 연결 성공 시:
# pms_db=#

# 테이블 확인
\dt auth.*;

# 종료
\q
```

---

## 성능 문제

### 메모리 사용량 확인

```bash
# 전체 컨테이너 리소스 사용량
docker stats

# 특정 서비스
docker stats pms-llm-service pms-backend

# 메모리 부족 시:
# 1. LLM GPU 레이어 감소
# 2. 작은 모델 사용
# 3. Docker Desktop 메모리 할당 증가
```

---

## 로그 분석

### 유용한 로그 명령어

```bash
# 모든 서비스 로그 (실시간)
docker compose logs -f

# 특정 서비스만
docker compose logs -f backend

# 마지막 100줄
docker compose logs --tail=100 llm-service

# 타임스탬프 포함
docker compose logs -t backend

# 에러만 필터링
docker compose logs backend 2>&1 | grep -i error

# 특정 키워드 검색
docker compose logs backend | grep -i "connection refused"
```

---

## 완전 초기화 (마지막 수단)

```bash
# ⚠️ 주의: 모든 데이터가 삭제됩니다!

# 1. 모든 컨테이너 중지 및 삭제
docker compose down -v

# 2. 모든 이미지 삭제
docker compose down --rmi all

# 3. 캐시 없이 재빌드
docker compose build --no-cache

# 4. 재시작
docker compose up -d

# 5. 로그 확인
docker compose logs -f
```

---

## 문의 및 지원

문제가 해결되지 않으면:
1. GitHub Issues에 다음 정보와 함께 등록:
   - 에러 메시지 전문
   - `docker compose ps` 출력
   - 관련 서비스 로그
   - OS 및 Docker 버전
2. 로그 파일 첨부:
   ```bash
   docker compose logs > logs.txt
   ```
