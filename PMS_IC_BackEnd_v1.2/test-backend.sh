#!/bin/bash

# 백엔드 테스트 실행 스크립트

set -e

echo "=========================================="
echo "PMS Backend 테스트 실행 스크립트"
echo "=========================================="

# 프로젝트 루트로 이동
cd "$(dirname "$0")"

# Java 버전 확인
echo "Java 버전 확인 중..."
if ! command -v java &> /dev/null; then
    echo "❌ Java가 설치되어 있지 않습니다."
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
    exit 1
fi
echo "✅ Maven 버전: $(mvn -version | head -n 1)"

echo ""
echo "=========================================="
echo "테스트 실행 중..."
echo "=========================================="
echo "프로필: test (H2 인메모리 데이터베이스 사용)"
echo ""

# 테스트 실행
mvn clean test

echo ""
echo "=========================================="
echo "테스트 완료!"
echo "=========================================="


