package com.example.DocIx.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.model.DocumentStatus;
import com.example.DocIx.domain.port.in.DocumentStatusQueryUseCase;
import com.example.DocIx.domain.port.out.DocumentRepository;

@Service
public class DocumentStatusQueryService implements DocumentStatusQueryUseCase {

	private final DocumentRepository documentRepository;

	public DocumentStatusQueryService(DocumentRepository documentRepository) {
		this.documentRepository = documentRepository;
	}

	@Override
	public Optional<DocumentStatusResponse> getDocumentStatus(String documentId) {
		return documentRepository.findById(DocumentId.of(documentId))
				.map(doc -> new DocumentStatusResponse(
						doc.getId().getValue(),
						doc.getOriginalFileName(),
						doc.getStatus().name(),
						doc.getUploadedAt().toString(),
						doc.getLastProcessedAt() != null ? doc.getLastProcessedAt().toString() : null,
						doc.getErrorMessage()
				));
	}

	@Override
	public List<DocumentStatusResponse> getDocumentsByStatus(String status) {
		DocumentStatus ds = DocumentStatus.valueOf(status.toUpperCase());
		List<Document> documents = documentRepository.findByStatus(ds);
		return documents.stream()
				.map(doc -> new DocumentStatusResponse(
						doc.getId().getValue(),
						doc.getOriginalFileName(),
						doc.getStatus().name(),
						doc.getUploadedAt().toString(),
						doc.getLastProcessedAt() != null ? doc.getLastProcessedAt().toString() : null,
						doc.getErrorMessage()))
				.collect(Collectors.toList());
	}
}


