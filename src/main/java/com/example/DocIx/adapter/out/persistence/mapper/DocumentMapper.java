package com.example.DocIx.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.example.DocIx.adapter.out.persistence.entity.DocumentJpaEntity;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "id", target = "id", qualifiedByName = "documentIdToString")
    DocumentJpaEntity toJpaEntity(Document document);

    @Named("documentIdToString")
    default String documentIdToString(DocumentId documentId) {
        return documentId != null ? documentId.getValue() : null;
    }

    @Mapping(source = "id", target = "id", qualifiedByName = "stringToDocumentId")
    @Mapping(target = ".", source = ".", qualifiedByName = "mapDocumentWithStatus")
    Document toDomainEntity(DocumentJpaEntity jpaEntity);

    @Named("stringToDocumentId")
    default DocumentId stringToDocumentId(String id) {
        return id != null ? DocumentId.of(id) : null;
    }

    @Named("mapDocumentWithStatus")
    default Document mapDocumentWithStatus(DocumentJpaEntity jpaEntity) {
        if (jpaEntity == null)
            return null;

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
            case UPLOADED -> {
                // UPLOADED is the default state, no additional operations needed
            }
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
            default -> throw new IllegalArgumentException("Unexpected value: " + jpaEntity.getStatus());
        }

        return document;
    }
}