package com.example.DocIx.domain.mapper;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.in.UploadDocumentUseCase.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface DocumentServiceMapper {

    DocumentServiceMapper INSTANCE = Mappers.getMapper(DocumentServiceMapper.class);

    // Helper method for creating upload result
    default UploadResult toUploadResult(DocumentId documentId, String message) {
        return new UploadResult(documentId, message);
    }

    // Helper method for creating upload result from document
    default UploadResult toUploadResultFromDocument(Document document, String message) {
        return new UploadResult(document.getId(), message);
    }

    // For creating success result
    default UploadResult createSuccessResult(DocumentId documentId) {
        return new UploadResult(documentId, "Document uploaded successfully and queued for processing");
    }
}
