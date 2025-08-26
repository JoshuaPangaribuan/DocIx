# DocIx - Document Indexer Search Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Gradle-green.svg)](https://gradle.org/)

## 📖 Overview

DocIx adalah web service pencarian dokumen yang memungkinkan upload, ekstraksi konten, dan pencarian full-text dokumen PDF dengan arsitektur Hexagonal (Ports & Adapters). Aplikasi ini dibangun menggunakan Spring Boot dan mendukung processing asynchronous dengan message queue serta **Page based indexing** untuk dokumen berukuran besar.

## ✨ Features

- 📄 **Document Upload**: Upload dokumen PDF (single & bulk) maksimal 100MB per file
- 🔍 **Full-Text Search**: Pencarian konten dokumen menggunakan Elasticsearch dengan dukungan pencarian berbasis halaman
- ⚡ **Async Processing**: Pipeline pemrosesan asinkron dengan RabbitMQ
- 💾 **Multi-Storage**: PostgreSQL untuk metadata, MinIO untuk file storage
- 🔧 **Graceful Shutdown**: Shutdown yang aman dengan penyelesaian task aktif
- 📊 **Monitoring**: Health checks dan metrics via Spring Actuator
- 🐳 **Docker Ready**: Containerized dengan multi-stage build
- 🏗️ **Hexagonal Architecture**: Clean architecture dengan ports & adapters
- 🔄 **Type-Safe Mapping**: Object mapping dengan MapStruct untuk performance dan type safety
- 🎯 **Page-based Processing**: Indexing dokumen PDF per halaman untuk pencarian yang lebih presisi

## 🏗️ Tech Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.5.5** - Application framework
- **Gradle 8.14.3** - Build tool

### Storage & Database
- **PostgreSQL** - Primary database for metadata
- **MinIO** - Object storage for PDF files
- **Elasticsearch** - Search engine for full-text indexing with page-based content support

### Message Processing
- **RabbitMQ** - Message broker for async processing
- **Apache Tika** - Document content extraction

### Monitoring & Operations
- **Spring Actuator** - Application monitoring
- **Flyway** - Database migrations
- **MapStruct** - Object mapping
- **Micrometer + Prometheus** - Metrics registry dan endpoint `/actuator/prometheus`
- **OpenTelemetry (Jaeger Exporter)** - Distributed tracing

## 📄 Page-based Document Processing

DocIx mengimplementasikan sistem pemrosesan dokumen berbasis halaman yang memungkinkan penanganan dokumen PDF dengan indexing per halaman untuk pencarian yang lebih presisi.

### How It Works

1. **Content Extraction**: Ekstraksi teks dari PDF menggunakan Apache PDFBox
2. **Page-based Processing**: Setiap halaman PDF diekstrak dan diindeks secara terpisah
3. **Precise Indexing**: Setiap halaman diindeks sebagai dokumen terpisah di Elasticsearch
4. **Enhanced Search**: Pencarian dapat menunjukkan halaman spesifik yang mengandung hasil pencarian

### Benefits

- ✅ **Better Precision**: Hasil pencarian menunjukkan halaman spesifik yang relevan
- ✅ **Improved Navigation**: User dapat langsung menuju halaman yang mengandung informasi
- ✅ **Memory Efficient**: Setiap halaman diproses secara independen
- ✅ **Scalable**: Cocok untuk dokumen dengan berbagai ukuran
- ✅ **Structured Results**: Hasil pencarian terstruktur berdasarkan halaman
- ✅ **Fast Processing**: Pemrosesan paralel per halaman

### Page Processing Features

- **Individual Page Extraction**: Setiap halaman diekstrak secara terpisah
- **Page Number Tracking**: Setiap halaman menyimpan nomor halaman asli
- **Document Metadata**: Informasi dokumen tetap terhubung dengan setiap halaman
- **Flexible Search**: Pencarian dapat dilakukan di level dokumen atau halaman

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
make dev-up
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
spring.datasource.username=docix_user
spring.datasource.password=docix_password

# MinIO
minio.url=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket-name=docix-documents

# Elasticsearch
spring.elasticsearch.uris=http://localhost:9200

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### 4. Run Application

```bash
# Development mode
./gradlew bootRun

# Or build and run
./gradlew build
java -jar build/libs/DocIx-0.0.1-SNAPSHOT.jar
```

Aplikasi akan berjalan di `http://localhost:8080`

Catatan: Jika menjalankan via Docker Compose (infrastructures/docker-compose.yml), akses aplikasi melalui `http://localhost:8081` (mapping port host 8081 ke container 8080).

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
curl -X POST "http://localhost:8080/api/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@document.pdf" \
  -F "uploader=John Doe"
```

**Search Documents:**
```bash
curl "http://localhost:8080/api/documents/search?query=spring+boot&page=0&size=10"
```

Lihat [API_DOCUMENTATION.md](API_DOCUMENTATION.md) untuk dokumentasi lengkap.

## 🔄 MapStruct Integration

DocIx menggunakan MapStruct untuk object mapping yang type-safe dan performant di seluruh layer aplikasi.

### Architecture & Mappers

#### Web Layer Mapping
```java
@Mapper(componentModel = "spring")
public interface DocumentWebMapper {
    
