@echo off
chcp 65001 > nul
echo ====================================
echo PMS 프로젝트 재시작 스크립트
echo ====================================
echo.

cd /d "%~dp0"

echo 어떤 서비스를 재시작하시겠습니까?
echo.
echo 1. 전체 서비스
echo 2. 백엔드만
echo 3. 프론트엔드만
echo 4. 데이터베이스 (PostgreSQL + Redis)
echo.

choice /c 1234 /n /m "선택 (1-4): "

if errorlevel 4 (
    echo.
    echo 데이터베이스를 재시작합니다...
    docker-compose restart postgres redis
    echo ✓ PostgreSQL과 Redis가 재시작되었습니다.
    goto end
)
if errorlevel 3 (
    echo.
    echo 프론트엔드를 재시작합니다...
    docker-compose restart frontend
    echo ✓ 프론트엔드가 재시작되었습니다.
    goto end
)
if errorlevel 2 (
    echo.
    echo 백엔드를 재시작합니다...
    docker-compose restart backend
    echo ✓ 백엔드가 재시작되었습니다.
    goto end
)
if errorlevel 1 (
    echo.
    echo 전체 서비스를 재시작합니다...
    docker-compose restart
    echo ✓ 모든 서비스가 재시작되었습니다.
    goto end
)

:end
echo.
echo 💡 상태 확인: status.bat
echo 💡 로그 확인: logs.bat
echo.
pause
