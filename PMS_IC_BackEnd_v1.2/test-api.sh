#!/bin/bash

# 백엔드 API 테스트 스크립트
# 백엔드가 실행 중이어야 합니다.

set -e

BASE_URL="http://localhost:8080"
TEST_EMAIL="admin@insure.com"
TEST_PASSWORD="admin123"

echo "=========================================="
echo "PMS Backend API 테스트"
echo "=========================================="

# Health Check
echo ""
echo "1. Health Check 테스트..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Health Check 성공"
    echo "   응답: $BODY"
else
    echo "❌ Health Check 실패 (HTTP $HTTP_CODE)"
    echo "   백엔드가 실행 중인지 확인하세요."
    exit 1
fi

# 로그인 테스트
echo ""
echo "2. 로그인 테스트..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ 로그인 성공"
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    if [ -z "$TOKEN" ]; then
        TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    fi
    if [ -n "$TOKEN" ]; then
        echo "   토큰 획득 성공"
        echo "   토큰: ${TOKEN:0:50}..."
    else
        echo "   ⚠️  토큰을 찾을 수 없습니다."
        echo "   응답: $BODY"
    fi
else
    echo "❌ 로그인 실패 (HTTP $HTTP_CODE)"
    echo "   응답: $BODY"
    exit 1
fi

# API 엔드포인트 테스트 (토큰이 있는 경우)
if [ -n "$TOKEN" ]; then
    echo ""
    echo "3. 인증이 필요한 API 테스트..."
    
    # Dashboard Stats 테스트
    echo ""
    echo "   - Dashboard Stats 테스트..."
    DASHBOARD_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/dashboard/stats" \
      -H "Authorization: Bearer $TOKEN")
    
    HTTP_CODE=$(echo "$DASHBOARD_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ Dashboard Stats 성공"
    else
        echo "   ⚠️  Dashboard Stats 실패 (HTTP $HTTP_CODE)"
    fi
    
    # Users 테스트
    echo ""
    echo "   - Users API 테스트..."
    USERS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/users" \
      -H "Authorization: Bearer $TOKEN")
    
    HTTP_CODE=$(echo "$USERS_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ Users API 성공"
    else
        echo "   ⚠️  Users API 실패 (HTTP $HTTP_CODE)"
    fi
fi

# Swagger UI 확인
echo ""
echo "4. Swagger UI 확인..."
SWAGGER_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/swagger-ui.html")
HTTP_CODE=$(echo "$SWAGGER_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ]; then
    echo "✅ Swagger UI 접근 가능"
    echo "   URL: $BASE_URL/swagger-ui.html"
else
    echo "⚠️  Swagger UI 접근 실패 (HTTP $HTTP_CODE)"
fi

echo ""
echo "=========================================="
echo "API 테스트 완료!"
echo "=========================================="
echo ""
echo "다음 URL에서 API 문서를 확인하세요:"
echo "  - Swagger UI: $BASE_URL/swagger-ui.html"
echo "  - OpenAPI JSON: $BASE_URL/api-docs"
echo ""


