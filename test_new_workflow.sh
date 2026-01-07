#!/bin/bash

echo "=== 테스트 1: 스크럼 (RAG 데이터 있음) ==="
curl -s -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "스크럼이란?", "context": []}' | jq -r '.reply' | head -5
echo ""

echo "=== 테스트 2: 새로운 주제 - 데브옵스 (키워드 없음, RAG 데이터 있을 수 있음) ==="
curl -s -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "데브옵스란?", "context": []}' | jq -r '.reply' | head -5
echo ""

echo "=== 테스트 3: 완전히 범위 밖 - 요리 (RAG 데이터 없음) ==="
curl -s -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "김치찌개 만드는 법", "context": []}' | jq -r '.reply'
echo ""

echo "=== 테스트 4: 인사말 ==="
curl -s -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕", "context": []}' | jq -r '.reply'
echo ""

echo "=== 로그 확인 ==="
docker compose logs --tail=100 llm-service | grep -E "(Simple|Refining|intent|RAG)" | tail -30
