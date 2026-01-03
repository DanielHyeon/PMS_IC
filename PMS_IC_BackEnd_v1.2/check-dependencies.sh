#!/bin/bash

# 백엔드 의존성 서비스 확인 스크립트

echo "=========================================="
echo "PMS Backend 의존성 서비스 확인"
echo "=========================================="

# Docker 확인
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다."
    exit 1
fi

# Docker Compose 확인
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose가 설치되어 있지 않습니다."
    exit 1
fi

echo ""
echo "의존성 서비스 상태:"
echo "----------------------------------------"

# PostgreSQL 확인
if docker ps | grep -q pms-postgres; then
    echo "✅ PostgreSQL: 실행 중 (포트 5433)"
else
    echo "❌ PostgreSQL: 실행 중이 아님"
    echo "   시작: docker-compose up -d postgres"
fi

# Redis 확인
if docker ps | grep -q pms-redis; then
    echo "✅ Redis: 실행 중 (포트 6379)"
else
    echo "❌ Redis: 실행 중이 아님"
    echo "   시작: docker-compose up -d redis"
fi

# LLM Service 확인 (선택사항)
if docker ps | grep -q pms-llm-service; then
    echo "✅ LLM Service: 실행 중 (포트 8000)"
else
    echo "⚠️  LLM Service: 실행 중이 아님 (선택사항)"
    echo "   시작: docker-compose up -d llm-service"
fi

# ChromaDB 확인 (선택사항)
if docker ps | grep -q pms-chromadb; then
    echo "✅ ChromaDB: 실행 중 (포트 8001)"
else
    echo "⚠️  ChromaDB: 실행 중이 아님 (선택사항)"
    echo "   시작: docker-compose up -d chromadb"
fi

echo ""
echo "=========================================="
echo "필수 서비스 시작:"
echo "  docker-compose up -d postgres redis"
echo ""
echo "전체 서비스 시작:"
echo "  docker-compose up -d"
echo "=========================================="


