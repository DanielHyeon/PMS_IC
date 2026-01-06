@echo off
chcp 65001 > nul
echo ====================================
echo PMS 프로젝트 로그 확인
echo ====================================
echo.

cd /d "%~dp0"

echo 어떤 서비스의 로그를 확인하시겠습니까?
echo.
echo 1. 전체 로그
echo 2. 백엔드 로그
echo 3. 프론트엔드 로그
echo 4. PostgreSQL 로그
echo 5. Redis 로그
echo.

choice /c 12345 /n /m "선택 (1-5): "

if errorlevel 5 (
    echo.
    echo Redis 로그를 확인합니다...
    docker-compose logs -f redis
    goto end
)
if errorlevel 4 (
    echo.
    echo PostgreSQL 로그를 확인합니다...
    docker-compose logs -f postgres
    goto end
)
if errorlevel 3 (
    echo.
    echo 프론트엔드 로그를 확인합니다...
    docker-compose logs -f frontend
    goto end
)
if errorlevel 2 (
    echo.
    echo 백엔드 로그를 확인합니다...
    docker-compose logs -f backend
    goto end
)
if errorlevel 1 (
    echo.
    echo 전체 로그를 확인합니다...
    docker-compose logs -f
    goto end
)

:end
echo.
echo 로그 확인을 종료하려면 Ctrl+C를 누르세요.
pause
