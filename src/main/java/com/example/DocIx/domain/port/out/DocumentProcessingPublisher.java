package com.example.DocIx.domain.port.out;

import com.example.DocIx.domain.model.DocumentId;

public interface DocumentProcessingPublisher {
    void publishDocumentForProcessing(DocumentId documentId);
}
