@echo off
chcp 65001 > nul
echo ====================================
echo PMS 프로젝트 상태 확인
echo ====================================
echo.

cd /d "%~dp0"

echo [컨테이너 상태]
echo.
docker-compose ps
echo.

echo ====================================
echo [서비스 헬스체크]
echo ====================================
echo.

echo [Backend API]
curl -f http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Backend: 정상 작동 중 (http://localhost:8080)
) else (
    echo ✗ Backend: 응답 없음
)

echo [Frontend]
curl -f http://localhost:5173 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Frontend: 정상 작동 중 (http://localhost:5173)
) else (
    echo ✗ Frontend: 응답 없음
)

echo [PostgreSQL]
docker-compose exec -T postgres pg_isready -U pms_user >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ PostgreSQL: 정상 작동 중 (http://localhost:5432)
) else (
    echo ✗ PostgreSQL: 응답 없음
)

echo [Redis]
docker-compose exec -T redis redis-cli ping >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Redis: 정상 작동 중 (http://localhost:6379)
) else (
    echo ✗ Redis: 응답 없음
)

echo.
echo ====================================
echo [리소스 사용량]
echo ====================================
echo.
docker stats --no-stream pms-backend pms-frontend pms-postgres pms-redis 2>nul

echo.
echo ====================================
echo 💡 유용한 명령어:
echo    로그 확인:     logs.bat
echo    서비스 재시작: docker-compose restart
echo    서비스 중지:   stop.bat
echo ====================================
echo.
pause
