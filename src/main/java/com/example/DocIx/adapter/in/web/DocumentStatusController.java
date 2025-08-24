package com.example.DocIx.adapter.in.web;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentStatusController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStatusController.class);
    private final DocumentRepository documentRepository;

    public DocumentStatusController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable String documentId) {
        long startTime = System.currentTimeMillis();

        logger.info("Starting document status check - DocumentId: {}", documentId);

        try {
            Optional<Document> documentOpt = documentRepository.findById(DocumentId.of(documentId));

            if (documentOpt.isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                logger.warn("Document status check failed - DocumentId: {} not found", documentId);

                LoggingUtil.logApiAccess("GET", "/api/documents/" + documentId + "/status", "anonymous",
                                       duration, 404, "Document not found");
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            DocumentStatusResponse response = new DocumentStatusResponse(
                document.getId().getValue(),
                document.getOriginalFileName(),
                document.getStatus().name(),
                document.getUploadedAt().toString(),
                document.getLastProcessedAt() != null ? document.getLastProcessedAt().toString() : null,
                document.getErrorMessage()
            );

            long duration = System.currentTimeMillis() - startTime;
            String safeFileName = LoggingUtil.safeFileName(document.getOriginalFileName());

            logger.info("Document status check completed - DocumentId: {}, Status: {}, File: {}, Duration: {}ms",
                       documentId, document.getStatus().name(), safeFileName, duration);

            LoggingUtil.logApiAccess("GET", "/api/documents/" + documentId + "/status", "anonymous",
                                   duration, 200, "Status: " + document.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Document status check failed - DocumentId: {}, Error: {}",
                        documentId, e.getMessage(), e);

            LoggingUtil.logApiError("GET", "/api/documents/" + documentId + "/status", "anonymous",
                                  duration, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DocumentStatusResponse>> getDocumentsByStatus(@PathVariable String status) {
        long startTime = System.currentTimeMillis();

        logger.info("Starting documents by status query - Status: {}", status);

        try {
            List<Document> documents = documentRepository.findByStatus(
                com.example.DocIx.domain.model.DocumentStatus.valueOf(status.toUpperCase())
            );

            List<DocumentStatusResponse> responses = documents.stream()
                .map(doc -> new DocumentStatusResponse(
                    doc.getId().getValue(),
                    doc.getOriginalFileName(),
                    doc.getStatus().name(),
                    doc.getUploadedAt().toString(),
                    doc.getLastProcessedAt() != null ? doc.getLastProcessedAt().toString() : null,
                    doc.getErrorMessage()
                ))
                .toList();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Documents by status query completed - Status: {}, Results: {}, Duration: {}ms",
                       status, responses.size(), duration);

            LoggingUtil.logApiAccess("GET", "/api/documents/status/" + status, "anonymous",
                                   duration, 200, "Results: " + responses.size());

            return ResponseEntity.ok(responses);

        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("Documents by status query failed - Invalid status: {}, Duration: {}ms",
                       status, duration);

            LoggingUtil.logApiAccess("GET", "/api/documents/status/" + status, "anonymous",
                                   duration, 400, "Invalid status");
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Documents by status query failed - Status: {}, Error: {}",
                        status, e.getMessage(), e);

            LoggingUtil.logApiError("GET", "/api/documents/status/" + status, "anonymous",
                                  duration, e.getMessage());
            throw e;
        }
    }

    public static class DocumentStatusResponse {
        private String documentId;
        private String originalFileName;
        private String status;
        private String uploadedAt;
        private String lastProcessedAt;
        private String errorMessage;

        public DocumentStatusResponse(String documentId, String originalFileName, String status,
                                    String uploadedAt, String lastProcessedAt, String errorMessage) {
            this.documentId = documentId;
            this.originalFileName = originalFileName;
            this.status = status;
            this.uploadedAt = uploadedAt;
            this.lastProcessedAt = lastProcessedAt;
            this.errorMessage = errorMessage;
        }

        public String getDocumentId() { return documentId; }
        public String getOriginalFileName() { return originalFileName; }
        public String getStatus() { return status; }
        public String getUploadedAt() { return uploadedAt; }
        public String getLastProcessedAt() { return lastProcessedAt; }
        public String getErrorMessage() { return errorMessage; }
    }
}
