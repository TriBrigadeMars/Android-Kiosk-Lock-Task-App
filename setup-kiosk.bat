@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "ADB=platform-tools\adb.exe"

echo ============================================
echo  CU Denver Equity - Kiosk Setup
echo ============================================
echo.

REM Check if ADB exists
if not exist "%ADB%" (
    echo [ERROR] ADB not found at %ADB%
    echo Run setup-tools.bat first to install the SDK.
    pause
    exit /b 1
)

echo [1/4] Checking device connection...
"%ADB%" devices 2>nul | findstr "device$" >nul
if errorlevel 1 (
    echo [ERROR] No device connected. Please:
    echo   1. Enable USB Debugging on your tablet
    echo   2. Connect via USB and accept the prompt
    echo   3. Run this script again
    pause
    exit /b 1
)
echo         Device connected.

echo [2/4] Building and installing the kiosk APK...
call bash build-apk.sh
if not exist "build\CU-Denver-Equity-Kiosk.apk" (
    echo [ERROR] APK build failed.
    pause
    exit /b 1
)
"%ADB%" install -r "build\CU-Denver-Equity-Kiosk.apk" 2>&1 | findstr /i "Success\|INSTALL_FAILED_UPDATE_INCOMPATIBLE" >nul
if errorlevel 1 (
    echo [WARNING] Install may have failed. Trying uninstall first...
    "%ADB%" uninstall com.example.webkiosk >nul 2>&1
    "%ADB%" install "build\CU-Denver-Equity-Kiosk.apk"
) else (
    echo         Kiosk APK installed successfully.
)

echo [3/4] Setting kiosk app as device owner...
"%ADB%" shell dpm set-device-owner com.example.webkiosk/.KioskDeviceAdminReceiver 2>&1 | findstr /i "already\|Success" >nul
if errorlevel 1 (
    echo [WARNING] Could not set device owner. It may already be set.
    echo         If Test DPC was the previous device owner, you may need to factory reset first.
) else (
    echo         Kiosk app is now the device owner.
)

echo [4/4] Verifying lock task allowlist...
REM The kiosk app configures the allowlist on first launch.
"%ADB%" shell dumpsys device_policy 2>nul | findstr "com.example.webkiosk" >nul
if errorlevel 1 (
    echo [WARNING] App might not be allowlisted yet. Open the kiosk app once
    echo         so it can configure Chrome and WiFi settings for lock task mode.
) else (
    echo         App is allowlisted for lock task mode.
)

echo.
echo ============================================
echo  SETUP COMPLETE
echo ============================================
echo.
echo To enter kiosk mode, tap the "CU Denver Equity"
echo app icon on your tablet's home screen.
echo.
echo Chrome and WiFi settings can now run in lock task mode.
echo.
echo To exit kiosk mode, run: exit-kiosk.bat
echo.
pause
