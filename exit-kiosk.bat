@echo off
cd /d "%~dp0"
set "ADB=platform-tools\adb.exe"

echo ============================================
echo  CU Denver Equity - Exit Kiosk Mode
echo ============================================
echo.

if not exist "%ADB%" (
    echo [ERROR] ADB not found. Run setup-tools.bat first.
    pause
    exit /b 1
)

echo [1/2] Force-stopping the kiosk app...
"%ADB%" shell am force-stop com.example.webkiosk 2>nul
echo         App stopped.

echo [2/2] Checking if out of lock task mode...
echo.
echo Your tablet should now be back to normal.
echo.
echo To fully remove kiosk capability:
echo    Settings ^> Security ^> Device Admin Apps ^> deactivate Test DPC
echo.
pause
