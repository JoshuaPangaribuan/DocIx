# DocIx Infrastructure Quick Start Guide

## 🚀 Quick Setup (Windows)

### Prerequisites
- Docker Desktop installed and running
- Git (optional)

### 1. Setup Infrastructure
```bash
cd infrastructures
make dev-setup
```

### 2. Start Development Environment
```bash
# Start all services
make dev-up

# View logs
make dev-logs
```

### 3. Run DocIx Application
```bash
cd ..
gradlew bootRun
```

## 🔧 Available Services

| Service | URL | Credentials |
|---------|-----|-------------|
| DocIx App | http://localhost:8080 | - |
| MinIO Console | http://localhost:9001 | minioadmin/minioadmin |
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| Elasticsearch | http://localhost:9200 | - |
| Kibana | http://localhost:5601 | - |

## 📋 Common Commands

### Infrastructure Management
```bash
# Start development environment
make dev-up

# Stop development environment
make dev-down

# View logs
make dev-logs

# Check service health
make health-check

# Clean everything (removes data)
make dev-clean
```

### Application Management
```bash
# Build application
make build-app

# Build Docker image
make docker-build

# Deploy to production
make deploy
```

## 🛠️ Troubleshooting

### Port Conflicts
If you get port conflicts, check what's running:
```bash
netstat -ano | findstr :9000
netstat -ano | findstr :5672
netstat -ano | findstr :9200
```

### Memory Issues
Elasticsearch needs sufficient memory:
- Increase Docker Desktop memory to 4GB+
- Adjust ES_JAVA_OPTS in docker-compose files

### Permission Issues
On Windows with WSL2:
```bash
# Fix Elasticsearch permissions
docker exec -it docix-elasticsearch-dev chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/data
```

## 📊 Monitoring

Start monitoring stack:
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/docix123)

## 🔐 Security Notes

### Development
- Uses default credentials for easy setup
- Security features disabled for simplicity

### Production
- Change all default passwords in .env.prod
- Enable security features
- Use proper SSL certificates
- Restrict network access

## 📁 File Structure
```
infrastructures/
├── docker-compose.yml          # Production setup
├── docker-compose.dev.yml      # Development setup
├── docker-compose.monitoring.yml # Monitoring stack
├── .env.dev                     # Development variables
├── .env.prod                    # Production variables
├── Makefile                     # Management commands
├── config/
│   ├── prometheus/             # Monitoring config
│   └── rabbitmq/              # Message broker config
└── init-scripts/
    └── postgres/              # Database initialization
```
