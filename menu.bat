@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

:menu
cls
echo ====================================
echo   PMS 프로젝트 관리 메뉴
echo ====================================
echo.
echo 1. 🚀 프로젝트 시작
echo 2. 🛑 프로젝트 중지
echo 3. 🔄 서비스 재시작
echo 4. 📊 상태 확인
echo 5. 📋 로그 확인
echo 6. 🌐 브라우저에서 열기
echo 7. 🔧 고급 옵션
echo 0. 종료
echo.
echo ====================================

choice /c 12345670 /n /m "선택하세요 (0-7): "

if errorlevel 8 goto exit
if errorlevel 7 goto advanced
if errorlevel 6 goto open_browser
if errorlevel 5 goto logs
if errorlevel 4 goto status
if errorlevel 3 goto restart
if errorlevel 2 goto stop
if errorlevel 1 goto start

:start
cls
call start.bat
goto menu

:stop
cls
call stop.bat
goto menu

:restart
cls
call restart.bat
goto menu

:status
cls
call status.bat
goto menu

:logs
cls
call logs.bat
goto menu

:open_browser
cls
echo ====================================
echo 브라우저에서 서비스 열기
echo ====================================
echo.
echo 1. Frontend (http://localhost:5173)
echo 2. Backend API (http://localhost:8080)
echo 3. PgAdmin (http://localhost:5050)
echo 4. Redis Commander (http://localhost:8082)
echo 5. 모두 열기
echo 0. 메인 메뉴로
echo.

choice /c 123450 /n /m "선택하세요 (0-5): "

if errorlevel 6 goto menu
if errorlevel 5 (
    start http://localhost:5173
    start http://localhost:8080
    start http://localhost:5050
    start http://localhost:8082
    goto menu
)
if errorlevel 4 (
    start http://localhost:8082
    goto menu
)
if errorlevel 3 (
    start http://localhost:5050
    goto menu
)
if errorlevel 2 (
    start http://localhost:8080
    goto menu
)
if errorlevel 1 (
    start http://localhost:5173
    goto menu
)

:advanced
cls
echo ====================================
echo 고급 옵션
echo ====================================
echo.
echo 1. 이미지 다시 빌드
echo 2. 데이터베이스 초기화 (주의!)
echo 3. 모든 데이터 삭제 (주의!)
echo 4. Docker 리소스 정리
echo 5. 개발 환경 초기 설정
echo 0. 메인 메뉴로
echo.

choice /c 123450 /n /m "선택하세요 (0-5): "

if errorlevel 6 goto menu
if errorlevel 5 goto dev_setup
if errorlevel 4 goto cleanup
if errorlevel 3 goto delete_all
if errorlevel 2 goto reset_db
if errorlevel 1 goto rebuild

:rebuild
cls
echo 이미지를 다시 빌드합니다...
docker-compose build
echo.
echo ✓ 빌드가 완료되었습니다.
pause
goto menu

:reset_db
cls
echo ⚠️  경고: 데이터베이스의 모든 데이터가 삭제됩니다!
echo.
choice /c YN /n /m "계속하시겠습니까? (Y/N): "
if errorlevel 2 goto menu
echo.
echo 데이터베이스를 초기화합니다...
docker-compose stop postgres
docker volume rm pms_postgres_data 2>nul
docker-compose up -d postgres
echo.
echo ✓ 데이터베이스가 초기화되었습니다.
pause
goto menu

:delete_all
cls
echo ⚠️  경고: 모든 컨테이너와 데이터가 삭제됩니다!
echo.
choice /c YN /n /m "정말로 삭제하시겠습니까? (Y/N): "
if errorlevel 2 goto menu
echo.
echo 모든 데이터를 삭제합니다...
docker-compose down -v
echo.
echo ✓ 모든 데이터가 삭제되었습니다.
pause
goto menu

:cleanup
cls
echo Docker 리소스를 정리합니다...
echo.
echo 사용하지 않는 이미지 삭제 중...
docker image prune -a -f
echo.
echo 사용하지 않는 볼륨 삭제 중...
docker volume prune -f
echo.
echo ✓ 정리가 완료되었습니다.
pause
goto menu

:dev_setup
cls
echo ====================================
echo 개발 환경 초기 설정
echo ====================================
echo.
echo .env 파일을 생성하고 Docker Compose를 시작합니다...
echo.
if not exist .env (
    copy .env.example .env
    echo ✓ .env 파일이 생성되었습니다.
) else (
    echo ✓ .env 파일이 이미 존재합니다.
)
echo.
echo Docker Compose를 시작합니다...
docker-compose up -d
echo.
echo ====================================
echo ✅ 개발 환경 설정이 완료되었습니다!
echo ====================================
echo.
pause
goto menu

:exit
cls
echo.
echo PMS 프로젝트 관리 메뉴를 종료합니다.
echo.
exit /b 0
