#!/bin/bash
#
# OpenMetadata 스택 중지 스크립트
#

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "=================================================="
echo "  🛑 Stopping OpenMetadata Stack"
echo "=================================================="
echo ""

# 프로젝트 루트 디렉토리로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# OpenMetadata 서비스 중지
echo -e "${BLUE}ℹ️  Stopping OpenMetadata services...${NC}"

docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml stop \
    openmetadata-ingestion \
    openmetadata-server \
    openmetadata-elasticsearch \
    openmetadata-mysql

echo -e "${GREEN}✅ OpenMetadata services stopped${NC}"
echo ""
echo "  💡 Note: PMS core services (postgres, neo4j, etc.) are still running"
echo "  💡 To remove containers: docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml rm -f openmetadata-*"
echo ""
