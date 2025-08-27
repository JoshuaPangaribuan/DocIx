package com.example.DocIx.domain.port.in;

import com.example.DocIx.domain.model.IndexingStatus;

public interface DocumentIndexingUseCase {

	void processDocumentIndexing(String documentId);

	void retryFailedIndexing();

	IndexingStatusResponse getIndexingStatus(String documentId);

	class IndexingStatusResponse {
		private final String documentId;
		private final IndexingStatus status;
		private final int totalPages;
		private final int pagesIndexed;
		private final int pagesFailed;
		private final double progress;

		public IndexingStatusResponse(String documentId, IndexingStatus status,
								   int totalPages, int pagesIndexed,
								   int pagesFailed, double progress) {
			this.documentId = documentId;
			this.status = status;
			this.totalPages = totalPages;
			this.pagesIndexed = pagesIndexed;
			this.pagesFailed = pagesFailed;
			this.progress = progress;
		}

		public String getDocumentId() { return documentId; }
		public IndexingStatus getStatus() { return status; }
		public int getTotalPages() { return totalPages; }
		public int getPagesIndexed() { return pagesIndexed; }
		public int getPagesFailed() { return pagesFailed; }
		public double getProgress() { return progress; }
	}
}


