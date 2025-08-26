# DocIx Infrastructure

This directory contains all the infrastructure configuration files for the DocIx (Document Indexer Search Engine) project.

## Services Overview

### Core Services
- **PostgreSQL 15**: Primary database for document metadata storage
- **MinIO**: S3-compatible object storage for PDF documents
- **Elasticsearch 8.12**: Search engine for page-based document indexing and search
- **RabbitMQ 3**: Message broker for asynchronous document processing

### Optional Services
- **Kibana 8.12**: Elasticsearch monitoring and visualization dashboard

## Configuration Details

Based on `application.properties`, the services are configured as follows:

### Database Configuration
- **Host**: localhost:5432
- **Database**: docix
- **Username**: docix_user
- **Password**: docix_password
- **Extensions**: uuid-ossp, pg_trgm (for enhanced search capabilities)

### MinIO Configuration
- **API Endpoint**: http://localhost:9000
- **Console**: http://localhost:9001
- **Access Key**: minioadmin
- **Secret Key**: minioadmin
- **Bucket**: docix-documents (auto-created)

### Elasticsearch Configuration
- **Endpoint**: http://localhost:9200
- **Security**: Disabled for development
- **Memory**: 512MB (production), 256MB (development)

### RabbitMQ Configuration
- **AMQP Port**: 5672
- **Management UI**: http://localhost:15672
- **Username**: guest
- **Password**: guest
- **Queues**: document.processing (auto-created)

## Quick Start

### Prerequisites
- Docker and Docker Compose
- At least 4GB available RAM
- Ports 5432, 9000, 9001, 9200, 5672, 15672, 5601 available

### Development Environment
```bash
# Start all development services
docker-compose -f docker-compose.dev.yml up -d

# Start specific services only
docker-compose -f docker-compose.dev.yml up -d postgres-dev minio-dev

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop all services
docker-compose -f docker-compose.dev.yml down
```

### Production Environment
```bash
# Start production infrastructure with application
docker-compose up -d

# Start infrastructure only (without DocIx app)
docker-compose up -d postgres minio elasticsearch rabbitmq

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## Service URLs

When running locally:

| Service | URL | Description |
|---------|-----|-------------|
| DocIx Application | http://localhost:8081 | Main application |
| MinIO Console | http://localhost:9001 | Object storage management |
| RabbitMQ Management | http://localhost:15672 | Message queue monitoring |
| Elasticsearch | http://localhost:9200 | Search engine API |
| Kibana | http://localhost:5601 | Elasticsearch dashboard |
| PostgreSQL | localhost:5432 | Database connection |

## Environment Variables

### Database
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/docix
DATABASE_USERNAME=docix_user
DATABASE_PASSWORD=docix_password
```

### MinIO
```env
MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=docix-documents
```

### Elasticsearch
```env
ELASTICSEARCH_URL=http://localhost:9200
```

### RabbitMQ
```env
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### Document Processing
```env
DOCIX_PROCESSING_QUEUE_NAME=document.processing
DOCIX_PROCESSING_EXCHANGE_NAME=document.exchange
DOCIX_PROCESSING_ROUTING_KEY=document.process
```

## Health Checks

All services include health checks for reliable startup:

```bash
# Check service health
docker-compose ps

# Check specific service logs
docker-compose logs postgres
docker-compose logs minio
docker-compose logs elasticsearch
docker-compose logs rabbitmq
```

## Data Persistence

Data is persisted in Docker volumes:
- `postgres-data`: Database files
- `minio-data`: Uploaded documents
- `esdata`: Elasticsearch indices
- `rabbitmq-data`: Message queue data

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports are not in use by other services
2. **Memory issues**: Elasticsearch requires sufficient memory
3. **Permissions**: Ensure Docker has permission to create volumes

### Reset Everything
```bash
# Stop and remove all containers and volumes
docker-compose down -v
docker-compose -f docker-compose.dev.yml down -v

# Remove all DocIx-related volumes
docker volume prune
```

### Service-specific Troubleshooting

#### PostgreSQL
```bash
# Connect to database
docker exec -it docix-postgres psql -U docix_user -d docix

# Check database logs
docker-compose logs postgres
```

#### MinIO
```bash
# Access MinIO console at http://localhost:9001
# Default credentials: minioadmin / minioadmin
```

#### Elasticsearch
```bash
# Check cluster health
curl http://localhost:9200/_cluster/health

# Check indices
curl http://localhost:9200/_cat/indices
```

#### RabbitMQ
```bash
# Access management UI at http://localhost:15672
# Default credentials: guest / guest

# Check queues
curl http://guest:guest@localhost:15672/api/queues
```

## Production Considerations

1. **Security**: Change default passwords and enable authentication
2. **Monitoring**: Add Prometheus/Grafana for monitoring
3. **Backup**: Implement backup strategies for all data stores
4. **Scaling**: Consider clustering for high availability
5. **Resource Limits**: Set appropriate memory and CPU limits
