# PMS Docker 개발 환경 간편 명령어

.PHONY: help up down logs restart clean build test db-migrate db-reset db-backup db-restore

# 기본 명령어
help: ## 도움말 표시
	@echo "PMS Docker 개발 환경 명령어"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## 전체 서비스 시작
	docker-compose up -d
	@echo "✅ 서비스가 시작되었습니다"
	@echo "Frontend: http://localhost:5173"
	@echo "Backend:  http://localhost:8080"
	@echo "PgAdmin:  http://localhost:5050"

down: ## 서비스 중지
	docker-compose down
	@echo "✅ 서비스가 중지되었습니다"

logs: ## 전체 로그 확인
	docker-compose logs -f

logs-backend: ## 백엔드 로그 확인
	docker-compose logs -f backend

logs-frontend: ## 프론트엔드 로그 확인
	docker-compose logs -f frontend

restart: ## 전체 서비스 재시작
	docker-compose restart
	@echo "✅ 서비스가 재시작되었습니다"

restart-backend: ## 백엔드만 재시작
	docker-compose restart backend
	@echo "✅ 백엔드가 재시작되었습니다"

restart-frontend: ## 프론트엔드만 재시작
	docker-compose restart frontend
	@echo "✅ 프론트엔드가 재시작되었습니다"

clean: ## 컨테이너 삭제 (데이터 유지)
	docker-compose down
	@echo "✅ 컨테이너가 삭제되었습니다"

clean-all: ## 모든 데이터 삭제 (주의!)
	docker-compose down -v
	@echo "⚠️  모든 데이터가 삭제되었습니다"

build: ## 이미지 다시 빌드
	docker-compose build
	@echo "✅ 이미지가 빌드되었습니다"

build-backend: ## 백엔드 이미지만 빌드
	docker-compose build backend
	@echo "✅ 백엔드 이미지가 빌드되었습니다"

build-frontend: ## 프론트엔드 이미지만 빌드
	docker-compose build frontend
	@echo "✅ 프론트엔드 이미지가 빌드되었습니다"

# 데이터베이스 관련
db-shell: ## PostgreSQL 쉘 접속
	docker-compose exec postgres psql -U pms_user -d pms_db

db-schemas: ## 스키마 목록 확인
	docker-compose exec postgres psql -U pms_user -d pms_db -c "\dn"

db-tables: ## 테이블 목록 확인
	docker-compose exec postgres psql -U pms_user -d pms_db -c "\dt auth.*; \dt project.*; \dt task.*; \dt chat.*;"

db-backup: ## 데이터베이스 백업
	docker-compose exec postgres pg_dump -U pms_user pms_db > backup_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "✅ 백업이 완료되었습니다"

db-restore: ## 데이터베이스 복원 (파일: backup.sql)
	docker-compose exec -T postgres psql -U pms_user -d pms_db < backup.sql
	@echo "✅ 복원이 완료되었습니다"

db-reset: ## 데이터베이스 초기화 (주의!)
	docker-compose down -v
	docker-compose up -d postgres redis
	sleep 5
	@echo "✅ 데이터베이스가 초기화되었습니다"

# Redis 관련
redis-shell: ## Redis CLI 접속
	docker-compose exec redis redis-cli

redis-keys: ## Redis 키 목록
	docker-compose exec redis redis-cli KEYS "*"

redis-flush: ## Redis 데이터 전체 삭제 (주의!)
	docker-compose exec redis redis-cli FLUSHALL
	@echo "⚠️  Redis 데이터가 삭제되었습니다"

# 테스트
test: ## 전체 테스트 실행
	docker-compose run --rm backend ./mvnw test
	docker-compose run --rm frontend npm test

test-backend: ## 백엔드 테스트
	docker-compose run --rm backend ./mvnw test

test-frontend: ## 프론트엔드 테스트
	docker-compose run --rm frontend npm test

# 모니터링
status: ## 서비스 상태 확인
	docker-compose ps

stats: ## 리소스 사용량 확인
	docker stats pms-backend pms-frontend pms-postgres pms-redis

health: ## 헬스체크 확인
	@echo "Backend Health:"
	@curl -f http://localhost:8080/actuator/health || echo "❌ Backend 응답 없음"
	@echo ""
	@echo "Frontend:"
	@curl -f http://localhost:5173 > /dev/null 2>&1 && echo "✅ Frontend 정상" || echo "❌ Frontend 응답 없음"
	@echo ""
	@echo "PostgreSQL:"
	@docker-compose exec postgres pg_isready -U pms_user && echo "✅ PostgreSQL 정상" || echo "❌ PostgreSQL 응답 없음"
	@echo ""
	@echo "Redis:"
	@docker-compose exec redis redis-cli ping > /dev/null 2>&1 && echo "✅ Redis 정상" || echo "❌ Redis 응답 없음"

# 프로덕션
prod-up: ## 프로덕션 모드로 실행
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
	@echo "✅ 프로덕션 모드로 실행되었습니다"

prod-down: ## 프로덕션 서비스 중지
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml down

prod-build: ## 프로덕션 이미지 빌드
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

# 개발 편의
dev-setup: ## 개발 환경 초기 설정
	@echo "🚀 개발 환경 설정 중..."
	cp .env.example .env || true
	docker-compose up -d
	@echo "✅ 개발 환경 설정 완료!"
	@echo ""
	@echo "다음 URL로 접속하세요:"
	@echo "  Frontend:  http://localhost:5173"
	@echo "  Backend:   http://localhost:8080"
	@echo "  PgAdmin:   http://localhost:5050"
	@echo "  Redis GUI: http://localhost:8082"

open: ## 브라우저에서 서비스 열기
	@echo "브라우저에서 서비스를 여는 중..."
	@start http://localhost:5173 || open http://localhost:5173 || xdg-open http://localhost:5173
