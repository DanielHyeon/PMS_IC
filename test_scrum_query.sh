#!/bin/bash

echo "=== Testing Scrum Query ==="
echo ""
echo "Sending query: 스크럼이란?"
echo ""

curl -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "스크럼이란?",
    "context": []
  }' | jq '.'

echo ""
echo "=== Check logs ==="
docker compose logs --tail=50 llm-service | grep -E "(Intent|strategy|search_impl|Retrieved|Found)"
