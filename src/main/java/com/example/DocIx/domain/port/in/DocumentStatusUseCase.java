package com.example.DocIx.domain.port.in;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.model.DocumentStatus;

import java.util.List;
import java.util.Optional;

public interface DocumentStatusUseCase {

    Optional<Document> getDocument(DocumentId id);

    List<Document> getDocumentsByStatus(DocumentStatus status);
}

