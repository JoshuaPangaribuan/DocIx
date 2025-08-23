#!/bin/bash
# DocIx Infrastructure Setup Script

set -e

echo "🚀 Setting up DocIx Infrastructure..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker and Docker Compose are installed${NC}"

# Create necessary directories
echo -e "${YELLOW}📁 Creating necessary directories...${NC}"
mkdir -p data/{postgres,minio,rabbitmq,elasticsearch,redis}
mkdir -p logs
mkdir -p backups

echo -e "${GREEN}✅ Directories created${NC}"

# Set proper permissions for Elasticsearch
echo -e "${YELLOW}🔐 Setting permissions for Elasticsearch...${NC}"
sudo chown -R 1000:1000 data/elasticsearch || echo -e "${YELLOW}⚠️  Could not set Elasticsearch permissions. You may need to run with sudo.${NC}"

# Copy environment files if they don't exist
if [ ! -f .env.dev ]; then
    echo -e "${YELLOW}📄 Creating development environment file...${NC}"
    cp .env.dev.example .env.dev 2>/dev/null || echo -e "${YELLOW}⚠️  Please create .env.dev file manually${NC}"
fi

if [ ! -f .env.prod ]; then
    echo -e "${YELLOW}📄 Creating production environment file...${NC}"
    cp .env.prod.example .env.prod 2>/dev/null || echo -e "${YELLOW}⚠️  Please create .env.prod file manually${NC}"
fi

# Pull Docker images
echo -e "${YELLOW}📥 Pulling Docker images...${NC}"
docker-compose -f docker-compose.dev.yml pull
docker-compose -f docker-compose.yml pull
docker-compose -f docker-compose.monitoring.yml pull

echo -e "${GREEN}✅ Docker images pulled successfully${NC}"

# Start development environment
echo -e "${YELLOW}🔄 Starting development environment...${NC}"
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Wait for services to be healthy
echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"
sleep 30

# Initialize MinIO buckets
echo -e "${YELLOW}🪣 Initializing MinIO buckets...${NC}"
docker exec docix-minio-dev mc alias set local http://localhost:9000 minioadmin minioadmin || true
docker exec docix-minio-dev mc mb local/docix-documents --ignore-existing || true

echo -e "${GREEN}✅ MinIO buckets initialized${NC}"

# Check service health
echo -e "${YELLOW}🩺 Checking service health...${NC}"

check_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f $url > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is healthy${NC}"
            return 0
        fi
        echo -e "${YELLOW}⏳ Waiting for $service_name... (attempt $attempt/$max_attempts)${NC}"
        sleep 5
        ((attempt++))
    done

    echo -e "${RED}❌ $service_name failed to start${NC}"
    return 1
}

check_service "MinIO" "http://localhost:9000/minio/health/live"
check_service "RabbitMQ" "http://localhost:15672"
check_service "Elasticsearch" "http://localhost:9200/_cluster/health"

echo -e "${GREEN}🎉 DocIx Infrastructure setup completed successfully!${NC}"
echo ""
echo -e "${YELLOW}📋 Service URLs:${NC}"
echo -e "  • MinIO Console: http://localhost:9001 (minioadmin/minioadmin)"
echo -e "  • RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo -e "  • Elasticsearch: http://localhost:9200"
echo ""
echo -e "${YELLOW}📖 Next steps:${NC}"
echo -e "  1. Review and update .env.dev and .env.prod files"
echo -e "  2. Build and run the DocIx application"
echo -e "  3. Test document upload and search functionality"
echo ""
echo -e "${YELLOW}💡 Useful commands:${NC}"
echo -e "  • make help              - Show all available commands"
echo -e "  • make dev-logs          - View service logs"
echo -e "  • make health-check      - Check service health"
echo -e "  • make dev-down          - Stop development environment"
