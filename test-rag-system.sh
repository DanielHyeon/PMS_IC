#!/bin/bash

# RAG 시스템 테스트 스크립트
# 전체 RAG 플로우를 테스트합니다.

set -e

echo "======================================"
echo "RAG 시스템 통합 테스트"
echo "======================================"
echo ""

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 서비스 헬스체크
echo "1. 서비스 헬스체크..."
echo "-----------------------------------"

echo -n "ChromaDB 체크: "
if curl -s http://localhost:8001/api/v1/heartbeat > /dev/null 2>&1; then
    echo -e "${GREEN}✓ OK${NC}"
else
    echo -e "${RED}✗ FAIL${NC}"
    echo "ChromaDB가 실행되지 않았습니다. docker-compose up -d chromadb 를 실행하세요."
    exit 1
fi

echo -n "LLM 서비스 체크: "
HEALTH=$(curl -s http://localhost:8000/health)
if echo "$HEALTH" | grep -q "healthy"; then
    echo -e "${GREEN}✓ OK${NC}"
    RAG_LOADED=$(echo "$HEALTH" | grep -o '"rag_service_loaded":[^,}]*' | cut -d':' -f2)
    if [ "$RAG_LOADED" = "true" ]; then
        echo -e "  └─ RAG 서비스: ${GREEN}로드됨${NC}"
    else
        echo -e "  └─ RAG 서비스: ${YELLOW}로드 안됨${NC}"
    fi
else
    echo -e "${RED}✗ FAIL${NC}"
    exit 1
fi

echo -n "백엔드 체크: "
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ OK${NC}"
else
    echo -e "${RED}✗ FAIL${NC}"
    exit 1
fi

echo ""

# 2. RAG 통계 확인
echo "2. RAG 통계 확인..."
echo "-----------------------------------"
STATS=$(curl -s http://localhost:8000/api/documents/stats)
echo "$STATS" | grep -q "total_chunks" && echo -e "${GREEN}✓ ChromaDB 연결 성공${NC}" || echo -e "${RED}✗ ChromaDB 연결 실패${NC}"
echo "현재 상태: $STATS"
echo ""

# 3. 테스트 문서 인덱싱
echo "3. 테스트 문서 인덱싱..."
echo "-----------------------------------"
TEST_DOC='{
  "documents": [
    {
      "id": "test-doc-1",
      "content": "PMS 프로젝트는 2024년 1월에 시작되었습니다. 주요 기능으로는 프로젝트 관리, 일정 관리, 리스크 관리가 있습니다. 개발 기간은 총 6개월이며, Spring Boot와 React를 사용합니다.",
      "metadata": {
        "source": "test",
        "type": "project_plan",
        "date": "2024-01-01"
      }
    },
    {
      "id": "test-doc-2",
      "content": "기술 스택은 다음과 같습니다. 백엔드: Spring Boot 3.2.1, Java 17, PostgreSQL 15. 프론트엔드: React 18, TypeScript. AI: Gemma 3 12B, ChromaDB, RAG.",
      "metadata": {
        "source": "test",
        "type": "tech_spec",
        "date": "2024-01-15"
      }
    }
  ]
}'

RESPONSE=$(curl -s -X POST http://localhost:8000/api/documents \
  -H "Content-Type: application/json" \
  -d "$TEST_DOC")

if echo "$RESPONSE" | grep -q "success_count"; then
    SUCCESS_COUNT=$(echo "$RESPONSE" | grep -o '"success_count":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ 테스트 문서 인덱싱 성공 (${SUCCESS_COUNT}개)${NC}"
else
    echo -e "${RED}✗ 인덱싱 실패${NC}"
    echo "응답: $RESPONSE"
fi
echo ""

# 4. RAG 검색 테스트
echo "4. RAG 검색 테스트..."
echo "-----------------------------------"

# 검색 1: 프로젝트 시작일
echo "질문 1: 프로젝트는 언제 시작했나요?"
SEARCH1=$(curl -s -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "프로젝트 시작일", "top_k": 2}')

if echo "$SEARCH1" | grep -q "2024년 1월"; then
    echo -e "${GREEN}✓ 관련 문서 검색 성공${NC}"
    echo "검색 결과 (발췌): $(echo "$SEARCH1" | grep -o '2024년 1월[^"]*' | head -1)"
else
    echo -e "${YELLOW}⚠ 관련 문서를 찾지 못했습니다${NC}"
fi
echo ""

# 검색 2: 기술 스택
echo "질문 2: 사용하는 데이터베이스는?"
SEARCH2=$(curl -s -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "데이터베이스", "top_k": 2}')

if echo "$SEARCH2" | grep -q "PostgreSQL"; then
    echo -e "${GREEN}✓ 관련 문서 검색 성공${NC}"
    echo "검색 결과 (발췌): PostgreSQL 15"
else
    echo -e "${YELLOW}⚠ 관련 문서를 찾지 못했습니다${NC}"
fi
echo ""

# 5. 통합 채팅 테스트 (선택사항 - 로그인 필요)
echo "5. 통합 채팅 테스트 (백엔드)"
echo "-----------------------------------"
echo -e "${YELLOW}이 단계는 JWT 토큰이 필요합니다.${NC}"
echo "수동으로 테스트하려면 다음을 실행하세요:"
echo ""
echo "# 로그인"
echo 'curl -X POST http://localhost:8080/api/auth/login \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"email": "admin@insure.com", "password": "admin123"}'"'"' | grep -o '"'"'eyJ[^"]*'"'"
echo ""
echo "# 채팅 (TOKEN을 위에서 받은 토큰으로 교체)"
echo 'curl -X POST http://localhost:8080/api/chat/message \'
echo '  -H "Authorization: Bearer TOKEN" \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"sessionId": null, "message": "프로젝트는 언제 시작했나요?"}'"'"
echo ""

# 6. 최종 통계
echo "6. 최종 RAG 통계"
echo "-----------------------------------"
FINAL_STATS=$(curl -s http://localhost:8000/api/documents/stats)
echo "$FINAL_STATS"
echo ""

echo "======================================"
echo -e "${GREEN}RAG 시스템 테스트 완료!${NC}"
echo "======================================"
echo ""
echo "다음 단계:"
echo "1. 프론트엔드 접속: http://localhost:5173"
echo "2. admin@insure.com / admin123 로 로그인"
echo "3. 프로젝트 → 단계별 산출물 업로드 (PDF/Word)"
echo "4. AI 챗봇으로 업로드한 문서에 대해 질문"
echo ""
echo "자세한 내용은 RAG_시스템_가이드.md 를 참고하세요."
