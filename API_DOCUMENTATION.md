# DocIx - Document Search Service API Documentation

## Overview
DocIx adalah web service pencarian dokumen yang memungkinkan upload, ekstraksi konten, dan pencarian full-text dokumen PDF dengan arsitektur Hexagonal (Ports & Adapters).

## Features
- Upload dokumen PDF (single & bulk) maksimal 50MB per file
- Ekstraksi konten otomatis menggunakan Apache Tika
- Indexing ke Elasticsearch untuk full-text search
- Autocomplete dengan maksimal 5 rekomendasi
- Async processing pipeline dengan RabbitMQ
- Storage menggunakan MinIO/S3
- Metadata disimpan di PostgreSQL

## API Endpoints

### 1. Upload Document
**POST** `/api/documents/upload`

Upload single PDF document.

**Parameters:**
- `file` (multipart/form-data): PDF file (max 50MB)
- `uploader` (form field): Uploader name (1-100 characters)

**Response:**
```json
{
  "documentId": "uuid-string",
  "message": "Document uploaded successfully and queued for processing"
}
```

### 2. Bulk Upload Documents
**POST** `/api/documents/upload/bulk`

Upload multiple PDF documents.

**Parameters:**
- `files` (multipart/form-data): Array of PDF files
- `uploader` (form field): Uploader name

**Response:**
```json
{
  "successful": [
    {
      "fileName": "document1.pdf",
      "documentId": "uuid-string",
      "message": "Document uploaded successfully"
    }
  ],
  "failed": [
    {
      "fileName": "document2.pdf",
      "error": "File size exceeds maximum limit"
    }
  ],
  "totalSuccessful": 1,
  "totalFailed": 1
}
```

### 3. Search Documents
**GET** `/api/documents/search`

Full-text search dalam dokumen.

**Parameters:**
- `q` (required): Search query (2-200 characters)
- `page` (optional, default=0): Page number
- `size` (optional, default=10): Results per page

**Response:**
```json
{
  "results": [
    {
      "documentId": "uuid-string",
      "fileName": "document.pdf",
      "highlightedContent": "...highlighted text...",
      "score": 0.95
    }
  ],
  "totalHits": 25,
  "page": 0,
  "size": 10,
  "hasNext": true,
  "hasPrevious": false
}
```

### 4. Autocomplete
**GET** `/api/documents/autocomplete`

Mendapatkan saran autocomplete.

**Parameters:**
- `q` (required): Query string (1-100 characters)
- `max` (optional, default=5): Maximum suggestions

**Response:**
```json
{
  "suggestions": [
    "machine learning",
    "machine intelligence",
    "machine vision"
  ]
}
```

### 5. Document Status
**GET** `/api/documents/{documentId}/status`

Cek status processing dokumen.

**Response:**
```json
{
  "documentId": "uuid-string",
  "originalFileName": "document.pdf",
  "status": "PROCESSED",
  "uploadedAt": "2025-08-23T10:30:00",
  "lastProcessedAt": "2025-08-23T10:31:00",
  "errorMessage": null
}
```

### 6. Documents by Status
**GET** `/api/documents/status/{status}`

Dapatkan dokumen berdasarkan status (UPLOADED, PROCESSING, PROCESSED, FAILED).

## Error Handling

Semua error response menggunakan format JSON konsisten:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "timestamp": "2025-08-23T10:30:00",
  "details": {
    "field": "error detail"
  }
}
```

### Error Codes:
- `VALIDATION_ERROR`: Input validation failed
- `FILE_SIZE_EXCEEDED`: File exceeds 50MB limit
- `INTERNAL_ERROR`: Internal server error
- `UNEXPECTED_ERROR`: Unexpected error occurred

## Document Processing Pipeline

1. **Upload**: File disimpan di MinIO, metadata ke database
2. **Queue**: Document ID dikirim ke RabbitMQ untuk async processing
3. **Extract**: Apache Tika mengekstrak teks dari PDF
4. **Index**: Konten diindex ke Elasticsearch
5. **Complete**: Status updated ke PROCESSED

## Configuration

### Environment Variables:
- `DATABASE_URL`: PostgreSQL connection string
- `ELASTICSEARCH_URL`: Elasticsearch endpoint
- `MINIO_URL`: MinIO endpoint
- `RABBITMQ_HOST`: RabbitMQ host
- `MINIO_ACCESS_KEY`: MinIO access key
- `MINIO_SECRET_KEY`: MinIO secret key

### Application Properties:
```properties
# File upload limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Processing queue configuration
docix.processing.queue.name=document.processing
docix.processing.exchange.name=document.exchange
docix.processing.routing.key=document.process
```

## Architecture

### Hexagonal Architecture Layers:
1. **Domain Layer**: Business logic (models, ports, services)
2. **Application Layer**: Use cases and domain services
3. **Infrastructure Layer**: Adapters (database, storage, search, messaging)
4. **API Layer**: REST controllers and DTOs

### Key Components:
- **DocumentRepository**: Database operations
- **DocumentStorage**: MinIO/S3 file storage
- **DocumentSearchEngine**: Elasticsearch operations
- **ContentExtractor**: Apache Tika text extraction
- **DocumentProcessingPublisher**: RabbitMQ messaging

## Monitoring & Observability

- **Logging**: Structured logging dengan level DEBUG untuk development
- **Metrics**: Spring Boot Actuator endpoints
- **Health Checks**: `/actuator/health`
- **Processing Status**: Real-time status tracking per document

## Development Setup

1. Start infrastructure:
   ```bash
   docker-compose up -d  # Elasticsearch, RabbitMQ, MinIO
   ```

2. Run application:
   ```bash
   ./gradlew bootRun
   ```

3. Access services:
   - API: http://localhost:8080
   - MinIO Console: http://localhost:9001
