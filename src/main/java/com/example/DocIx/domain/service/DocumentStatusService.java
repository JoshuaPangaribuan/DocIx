package com.example.DocIx.domain.service;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.model.DocumentStatus;
import com.example.DocIx.domain.port.in.DocumentStatusUseCase;
import com.example.DocIx.domain.port.out.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentStatusService implements DocumentStatusUseCase {

    private final DocumentRepository documentRepository;

    public DocumentStatusService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Optional<Document> getDocument(DocumentId id) {
        return documentRepository.findById(id);
    }

    @Override
    public List<Document> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }
}

