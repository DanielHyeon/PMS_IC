#!/bin/bash
#
# OpenMetadata 스택 시작 스크립트
# 
# 사용법: ./scripts/start-openmetadata.sh [--full|--minimal]
#
# Options:
#   --full     OpenMetadata + Ingestion 서비스 모두 시작
#   --minimal  OpenMetadata 서버만 시작 (기본값)
#

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 프로젝트 루트 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# 옵션 파싱
MODE="minimal"
if [[ "$1" == "--full" ]]; then
    MODE="full"
elif [[ "$1" == "--minimal" ]]; then
    MODE="minimal"
fi

echo ""
echo "=================================================="
echo "  🚀 OpenMetadata Stack Startup Script"
echo "  Mode: $MODE"
echo "=================================================="
echo ""

# 1. 필수 파일 확인
log_info "Checking required files..."

if [[ ! -f "docker-compose.yml" ]]; then
    log_error "docker-compose.yml not found!"
    exit 1
fi

if [[ ! -f "docker-compose.openmetadata.yml" ]]; then
    log_error "docker-compose.openmetadata.yml not found!"
    exit 1
fi

log_success "Required files found"

# 2. 환경변수 확인
log_info "Checking environment variables..."

if [[ -f ".env" ]]; then
    source .env
fi

# 기본값 설정
export OM_MYSQL_ROOT_PASSWORD="${OM_MYSQL_ROOT_PASSWORD:-openmetadata_root_password}"
export OM_MYSQL_PASSWORD="${OM_MYSQL_PASSWORD:-openmetadata_password}"
export OM_JWT_TOKEN="${OM_JWT_TOKEN:-}"

log_success "Environment variables configured"

# 3. PMS 핵심 서비스 상태 확인
log_info "Checking PMS core services..."

PMS_SERVICES_RUNNING=true

if ! docker-compose ps postgres | grep -q "Up"; then
    log_warning "PostgreSQL is not running"
    PMS_SERVICES_RUNNING=false
fi

if ! docker-compose ps neo4j | grep -q "Up"; then
    log_warning "Neo4j is not running"
    PMS_SERVICES_RUNNING=false
fi

if [[ "$PMS_SERVICES_RUNNING" == "false" ]]; then
    log_info "Starting PMS core services first..."
    docker-compose up -d postgres redis neo4j
    
    log_info "Waiting for services to be healthy (60 seconds)..."
    sleep 60
fi

log_success "PMS core services are running"

# 4. OpenMetadata 서비스 시작
log_info "Starting OpenMetadata services..."

if [[ "$MODE" == "full" ]]; then
    # 전체 모드: 모든 서비스 시작
    docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml up -d \
        openmetadata-mysql \
        openmetadata-elasticsearch \
        openmetadata-server \
        openmetadata-ingestion
else
    # 최소 모드: 서버만 시작 (Ingestion 제외)
    docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml up -d \
        openmetadata-mysql \
        openmetadata-elasticsearch \
        openmetadata-server
fi

log_success "OpenMetadata containers started"

# 5. 서비스 준비 대기
log_info "Waiting for OpenMetadata to be ready..."
echo ""

MAX_RETRIES=30
RETRY_COUNT=0
READY=false

while [[ $RETRY_COUNT -lt $MAX_RETRIES ]]; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    
    # Health check
    if curl -s -f "http://localhost:8585/api/v1/system/version" > /dev/null 2>&1; then
        READY=true
        break
    fi
    
    echo -n "."
    sleep 5
done

echo ""

if [[ "$READY" == "true" ]]; then
    log_success "OpenMetadata is ready!"
else
    log_error "OpenMetadata failed to start within timeout"
    log_info "Check logs with: docker-compose -f docker-compose.yml -f docker-compose.openmetadata.yml logs openmetadata-server"
    exit 1
fi

# 6. 서비스 정보 출력
echo ""
echo "=================================================="
echo "  📊 OpenMetadata is now running!"
echo "=================================================="
echo ""
echo "  🌐 Web UI:        http://localhost:8585"
echo "  🔑 Default Login: admin / admin"
echo ""
echo "  📝 Quick Start:"
echo "     1. Access http://localhost:8585"
echo "     2. Login with admin/admin"
echo "     3. Go to Settings > Services > Add Database Service"
echo "     4. Configure 'pms-postgres' with:"
echo "        - Host: postgres"
echo "        - Port: 5432"
echo "        - Database: ${POSTGRES_DB:-pms_db}"
echo "        - Username: ${POSTGRES_USER:-pms_user}"
echo ""

if [[ "$MODE" == "full" ]]; then
    echo "  🔄 Ingestion UI:  http://localhost:8586"
fi

echo ""
echo "  📚 Documentation: /docs/OpenMetadata_도입_로드맵.md"
echo ""
echo "  🛑 To stop:       ./scripts/stop-openmetadata.sh"
echo ""
echo "=================================================="
