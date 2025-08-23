@echo off
REM DocIx Graceful Shutdown Test Script for Windows

echo 🧪 Testing DocIx Graceful Shutdown
echo ==================================

set DOCIX_URL=http://localhost:8081
set ACTUATOR_URL=%DOCIX_URL%/actuator

echo Step 1: Checking initial application status
curl -s -o nul -w "%%{http_code}" %DOCIX_URL%/api/system/status > temp_status.txt 2>nul
set /p STATUS_CODE=<temp_status.txt
del temp_status.txt

if "%STATUS_CODE%"=="200" (
    echo ✅ Application is running

    REM Get system status
    curl -s %DOCIX_URL%/api/system/status 2>nul > temp_response.json
    echo Current system status available at: %DOCIX_URL%/api/system/status
    del temp_response.json 2>nul
) else (
    echo ❌ Application is not running or not responding
    echo HTTP Status Code: %STATUS_CODE%
    pause
    exit /b 1
)

echo.
echo Step 2: Triggering graceful shutdown
echo 💡 You can trigger shutdown using one of these methods:
echo    1. Send Ctrl+C in the terminal running the application
echo    2. Use Actuator endpoint: curl -X POST %ACTUATOR_URL%/shutdown
echo    3. Close the terminal/command window running the application

echo.
echo Triggering shutdown via Actuator endpoint...
curl -s -X POST %ACTUATOR_URL%/shutdown 2>nul
if %errorlevel% equ 0 (
    echo ✅ Shutdown request sent successfully
) else (
    echo ⚠️ Failed to send shutdown request via Actuator
    echo You can manually stop the application using Ctrl+C
)

echo.
echo Step 3: Monitoring shutdown process
echo ⏳ Please check the application console for shutdown logs...
echo.
echo Expected shutdown sequence:
echo    1. 🛑 Graceful shutdown initiated for DocIx application
echo    2. 📋 Starting graceful shutdown sequence...
echo    3. 1️⃣ Stopping acceptance of new requests...
echo    4. 2️⃣ Waiting for ongoing document processing to complete...
echo    5. 3️⃣ Closing external connections...
echo    6. 4️⃣ Cleaning up resources...
echo    7. ✅ Graceful shutdown completed successfully

echo.
echo Step 4: Verification endpoints
echo You can check these endpoints during shutdown:
echo    • System Status: %DOCIX_URL%/api/system/status
echo    • Health Check: %DOCIX_URL%/actuator/health
echo    • Readiness: %DOCIX_URL%/api/system/health/ready
echo    • Liveness: %DOCIX_URL%/api/system/health/live

echo.
echo 🎉 Graceful shutdown test script completed
echo Check the application logs to see detailed shutdown sequence.
pause
