@echo off
chcp 65001 > nul
echo ====================================
echo PMS 프로젝트 중지 스크립트
echo ====================================
echo.

cd /d "%~dp0"

echo 서비스를 중지하는 중...
docker-compose down

if %errorlevel% equ 0 (
    echo.
    echo ✅ 모든 서비스가 중지되었습니다.
    echo.
    echo 💡 참고:
    echo    - 데이터는 유지됩니다.
    echo    - 모든 데이터를 삭제하려면: docker-compose down -v
    echo    - 다시 시작하려면: start.bat 실행
) else (
    echo.
    echo ✗ 서비스 중지 중 오류가 발생했습니다.
)

echo.
pause
