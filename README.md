# DocIx - Document Indexer Search Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Gradle-green.svg)](https://gradle.org/)

## 📖 Overview

DocIx adalah web service pencarian dokumen yang memungkinkan upload, ekstraksi konten, dan pencarian full-text dokumen PDF dengan arsitektur Hexagonal (Ports & Adapters). Aplikasi ini dibangun menggunakan Spring Boot dan mendukung processing asynchronous dengan message queue.

## ✨ Features

- 📄 **Document Upload**: Upload dokumen PDF (single & bulk) maksimal 50MB per file
- 🔍 **Full-Text Search**: Pencarian konten dokumen menggunakan Elasticsearch
- ⚡ **Async Processing**: Pipeline pemrosesan asinkron dengan RabbitMQ
- 💾 **Multi-Storage**: PostgreSQL untuk metadata, MinIO untuk file storage
- 🔧 **Graceful Shutdown**: Shutdown yang aman dengan penyelesaian task aktif
- 📊 **Monitoring**: Health checks dan metrics via Spring Actuator
- 🐳 **Docker Ready**: Containerized dengan multi-stage build
- 🏗️ **Hexagonal Architecture**: Clean architecture dengan ports & adapters

## 🏗️ Tech Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.5.5** - Application framework
- **Gradle 8.5** - Build tool

### Storage & Database
- **PostgreSQL** - Primary database for metadata
- **H2** - In-memory database for development
- **MinIO** - Object storage for PDF files
- **Elasticsearch** - Search engine for full-text indexing

### Message Processing
- **RabbitMQ** - Message broker for async processing
- **Apache Tika** - Document content extraction

### Monitoring & Operations
- **Spring Actuator** - Application monitoring
- **Flyway** - Database migrations
- **MapStruct** - Object mapping

## 🚀 Quick Start

### Prerequisites

- Java 21 atau lebih tinggi
- Docker & Docker Compose
- Gradle 8.x (optional, menggunakan wrapper)

### 1. Clone Repository

```bash
git clone https://github.com/JoshuaPangaribuan/DocIx.git
cd DocIx
```

### 2. Start Infrastructure

```bash
cd infrastructures
docker-compose up -d
```

Ini akan menjalankan:
- PostgreSQL (port 5432)
- MinIO (port 9000, 9001)
- Elasticsearch (port 9200)
- RabbitMQ (port 5672, 15672)

### 3. Configure Application

Edit `src/main/resources/application.properties` atau set environment variables:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/docix
spring.datasource.username=docix
spring.datasource.password=docix

# MinIO
minio.url=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin

# Elasticsearch
spring.elasticsearch.uris=http://localhost:9200

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

### 4. Run Application

```bash
# Development mode
./gradlew bootRun

# Or build and run
./gradlew build
java -jar build/libs/DocIx-0.0.1-SNAPSHOT.jar
```

Aplikasi akan berjalan di `http://localhost:8081`

## 📡 API Endpoints

### Document Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/documents/upload` | Upload single PDF document |
| POST | `/api/documents/bulk-upload` | Upload multiple PDF documents |
| GET | `/api/documents/{id}` | Get document metadata |
| GET | `/api/documents/{id}/download` | Download original document |
| GET | `/api/documents/search` | Search documents with pagination |
| GET | `/api/documents/autocomplete` | Get search suggestions |

### System Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/system/status` | Application status dan active tasks |
| GET | `/api/system/health/ready` | Readiness probe |
| GET | `/api/system/health/live` | Liveness probe |
| POST | `/actuator/shutdown` | Trigger graceful shutdown |

### Example Usage

**Upload Document:**
```bash
curl -X POST "http://localhost:8081/api/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@document.pdf" \
  -F "uploader=John Doe"
```

**Search Documents:**
```bash
curl "http://localhost:8081/api/documents/search?query=spring+boot&page=0&size=10"
```

Lihat [API_DOCUMENTATION.md](API_DOCUMENTATION.md) untuk dokumentasi lengkap.

## 🛠️ Development

### Project Structure