    // Upload response mapping
    @Mapping(target = "documentId", source = "documentId.value")
    UploadResponse toUploadResponse(UploadDocumentUseCase.UploadResult result);
    
    // Search response mapping dengan custom logic
    default SearchResponse toSearchResponse(SearchDocumentUseCase.SearchResponse searchResponse) {
        return new SearchResponse(
            toSearchResultDtoList(searchResponse.getResults()),
            searchResponse.getTotalHits(),
            searchResponse.getPage(),
            searchResponse.getSize(),
            searchResponse.hasNext(),
            searchResponse.hasPrevious()
        );
    }
    
    // Bulk upload helper methods
    default BulkUploadResponse.UploadResult toUploadResult(String fileName, String documentId, String message) {
        return new BulkUploadResponse.UploadResult(fileName, documentId, message);
    }
}
```

#### Service Layer Mapping
```java
@Mapper(componentModel = "spring")
public interface DocumentServiceMapper {
    
    // Helper methods untuk use case results
    default UploadResult createSuccessResult(DocumentId documentId) {
        return new UploadResult(documentId, "Document uploaded successfully and queued for processing");
    }
    
    default UploadResult toUploadResultFromDocument(Document document, String message) {
        return new UploadResult(document.getId(), message);
    }
}
```

### Key Benefits

- **🚀 Performance**: Zero reflection, generated plain Java code
- **🔒 Type Safety**: Compile-time validation dan error detection
- **🧹 Clean Code**: Centralized mapping logic, reduced boilerplate
- **🔧 Maintainable**: Easy to modify dan extend mappings
- **🏗️ Spring Integration**: Auto-injection sebagai Spring beans

### Usage Examples

#### Controller Layer
```java
@RestController
public class DocumentController {
    
    private final DocumentWebMapper documentWebMapper;
    
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        UploadDocumentUseCase.UploadResult result = uploadDocumentUseCase.uploadDocument(command);
        return ResponseEntity.ok(documentWebMapper.toUploadResponse(result));
    }
    
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchDocuments(@RequestParam("q") String query) {
        SearchDocumentUseCase.SearchResponse result = searchDocumentUseCase.searchDocuments(searchQuery);
        return ResponseEntity.ok(documentWebMapper.toSearchResponse(result));
    }
}
```

#### Service Layer
```java
@Service
public class UploadDocumentService implements UploadDocumentUseCase {
    
    private final DocumentServiceMapper documentServiceMapper;
    
    @Override
    public UploadResult uploadDocument(UploadCommand command) {
        // ...business logic...
        documentRepository.save(document);
        processingPublisher.publishDocumentForProcessing(documentId);
        
        return documentServiceMapper.createSuccessResult(documentId);
    }
}
```

### Build Configuration

MapStruct dikonfigurasi di `build.gradle`:

```gradle
dependencies {
    // MapStruct for object mapping
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    testAnnotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
}
```

### Generated Code Location

MapStruct annotation processor menghasilkan implementation classes di:
- **Development**: `build/generated/sources/annotationProcessor/java/main/`
- **IDE Integration**: Auto-detected dan indexed untuk code completion

### Best Practices

- ✅ Gunakan `componentModel = "spring"` untuk dependency injection
- ✅ Combine `@Mapping` annotations dengan `default` methods untuk complex mapping
- ✅ Leverage MapStruct untuk list/collection mappings
- ✅ Use custom mapping methods untuk non-standard conversions
- ✅ Test mapper behavior dengan unit tests

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
  -e DATABASE_USERNAME=docix_user \
  -e DATABASE_PASSWORD=docix_password \
  -e MINIO_URL=http://host.docker.internal:9000 \
  -e MINIO_ACCESS_KEY=minioadmin \
  -e MINIO_SECRET_KEY=minioadmin \
  -e MINIO_BUCKET_NAME=docix-documents \
  -e ELASTICSEARCH_URL=http://host.docker.internal:9200 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=guest \
  -e RABBITMQ_PASSWORD=guest \
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
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/docix` |
| `DATABASE_USERNAME` | Database username | `docix_user` |
| `DATABASE_PASSWORD` | Database password | `docix_password` |
| `MINIO_URL` | MinIO server URL | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | `minioadmin` |
| `MINIO_BUCKET_NAME` | MinIO bucket for documents | `docix-documents` |
| `ELASTICSEARCH_URL` | Elasticsearch URL | `http://localhost:9200` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |

### Application Profiles

- **default**: PostgreSQL database, local services
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
- Prometheus scrape endpoint: `/actuator/prometheus`
- System status: `/api/system/status`
- Active tasks monitoring
- Database connection pool status

### Tracing
- OpenTelemetry tracing diaktifkan, ekspor ke Jaeger (konfigurasi melalui starter OpenTelemetry)
- Gunakan Jaeger UI untuk visualisasi trace

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
