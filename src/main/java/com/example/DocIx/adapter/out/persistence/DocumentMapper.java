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
            document.getExtractedContent(),
            document.getStatus(),
            document.getErrorMessage(),
            document.getLastProcessedAt()
        );
    }

    public Document toDomainEntity(DocumentJpaEntity jpaEntity) {
        Document document = new Document(
            DocumentId.of(jpaEntity.getId()),
            jpaEntity.getFileName(),
            jpaEntity.getOriginalFileName(),
            jpaEntity.getFileSize(),
            jpaEntity.getContentType(),
            jpaEntity.getStoragePath(),
            jpaEntity.getUploader()
        );

        // Set additional fields using reflection-like approach
        // In a real implementation, you might want to use a proper mapping library
        // or add a constructor/method to handle this
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
            jpaEntity.getUploader()
        );

        // Apply status and content based on JPA entity state
        switch (jpaEntity.getStatus()) {
            case PROCESSING -> document.markAsProcessing();
            case PROCESSED -> {
                if (jpaEntity.getExtractedContent() != null) {
                    document.markAsProcessed(jpaEntity.getExtractedContent());
                }
            }
            case FAILED -> {
                if (jpaEntity.getErrorMessage() != null) {
                    document.markAsFailed(jpaEntity.getErrorMessage());
                }
            }
            // UPLOADED is the default state
        }

        return document;
    }
}
