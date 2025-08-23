package com.example.DocIx.domain.port.out;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.model.DocumentStatus;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(DocumentId id);
    List<Document> findByStatus(DocumentStatus status);
    List<Document> findByUploader(String uploader);
    void deleteById(DocumentId id);
    boolean existsById(DocumentId id);
}
