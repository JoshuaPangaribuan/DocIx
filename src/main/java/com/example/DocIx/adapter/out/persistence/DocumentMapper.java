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
        return new Document(
            DocumentId.of(jpaEntity.getId()),
            jpaEntity.getFileName(),
            jpaEntity.getOriginalFileName(),
            jpaEntity.getFileSize(),
            jpaEntity.getContentType(),
            jpaEntity.getStoragePath(),
            jpaEntity.getUploader(),
            jpaEntity.getUploadedAt(),
            jpaEntity.getExtractedContent(),
            jpaEntity.getStatus(),
            jpaEntity.getErrorMessage(),
            jpaEntity.getLastProcessedAt()
        );
    }
}