```
src/
├── main/java/com/example/DocIx/
│   ├── adapter/           # Adapters (Controllers, Repositories)
│   │   ├── in/           # Input adapters (REST, Messaging)
│   │   └── out/          # Output adapters (Database, Storage)
│   ├── config/           # Configuration classes
│   ├── domain/           # Business logic & entities
│   └── application/      # Use cases & services
├── main/resources/
│   ├── application.properties
│   └── db/migration/     # Flyway migration scripts
└── test/                 # Test classes
```

### Build Commands

```bash
# Compile
./gradlew compileJava

# Run tests
./gradlew test

# Build JAR
./gradlew build

# Clean build
./gradlew clean build

# Run with profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Testing

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Test graceful shutdown
./test-graceful-shutdown.bat  # Windows
./test-graceful-shutdown.sh   # Linux/Mac
```

## 🐳 Docker Deployment

### Build Image

```bash
docker build -t docix:latest .
```

### Run Container

```bash
docker run -d \
  --name docix \
  -p 8081:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/docix \
  -e MINIO_URL=http://host.docker.internal:9000 \
  -e ELASTICSEARCH_URL=http://host.docker.internal:9200 \
  -e RABBITMQ_HOST=host.docker.internal \
  docix:latest
```

### Docker Compose

```bash
# Start all services including application
docker-compose -f infrastructures/docker-compose.yml up -d
```

## ☸️ Kubernetes Deployment

Example deployment manifest:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: docix
spec:
  replicas: 3
  selector:
    matchLabels:
      app: docix
  template:
    metadata:
      labels:
        app: docix
    spec:
      containers:
      - name: docix
        image: docix:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres:5432/docix"
        livenessProbe:
          httpGet:
            path: /api/system/health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /api/system/health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:h2:mem:testdb` |
| `DATABASE_USERNAME` | Database username | `sa` |
| `DATABASE_PASSWORD` | Database password | `` |
| `MINIO_URL` | MinIO server URL | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | `minioadmin` |
| `ELASTICSEARCH_URL` | Elasticsearch URL | `http://localhost:9200` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |

### Application Profiles

- **default**: H2 database, local services
- **dev**: PostgreSQL, containerized services
- **prod**: Production configuration with external services

## 🛡️ Graceful Shutdown

DocIx mendukung graceful shutdown yang memastikan:

- ✅ Task processing selesai sebelum shutdown
- ✅ Database connections ditutup dengan proper
- ✅ Message queue acknowledgments diproses
- ✅ File operations diselesaikan

Konfigurasi utama:
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
management.endpoint.shutdown.enabled=true
```

Lihat [GRACEFUL_SHUTDOWN.md](GRACEFUL_SHUTDOWN.md) untuk detail lengkap.

## 📊 Monitoring

### Health Checks

- **Liveness**: `/api/system/health/live` - Aplikasi berjalan
- **Readiness**: `/api/system/health/ready` - Siap menerima traffic
- **Detailed Health**: `/actuator/health` - Status semua components

### Metrics

- Application metrics: `/actuator/metrics`
- System status: `/api/system/status`
- Active tasks monitoring
- Database connection pool status

### Logging

Logs tersedia dalam format structured dengan level:
- `ERROR`: Critical errors
- `WARN`: Warnings dan recoverable errors  
- `INFO`: General application flow
- `DEBUG`: Detailed debugging information

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

### Development Guidelines

- Follow Java coding standards
- Write unit tests untuk new features
- Update documentation
- Use conventional commit messages
- Ensure all tests pass before PR

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team untuk excellent framework
- Apache Tika untuk document processing capabilities
- Elasticsearch untuk powerful search engine
- MinIO untuk S3-compatible storage solution

## 📞 Support

Jika ada pertanyaan atau issues:

1. Check [Issues](https://github.com/JoshuaPangaribuan/DocIx/issues) for existing problems
2. Create new issue dengan detailed description
3. Include relevant logs dan configuration
4. Tag dengan appropriate labels

---

**DocIx** - Making document search simple and powerful! 🚀
