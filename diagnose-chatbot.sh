#!/bin/bash

# 챗봇 문제 진단 스크립트

echo "=========================================="
echo "챗봇 문제 진단 스크립트"
echo "=========================================="

# LLM 서비스 확인
echo ""
echo "1. LLM 서비스 상태 확인..."
LLM_HEALTH=$(curl -s -w "\n%{http_code}" http://localhost:8000/health 2>/dev/null)
LLM_HTTP_CODE=$(echo "$LLM_HEALTH" | tail -n1)
LLM_BODY=$(echo "$LLM_HEALTH" | sed '$d')

if [ "$LLM_HTTP_CODE" = "200" ]; then
    echo "✅ LLM 서비스 정상 (포트 8000)"
    echo "   응답: $LLM_BODY"
else
    echo "❌ LLM 서비스 오류 (HTTP $LLM_HTTP_CODE)"
    echo "   LLM 서비스가 실행 중인지 확인하세요:"
    echo "   docker-compose ps llm-service"
    echo "   docker-compose logs llm-service | tail -50"
fi

# Mock 서비스 확인
echo ""
echo "2. Mock 서비스 상태 확인..."
MOCK_HEALTH=$(curl -s -w "\n%{http_code}" http://localhost:1080/health 2>/dev/null)
MOCK_HTTP_CODE=$(echo "$MOCK_HEALTH" | tail -n1)

if [ "$MOCK_HTTP_CODE" = "200" ] || [ "$MOCK_HTTP_CODE" = "404" ]; then
    echo "✅ Mock 서비스 접근 가능 (포트 1080)"
else
    echo "❌ Mock 서비스 오류 (HTTP $MOCK_HTTP_CODE)"
    echo "   Mock 서비스가 실행 중인지 확인하세요:"
    echo "   docker-compose ps ai-service"
    echo "   docker-compose logs ai-service | tail -50"
fi

# 백엔드 확인
echo ""
echo "3. 백엔드 상태 확인..."
BACKEND_HEALTH=$(curl -s -w "\n%{http_code}" http://localhost:8080/actuator/health 2>/dev/null)
BACKEND_HTTP_CODE=$(echo "$BACKEND_HEALTH" | tail -n1)

if [ "$BACKEND_HTTP_CODE" = "200" ]; then
    echo "✅ 백엔드 정상 (포트 8080)"
else
    echo "❌ 백엔드 오류 (HTTP $BACKEND_HTTP_CODE)"
    echo "   백엔드가 실행 중인지 확인하세요:"
    echo "   docker-compose ps backend"
    echo "   docker-compose logs backend | tail -50"
fi

# LLM 서비스 직접 테스트
echo ""
echo "4. LLM 서비스 직접 테스트..."
LLM_TEST=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"안녕하세요","context":[],"retrieved_docs":[]}' 2>/dev/null)
LLM_TEST_CODE=$(echo "$LLM_TEST" | tail -n1)
LLM_TEST_BODY=$(echo "$LLM_TEST" | sed '$d')

if [ "$LLM_TEST_CODE" = "200" ]; then
    echo "✅ LLM 서비스 채팅 API 정상"
    echo "   응답: $(echo "$LLM_TEST_BODY" | head -c 100)..."
else
    echo "❌ LLM 서비스 채팅 API 오류 (HTTP $LLM_TEST_CODE)"
    echo "   응답: $LLM_TEST_BODY"
fi

# Mock 서비스 직접 테스트
echo ""
echo "5. Mock 서비스 직접 테스트..."
MOCK_TEST=$(curl -s -w "\n%{http_code}" -X POST http://localhost:1080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","message":"안녕하세요","context":[]}' 2>/dev/null)
MOCK_TEST_CODE=$(echo "$MOCK_TEST" | tail -n1)
MOCK_TEST_BODY=$(echo "$MOCK_TEST" | sed '$d')

if [ "$MOCK_TEST_CODE" = "200" ]; then
    echo "✅ Mock 서비스 채팅 API 정상"
    echo "   응답: $(echo "$MOCK_TEST_BODY" | head -c 100)..."
else
    echo "❌ Mock 서비스 채팅 API 오류 (HTTP $MOCK_TEST_CODE)"
    echo "   응답: $MOCK_TEST_BODY"
fi

# Docker 컨테이너 상태
echo ""
echo "6. Docker 컨테이너 상태..."
echo "----------------------------------------"
docker-compose ps | grep -E "llm-service|ai-service|backend" || echo "컨테이너를 찾을 수 없습니다."

# 최근 로그 확인
echo ""
echo "7. 최근 백엔드 로그 (챗봇 관련)..."
echo "----------------------------------------"
docker-compose logs backend 2>/dev/null | grep -i "chat\|llm\|mock\|fallback" | tail -10 || echo "로그를 찾을 수 없습니다."

echo ""
echo "=========================================="
echo "진단 완료"
echo "=========================================="
echo ""
echo "문제 해결 방법:"
echo "1. LLM 서비스 재시작: docker-compose restart llm-service"
echo "2. Mock 서비스 재시작: docker-compose restart ai-service"
echo "3. 백엔드 재시작: docker-compose restart backend"
echo "4. 전체 재시작: docker-compose restart"
echo ""


