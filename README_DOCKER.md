# PMS Docker 개발 환경 가이드

## 📦 구성 요소

```
┌─────────────────────────────────────────────────────┐
│                  Docker Compose                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │   Frontend   │  │   Backend    │  │ PostgreSQL│ │
│  │  React+Vite  │  │ Spring Boot  │  │  (DB)     │ │
│  │  :5173       │  │  :8083       │  │  :5432    │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │    Redis     │  │ LLM Service  │  │  PgAdmin  │ │
│  │  (Cache)     │  │  (Flask+GPU) │  │   (GUI)   │ │
│  │  :6379       │  │  :8000       │  │  :5050    │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐                │
│  │    Neo4j     │  │ MockServer   │                │
│  │(Graph+Vector)│  │  (Fallback)  │                │
│  │  :7474/:7687 │  │  :1080       │                │
│  └──────────────┘  └──────────────┘                │
│                                                      │
└─────────────────────────────────────────────────────┘
```

## 🚀 빠른 시작

### 1. 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# 필요시 수정
nano .env
```

### 2. 전체 환경 실행

```bash
# 전체 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스만 로그 확인
docker-compose logs -f backend
```

### 3. 서비스 접속

| 서비스 | URL | 설명 |
|--------|-----|------|
| **Frontend** | http://localhost:5173 | React 앱 |
| **Backend API** | http://localhost:8083 | Spring Boot API |
| **LLM Service** | http://localhost:8000 | AI 챗봇 (Flask + LLM + RAG) |
| **Neo4j Browser** | http://localhost:7474 | Graph DB GUI (neo4j / pmspassword123) |
| **PgAdmin** | http://localhost:5050 | PostgreSQL GUI (admin@pms.com / admin) |
| **Redis Commander** | http://localhost:8081 | Redis GUI |
| **MockServer** | http://localhost:1080 | AI Fallback Mock API |

### 4. 서비스 중지

```bash
# 서비스 중지 (데이터 유지)
docker-compose stop

# 서비스 중지 및 컨테이너 삭제 (데이터 유지)
docker-compose down

# 모든 데이터 삭제 (주의!)
docker-compose down -v
```

---

## 🛠️ 개발 워크플로우

### 백엔드 개발

```bash
# 백엔드만 재시작
docker-compose restart backend

# 백엔드 로그 실시간 확인
docker-compose logs -f backend

# 백엔드 컨테이너 접속
docker-compose exec backend bash

# 백엔드 빌드 다시 하기
docker-compose up -d --build backend
```

### 프론트엔드 개발

```bash
# 프론트엔드는 Hot Reload 활성화되어 있음
# 코드 수정하면 자동 반영

# 프론트엔드만 재시작
docker-compose restart frontend

# 프론트엔드 컨테이너 접속
docker-compose exec frontend sh
```

### 데이터베이스 작업

```bash
# PostgreSQL 접속
docker-compose exec postgres psql -U pms_user -d pms_db

# 스키마 확인
docker-compose exec postgres psql -U pms_user -d pms_db -c "\dn"

# 테이블 확인
docker-compose exec postgres psql -U pms_user -d pms_db -c "\dt auth.*"

# SQL 파일 실행
docker-compose exec -T postgres psql -U pms_user -d pms_db < schema.sql
```

### Redis 작업

```bash
# Redis CLI 접속
docker-compose exec redis redis-cli

# 모든 키 조회
docker-compose exec redis redis-cli KEYS "*"

# 특정 키 조회
docker-compose exec redis redis-cli GET "chat:session:U001"
```

---

## 🔧 고급 사용법

### 개별 서비스 실행

```bash
# 데이터베이스만 실행
docker-compose up -d postgres redis

# 백엔드만 실행 (DB 포함)
docker-compose up -d postgres redis backend

# 프론트엔드만 실행
docker-compose up -d frontend
```

### 프로덕션 모드

```bash
# 프로덕션 설정으로 실행
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 프로덕션 빌드
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build
```

### 리소스 정리

```bash
# 사용하지 않는 이미지 삭제
docker image prune -a

# 사용하지 않는 볼륨 삭제
docker volume prune

