package com.example.DocIx.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.IndexingStatus;
import com.example.DocIx.domain.port.in.AdminIndexingUseCase;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import com.example.DocIx.domain.port.out.IndexingLogRepository;

@Service
public class AdminIndexingService implements AdminIndexingUseCase {

	private final DocumentIndexingService documentIndexingService;
	private final IndexingLogRepository indexingLogRepository;
	private final DocumentRepository documentRepository;
	private final DocumentSearchEngine searchEngine;

	public AdminIndexingService(DocumentIndexingService documentIndexingService,
							  IndexingLogRepository indexingLogRepository,
							  DocumentRepository documentRepository,
							  DocumentSearchEngine searchEngine) {
		this.documentIndexingService = documentIndexingService;
		this.indexingLogRepository = indexingLogRepository;
		this.documentRepository = documentRepository;
		this.searchEngine = searchEngine;
	}

	@Override
	public IndexingSummaryResponse getIndexingSummary() {
		long pendingCount = indexingLogRepository.findByStatus(IndexingStatus.PENDING).size();
		long inProgressCount = indexingLogRepository.findByStatus(IndexingStatus.IN_PROGRESS).size();
		long fullyIndexedCount = indexingLogRepository.findByStatus(IndexingStatus.FULLY_INDEXED).size();
		long partiallyIndexedCount = indexingLogRepository.findByStatus(IndexingStatus.PARTIALLY_INDEXED).size();
		long failedCount = indexingLogRepository.findByStatus(IndexingStatus.FAILED).size();
		return new IndexingSummaryResponse(pendingCount, inProgressCount, fullyIndexedCount, partiallyIndexedCount, failedCount);
	}

	@Override
	public IndexingConsistencyResponse checkIndexingConsistency() {
		List<Document> processedDocuments = documentRepository.findByProcessed(true);
		int indexedDocuments = 0;
		int missingDocuments = 0;
		List<String> missingDocumentIds = new ArrayList<>();
		for (Document doc : processedDocuments) {
			try {
				boolean exists = searchEngine.search(doc.getId().getValue(), 0, 1).stream()
						.anyMatch(r -> r.getDocumentId().getValue().equals(doc.getId().getValue()));
				if (exists) {
					indexedDocuments++;
				} else {
					missingDocuments++;
					missingDocumentIds.add(doc.getId().getValue());
				}
			} catch (Exception e) {
				missingDocuments++;
				missingDocumentIds.add(doc.getId().getValue());
			}
		}
		double consistencyPercentage = processedDocuments.size() > 0 ? (double) indexedDocuments / processedDocuments.size() * 100 : 100.0;
		return new IndexingConsistencyResponse(processedDocuments.size(), indexedDocuments, missingDocuments, consistencyPercentage, missingDocumentIds);
	}

	@Override
	public void retryFailedIndexing() {
		this.documentIndexingService.retryFailedIndexing();
	}

	@Override
	public int reindexMissingDocuments() {
		List<Document> processedDocuments = documentRepository.findByProcessed(true);
		int reindexed = 0;
		for (Document doc : processedDocuments) {
			boolean exists = searchEngine.search(doc.getId().getValue(), 0, 1).stream()
					.anyMatch(r -> r.getDocumentId().getValue().equals(doc.getId().getValue()));
			if (!exists) {
				documentIndexingService.processDocumentIndexing(doc.getId().getValue());
				reindexed++;
			}
		}
		return reindexed;
	}

	@Override
	public void reindexAllDocuments() {
		List<Document> processedDocuments = documentRepository.findByProcessed(true);
		for (Document doc : processedDocuments) {
			documentIndexingService.processDocumentIndexing(doc.getId().getValue());
		}
	}
}


