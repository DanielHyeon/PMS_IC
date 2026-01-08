@echo off
chcp 65001 > nul
echo ====================================
echo PMS 프로젝트 실행 스크립트
echo ====================================
echo.

cd /d "%~dp0"

echo [1/5] 환경 변수 설정 확인...
if not exist .env (
    echo .env 파일이 없습니다. .env.example을 복사합니다...
    copy .env.example .env
    echo ✓ .env 파일이 생성되었습니다.
) else (
    echo ✓ .env 파일이 이미 존재합니다.
)
echo.

echo [2/5] Docker 실행 확인...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Docker가 설치되어 있지 않거나 실행되지 않습니다.
    echo   Docker Desktop을 설치하고 실행한 후 다시 시도해주세요.
    pause
    exit /b 1
)
echo ✓ Docker가 실행 중입니다.
echo.

echo [3/5] Docker Compose 실행 확인...
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Docker Compose가 설치되어 있지 않습니다.
    pause
    exit /b 1
)
echo ✓ Docker Compose가 설치되어 있습니다.
echo.

echo [4/5] 기존 컨테이너 중지...
docker-compose down >nul 2>&1
echo ✓ 기존 컨테이너를 중지했습니다.
echo.

echo [5/5] 전체 서비스 시작...
echo    이 작업은 몇 분 정도 걸릴 수 있습니다...
docker-compose up -d

if %errorlevel% equ 0 (
    echo.
    echo ====================================
    echo ✅ 모든 서비스가 성공적으로 시작되었습니다!
    echo ====================================
    echo.
    echo 📌 접속 URL:
    echo    Frontend:      http://localhost:5173
    echo    Backend API:   http://localhost:8080
    echo    PgAdmin:       http://localhost:5050 (admin@pms.com / admin)
    echo    Redis GUI:     http://localhost:8082
    echo.
    echo 📋 유용한 명령어:
    echo    로그 확인:     docker-compose logs -f
    echo    상태 확인:     docker-compose ps
    echo    서비스 중지:   docker-compose down
    echo.
    echo 🌐 브라우저에서 Frontend를 여시겠습니까? (Y/N)
    choice /c YN /n /m "선택: "
    if errorlevel 2 goto end
    if errorlevel 1 start http://localhost:5173
) else (
    echo.
    echo ✗ 서비스 시작 중 오류가 발생했습니다.
    echo   로그를 확인하려면: docker-compose logs
    pause
    exit /b 1
)

:end
echo.
echo 로그를 실시간으로 보려면 다음 명령을 실행하세요:
echo    docker-compose logs -f
echo.
pause
