package com.example.DocIx.adapter.in.web;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.model.DocumentStatus;
import com.example.DocIx.domain.port.in.DocumentStatusUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentStatusController {

    private final DocumentStatusUseCase documentStatusUseCase;

    public DocumentStatusController(DocumentStatusUseCase documentStatusUseCase) {
        this.documentStatusUseCase = documentStatusUseCase;
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable String documentId) {
        Optional<Document> documentOpt = documentStatusUseCase.getDocument(DocumentId.of(documentId));

        if (documentOpt.isEmpty()) {
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

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DocumentStatusResponse>> getDocumentsByStatus(@PathVariable String status) {
        try {
            List<Document> documents = documentStatusUseCase.getDocumentsByStatus(
                DocumentStatus.valueOf(status.toUpperCase())
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

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
