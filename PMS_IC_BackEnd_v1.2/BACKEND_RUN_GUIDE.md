# 백엔드 실행 및 테스트 가이드

## 📋 사전 요구사항

### 필수
- **Java 17 이상**
- **Maven 3.6 이상**
- **Docker & Docker Compose** (의존성 서비스용)

### 확인 방법

```bash
# Java 버전 확인
java -version

# Maven 버전 확인
mvn -version

# Docker 확인
docker --version
docker-compose --version
```

## 🚀 빠른 시작

### 1. 의존성 서비스 시작

백엔드 실행 전에 PostgreSQL과 Redis가 필요합니다:

```bash
# 프로젝트 루트에서 실행
cd /wp/PMS_IC

# 필수 서비스만 시작
docker-compose up -d postgres redis

# 또는 전체 서비스 시작
docker-compose up -d
```

### 2. 의존성 서비스 확인

```bash
cd PMS_IC_BackEnd_v1.2
chmod +x check-dependencies.sh
./check-dependencies.sh
```

### 3. 백엔드 실행

```bash
cd PMS_IC_BackEnd_v1.2
chmod +x run-backend.sh
./run-backend.sh
```

또는 Maven으로 직접 실행:

```bash
cd PMS_IC_BackEnd_v1.2
mvn spring-boot:run
```

### 4. 백엔드 테스트

```bash
cd PMS_IC_BackEnd_v1.2
chmod +x test-backend.sh
./test-backend.sh
```

또는 Maven으로 직접 실행:

```bash
cd PMS_IC_BackEnd_v1.2
mvn clean test
```

## 📝 상세 가이드

### 환경 변수 설정

백엔드는 다음 환경 변수를 사용합니다:

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/pms_db
export SPRING_DATASOURCE_USERNAME=pms_user
export SPRING_DATASOURCE_PASSWORD=pms_password
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379
export AI_TEAM_API_URL=http://localhost:8000
export AI_TEAM_MOCK_URL=http://localhost:1080
export JWT_SECRET=your-secret-key-change-in-production-must-be-at-least-256-bits-long
```

`run-backend.sh` 스크립트는 자동으로 이 환경 변수들을 설정합니다.

### 프로파일

백엔드는 3가지 프로파일을 지원합니다:

- **dev**: 개발 환경 (기본값)
  - PostgreSQL 사용
  - SQL 로그 출력
  - Hot Reload 활성화

- **test**: 테스트 환경
  - H2 인메모리 데이터베이스 사용
  - 테스트 전용 설정

- **prod**: 프로덕션 환경
  - PostgreSQL 사용
  - SQL 로그 비활성화
  - 보안 강화

### 실행 방법

#### 방법 1: 스크립트 사용 (권장)

```bash
cd PMS_IC_BackEnd_v1.2
./run-backend.sh
```

#### 방법 2: Maven 직접 실행

```bash
cd PMS_IC_BackEnd_v1.2
mvn spring-boot:run
```

#### 방법 3: JAR 파일 실행

```bash
cd PMS_IC_BackEnd_v1.2
mvn clean package
java -jar target/pms-backend-1.2.0.jar
```

### 테스트 실행

#### 전체 테스트

```bash
cd PMS_IC_BackEnd_v1.2
./test-backend.sh
# 또는
mvn clean test
```

#### 특정 테스트 클래스만 실행

```bash
mvn test -Dtest=PmsApplicationTests
```

#### 특정 패키지 테스트

```bash
mvn test -Dtest=com.insuretech.pms.auth.*
```

#### 테스트 커버리지 확인

```bash
mvn clean test jacoco:report
# 리포트: target/site/jacoco/index.html
```

## 🔍 확인 사항

### 백엔드가 정상 실행되었는지 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Swagger UI
# 브라우저에서 http://localhost:8080/swagger-ui.html 접속
```

### 로그 확인

백엔드는 콘솔에 로그를 출력합니다. 주요 로그:

- `Started PmsApplication` - 백엔드 시작 완료
- `HikariPool` - 데이터베이스 연결 풀 초기화
- `Redis` - Redis 연결 확인

## 🛠️ 문제 해결

### 포트 8080이 이미 사용 중인 경우

```bash
# 포트 사용 확인
netstat -ano | grep 8080
# 또는
lsof -i :8080

# application.yml에서 포트 변경
# server.port: 8081
```

### 데이터베이스 연결 실패

1. PostgreSQL이 실행 중인지 확인:
```bash
docker ps | grep pms-postgres
```

2. 포트 확인 (기본값: 5433):
```bash
docker-compose ps postgres
```

3. 연결 테스트:
```bash
docker-compose exec postgres psql -U pms_user -d pms_db -c "SELECT 1;"
```

### Redis 연결 실패

1. Redis가 실행 중인지 확인:
```bash
docker ps | grep pms-redis
```

2. 연결 테스트:
```bash
docker-compose exec redis redis-cli ping
```

### Maven 빌드 실패

```bash
# Maven 캐시 정리
mvn clean

# 의존성 다시 다운로드
mvn dependency:resolve

# 전체 재빌드
mvn clean install
```

### Java 버전 문제

Java 17 이상이 필요합니다:

```bash
# Java 버전 확인
java -version

# JAVA_HOME 설정 확인
echo $JAVA_HOME

# Java 17 설치 (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-17-jdk
```

## 📚 API 문서

백엔드 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## 🔗 관련 문서

- [프로젝트 README](../README.md)
- [실행 가이드](../실행가이드.md)
- [Docker 가이드](../README_DOCKER.md)

## 💡 개발 팁

### Hot Reload 활성화

Spring Boot DevTools가 포함되어 있어 코드 변경 시 자동 재시작됩니다.

### 디버그 모드 실행

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

IDE에서 Remote Debug 설정:
- Host: localhost
- Port: 5005

### 로그 레벨 변경

`application.yml`에서 로그 레벨 조정:

```yaml
logging:
  level:
    com.insuretech.pms: DEBUG
    org.springframework.web: DEBUG
```


