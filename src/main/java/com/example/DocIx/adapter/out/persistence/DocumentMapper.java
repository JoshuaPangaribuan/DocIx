package com.example.DocIx.adapter.out.persistence;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentJpaEntity toJpaEntity(Document document) {
        return new DocumentJpaEntity(
                document.getId().getValue(),
                document.getFileName(),
                document.getOriginalFileName(),
                document.getFileSize(),
                document.getContentType(),
                document.getStoragePath(),
                document.getUploader(),
                document.getUploadedAt(),
                document.getDownloadUrl(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getLastProcessedAt());
    }

    public Document toDomainEntity(DocumentJpaEntity jpaEntity) {
        return createDocumentWithAllFields(jpaEntity);
    }

    private Document createDocumentWithAllFields(DocumentJpaEntity jpaEntity) {
        // Create document with basic constructor
        Document document = new Document(
                DocumentId.of(jpaEntity.getId()),
                jpaEntity.getFileName(),
                jpaEntity.getOriginalFileName(),
                jpaEntity.getFileSize(),
                jpaEntity.getContentType(),
                jpaEntity.getStoragePath(),
                jpaEntity.getUploader());

        // Apply status and download URL based on JPA entity state
        switch (jpaEntity.getStatus()) {
            case PROCESSING -> document.markAsProcessing();
            case PROCESSED -> {
                if (jpaEntity.getDownloadUrl() != null) {
                    document.markAsProcessed(jpaEntity.getDownloadUrl());
                }
            }
            case FAILED -> {
                if (jpaEntity.getErrorMessage() != null) {
                    document.markAsFailed(jpaEntity.getErrorMessage());
                }
            }
            // UPLOADED is the default state
            default -> throw new IllegalArgumentException("Unexpected value: " + jpaEntity.getStatus());
        }

        return document;
    }
}
