# PMS Backend v1.2

Spring Boot 기반 프로젝트 관리 시스템 백엔드

## 기술 스택

- **Framework:** Spring Boot 3.2.1
- **Language:** Java 17
- **Database:** PostgreSQL 15 (prod), H2 (test)
- **Cache:** Redis 7
- **Security:** Spring Security + JWT
- **API Docs:** Swagger/OpenAPI 3

## 🚀 빠른 시작

### 1. 의존성 서비스 시작

```bash
# 프로젝트 루트에서 실행
cd /wp/PMS_IC
docker-compose up -d postgres redis
```

### 2. 백엔드 실행

```bash
cd PMS_IC_BackEnd_v1.2
./run-backend.sh
```

### 3. 테스트 실행

```bash
cd PMS_IC_BackEnd_v1.2
./test-backend.sh
```

## 📚 상세 가이드

**자세한 실행 및 테스트 가이드는 [BACKEND_RUN_GUIDE.md](./BACKEND_RUN_GUIDE.md)를 참조하세요.**

## 실행 방법

### 방법 1: 스크립트 사용 (권장)

```bash
# 의존성 확인
./check-dependencies.sh

# 백엔드 실행
./run-backend.sh

# 테스트 실행
./test-backend.sh
```

### 방법 2: Maven 직접 실행

```bash
# 백엔드 실행
mvn spring-boot:run

# 테스트 실행
mvn clean test
```

### 방법 3: Docker Compose (전체 시스템)

```bash
# 프로젝트 루트에서
docker-compose up -d

# 백엔드만 재시작
docker-compose restart backend

# 로그 확인
docker-compose logs -f backend
```

## API 문서

백엔드 실행 후:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## 테스트 계정

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| 관리자 | admin@insure.com | admin123 |
| 개발자 | dev@insure.com | admin123 |
| PM | pm@insure.com | admin123 |

## API 엔드포인트 예시

### 로그인

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@insure.com","password":"admin123"}'
```

### API 호출 (인증 필요)

```bash
curl -X GET http://localhost:8080/api/dashboard/stats \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 문제 해결

자세한 문제 해결 방법은 [BACKEND_RUN_GUIDE.md](./BACKEND_RUN_GUIDE.md#-문제-해결)를 참조하세요.

## 관련 문서

- [백엔드 실행 가이드](./BACKEND_RUN_GUIDE.md)
- [프로젝트 README](../README.md)
- [실행 가이드](../실행가이드.md)
