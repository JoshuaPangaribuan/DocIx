@echo off
REM DocIx Infrastructure Setup Script for Windows

echo 🚀 Setting up DocIx Infrastructure...

REM Check if Docker is running
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

echo ✅ Docker is running

REM Check if Docker Compose is available
docker-compose version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose is not available. Please install Docker Compose.
    pause
    exit /b 1
)

echo ✅ Docker Compose is available

REM Create necessary directories
echo 📁 Creating necessary directories...
if not exist "data" mkdir data
if not exist "data\postgres" mkdir data\postgres
if not exist "data\minio" mkdir data\minio
if not exist "data\rabbitmq" mkdir data\rabbitmq
if not exist "data\elasticsearch" mkdir data\elasticsearch
if not exist "data\redis" mkdir data\redis
if not exist "logs" mkdir logs
if not exist "backups" mkdir backups

echo ✅ Directories created

REM Pull Docker images
echo 📥 Pulling Docker images...
docker-compose -f docker-compose.dev.yml pull
if %errorlevel% neq 0 (
    echo ❌ Failed to pull development images
    pause
    exit /b 1
)

echo ✅ Docker images pulled successfully

REM Start development environment
echo 🔄 Starting development environment...
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d
if %errorlevel% neq 0 (
    echo ❌ Failed to start development environment
    pause
    exit /b 1
)

echo ⏳ Waiting for services to be ready...
timeout /t 30 /nobreak >nul

REM Initialize MinIO buckets
echo 🪣 Initializing MinIO buckets...
docker exec docix-minio-dev mc alias set local http://localhost:9000 minioadmin minioadmin >nul 2>&1
docker exec docix-minio-dev mc mb local/docix-documents --ignore-existing >nul 2>&1

echo ✅ MinIO buckets initialized

echo 🎉 DocIx Infrastructure setup completed successfully!
echo.
echo 📋 Service URLs:
echo   • MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
echo   • RabbitMQ Management: http://localhost:15672 (guest/guest)
echo   • Elasticsearch: http://localhost:9200
echo.
echo 📖 Next steps:
echo   1. Review and update .env.dev and .env.prod files
echo   2. Build and run the DocIx application with: gradlew bootRun
echo   3. Test document upload and search functionality
echo.
echo 💡 Useful commands:
echo   • docker-compose -f docker-compose.dev.yml logs -f  - View service logs
echo   • docker-compose -f docker-compose.dev.yml down     - Stop services
echo.
pause
