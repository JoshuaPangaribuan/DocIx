# DocIx Infrastructure

This directory contains all the infrastructure configuration files for the DocIx (Document Indexer Search Engine) project.

## Services Overview

### Core Services
- **PostgreSQL**: Primary database for metadata storage
- **MinIO**: Object storage for PDF documents
- **Elasticsearch**: Search engine for full-text search and indexing
- **RabbitMQ**: Message broker for asynchronous document processing

### Optional Services
- **Kibana**: Elasticsearch monitoring and visualization
- **Redis**: Caching layer (optional)

## Quick Start

### Development Environment
```bash
# Start development infrastructure
make dev-up

# Stop development infrastructure
make dev-down

# View logs
make dev-logs
```

### Production Environment
```bash
# Start production infrastructure
make prod-up

# Stop production infrastructure
make prod-down

# Deploy application
make deploy
```

## Environment Files

Create `.env` files for different environments:

### Development (.env.dev)
```env
COMPOSE_PROJECT_NAME=docix-dev
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest
ES_HEAP_SIZE=256m
```

### Production (.env.prod)
```env
COMPOSE_PROJECT_NAME=docix-prod
POSTGRES_DB=docix
POSTGRES_USER=docix
POSTGRES_PASSWORD=your_secure_password
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your_secure_password
RABBITMQ_DEFAULT_USER=docix
RABBITMQ_DEFAULT_PASS=your_secure_password
ES_HEAP_SIZE=1g
```

## Service URLs

### Development
- Application: http://localhost:8080
- MinIO Console: http://localhost:9001
- RabbitMQ Management: http://localhost:15672
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

### Production
Same ports but with production security configurations.

## Data Volumes

All services use Docker volumes for data persistence:
- `postgres-data`: PostgreSQL database files
- `minio-data`: MinIO object storage files
- `rabbitmq-data`: RabbitMQ queue data
- `esdata`: Elasticsearch indices

## Security Notes

### Development
- Uses default credentials for ease of development
- Security features are disabled for Elasticsearch

### Production
- Use strong passwords for all services
- Enable security features
- Use environment variables for sensitive data
- Consider using Docker secrets

## Monitoring & Health Checks

All services include health checks:
- Database connectivity checks
- Service-specific health endpoints
- Dependency waiting with health conditions

## Backup & Recovery

Backup important volumes:
```bash
# Backup PostgreSQL
docker exec docix-postgres pg_dump -U docix docix > backup.sql

# Backup MinIO
docker exec docix-minio mc mirror /data /backup

# Backup Elasticsearch
docker exec docix-elasticsearch curl -X POST "localhost:9200/_snapshot/backup"
```

## Troubleshooting

### Common Issues
1. **Port conflicts**: Ensure ports 5432, 9000, 9001, 5672, 15672, 9200, 5601 are available
2. **Memory issues**: Adjust ES_JAVA_OPTS for Elasticsearch memory
3. **Permission issues**: Check Docker volume permissions

### Logs
```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f elasticsearch
```

## Scaling

### Horizontal Scaling
- Add more Elasticsearch nodes
- Scale RabbitMQ with clustering
- Use MinIO distributed mode

### Vertical Scaling
- Increase memory allocation
- Add more CPU cores
- Use SSD storage for better performance