# 모든 PMS 관련 리소스 삭제
docker-compose down -v --rmi all
```

---

## 📊 서비스별 상세 정보

### PostgreSQL

- **Port:** 5432
- **User:** pms_user
- **Password:** pms_password
- **Database:** pms_db
- **Schemas:**
  - `auth` - 사용자, 권한
  - `project` - 프로젝트, 단계
  - `task` - 칸반, 백로그
  - `chat` - 챗봇 히스토리
  - `risk` - 리스크, 이슈
  - `report` - 리포팅 데이터

### Redis

- **Port:** 6379
- **용도:**
  - 세션 관리
  - 챗봇 대화 캐싱
  - API 응답 캐싱
  - Rate Limiting

### Spring Boot Backend

- **Port:** 8083
- **Profile:** dev
- **API Docs:** <http://localhost:8083/swagger-ui.html>
- **Actuator:** <http://localhost:8083/actuator/health>

### LLM Service (Flask)

- **Port:** 8000
- **용도:** AI 챗봇 (LLM + RAG)
- **모델:** LFM2-2.6B 또는 Gemma 3 12B
- **Health Check:** <http://localhost:8000/health>
- **모델 위치:** `llm-service/models/` 디렉토리

### Neo4j (GraphRAG)

- **Port:** 7474 (Browser), 7687 (Bolt)
- **용도:** RAG 벡터 검색 + 그래프 쿼리
- **Browser:** <http://localhost:7474>
- **계정:** neo4j / pmspassword123

### MockServer

- **Port:** 1080
- **용도:** LLM 서비스 장애 시 Fallback Mock 응답 제공

---

## 🐛 트러블슈팅

### 포트 충돌

```bash
# 사용 중인 포트 확인 (Linux)
lsof -i :5432
lsof -i :8083

# 사용 중인 포트 확인 (Windows)
netstat -ano | findstr :5432
netstat -ano | findstr :8083

# 프로세스 종료 (Linux)
kill -9 <PID>

# 프로세스 종료 (Windows)
taskkill /PID <PID> /F

# 또는 docker-compose.yml에서 포트 변경
# ports:
#   - "8084:8080"  # 8083 대신 8084 사용
```

### 컨테이너가 시작되지 않음

```bash
# 로그 확인
docker-compose logs backend

# 상태 확인
docker-compose ps

# 강제 재시작
docker-compose down
docker-compose up -d --force-recreate
```

### 데이터베이스 연결 실패

```bash
# PostgreSQL 헬스체크 확인
docker-compose ps postgres

# 수동 연결 테스트
docker-compose exec postgres pg_isready -U pms_user

# 백엔드 환경 변수 확인
docker-compose exec backend env | grep SPRING_DATASOURCE
```

### Hot Reload가 동작하지 않음

```bash
# 프론트엔드
# - docker-compose.override.yml 확인
# - volumes 마운트 확인

# 백엔드
# - Spring DevTools 활성화 확인
# - IDE에서 자동 빌드 활성화
```

---

## 📝 개발 팁

### 1. 로그 레벨 조정

```yaml
# docker-compose.override.yml에 추가
services:
  backend:
    environment:
      LOGGING_LEVEL_ROOT: DEBUG
```

### 2. 데이터 초기화

```bash
# 테스트 데이터 초기화 스크립트
docker-compose exec postgres psql -U pms_user -d pms_db -f /docker-entrypoint-initdb.d/02-seed-data.sql
```

### 3. 백업 & 복원

```bash
# 백업
docker-compose exec postgres pg_dump -U pms_user pms_db > backup.sql

# 복원
docker-compose exec -T postgres psql -U pms_user -d pms_db < backup.sql
```

### 4. 성능 모니터링

```bash
# 컨테이너 리소스 사용량 확인
docker stats

# 특정 서비스만
docker stats pms-backend pms-postgres
```

---

## 🔄 CI/CD 통합

### GitHub Actions 예시

```yaml
# .github/workflows/ci.yml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start services
        run: docker-compose up -d postgres redis

      - name: Run backend tests
        run: docker-compose run backend ./mvnw test

      - name: Run frontend tests
        run: docker-compose run frontend npm test
```

---

## 📚 추가 리소스

- [Docker Compose 문서](https://docs.docker.com/compose/)
- [Spring Boot Docker 가이드](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker 가이드](https://hub.docker.com/_/postgres)
- [Redis Docker 가이드](https://hub.docker.com/_/redis)

---

## ✅ 체크리스트

개발 환경 설정 시 확인:

- [ ] `.env` 파일 생성 및 설정
- [ ] Docker Desktop 실행 중
- [ ] 필요한 포트가 사용 가능한지 확인
- [ ] `docker-compose up -d` 실행
- [ ] 모든 서비스 헬스체크 통과
- [ ] Frontend <http://localhost:5173> 접속 확인
- [ ] Backend <http://localhost:8083/actuator/health> 확인
- [ ] LLM Service <http://localhost:8000/health> 확인 (model_loaded: true)
- [ ] Neo4j Browser <http://localhost:7474> 접속 확인
- [ ] PgAdmin 접속 및 DB 연결 확인

---

**문제가 발생하면 로그를 먼저 확인하세요:**
```bash
docker-compose logs -f
```
