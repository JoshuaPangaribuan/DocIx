package com.example.DocIx.domain.service;

import java.io.InputStream;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.in.DownloadDocumentUseCase;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentStorage;

@Service
public class DownloadDocumentService implements DownloadDocumentUseCase {

	private final DocumentRepository documentRepository;
	private final DocumentStorage documentStorage;

	public DownloadDocumentService(DocumentRepository documentRepository, DocumentStorage documentStorage) {
		this.documentRepository = documentRepository;
		this.documentStorage = documentStorage;
	}

	@Override
	public Optional<DownloadResult> downloadByDocumentId(String documentId) {
		Optional<Document> documentOptional = documentRepository.findById(DocumentId.of(documentId));
		if (documentOptional.isEmpty()) {
			return Optional.empty();
		}

		Document document = documentOptional.get();
		if (!document.isProcessed()) {
			return Optional.empty();
		}

		InputStream inputStream = documentStorage.retrieve(document.getStoragePath());
		return Optional.of(new DownloadResult(document.getOriginalFileName(), document.getContentType(), inputStream));
	}
}


