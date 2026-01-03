#!/bin/bash

# 백엔드 실행 스크립트
# PostgreSQL과 Redis가 실행 중이어야 합니다.

set -e

echo "=========================================="
echo "PMS Backend 실행 스크립트"
echo "=========================================="

# 프로젝트 루트로 이동
cd "$(dirname "$0")"

# Java 버전 확인
echo "Java 버전 확인 중..."
if ! command -v java &> /dev/null; then
    echo "❌ Java가 설치되어 있지 않습니다."
    echo "Java 17 이상을 설치해주세요."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 이상이 필요합니다. 현재 버전: $JAVA_VERSION"
    exit 1
fi
echo "✅ Java 버전: $(java -version 2>&1 | head -n 1)"

# Maven 확인
echo ""
echo "Maven 확인 중..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven이 설치되어 있지 않습니다."
    echo "Maven을 설치해주세요: sudo apt install maven"
    exit 1
fi
echo "✅ Maven 버전: $(mvn -version | head -n 1)"

# 의존성 확인
echo ""
echo "의존성 서비스 확인 중..."

# PostgreSQL 확인
if ! docker ps | grep -q pms-postgres; then
    echo "⚠️  PostgreSQL이 실행 중이지 않습니다."
    echo "다음 명령으로 시작하세요: docker-compose up -d postgres"
    read -p "계속하시겠습니까? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ PostgreSQL 실행 중"
fi

# Redis 확인
if ! docker ps | grep -q pms-redis; then
    echo "⚠️  Redis가 실행 중이지 않습니다."
    echo "다음 명령으로 시작하세요: docker-compose up -d redis"
    read -p "계속하시겠습니까? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ Redis 실행 중"
fi

# 환경 변수 설정
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/pms_db
export SPRING_DATASOURCE_USERNAME=pms_user
export SPRING_DATASOURCE_PASSWORD=pms_password
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379
export AI_TEAM_API_URL=http://localhost:8000
export AI_TEAM_MOCK_URL=http://localhost:1080
export JWT_SECRET=your-secret-key-change-in-production-must-be-at-least-256-bits-long

echo ""
echo "=========================================="
echo "백엔드 실행 중..."
echo "=========================================="
echo "프로필: dev"
echo "포트: 8080"
echo "API 문서: http://localhost:8080/swagger-ui.html"
echo "Health Check: http://localhost:8080/actuator/health"
echo ""
echo "종료하려면 Ctrl+C를 누르세요."
echo "=========================================="
echo ""

# Maven으로 실행
mvn spring-boot:run


