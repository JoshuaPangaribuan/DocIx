package com.example.DocIx.domain.port.in;

import java.util.List;

public interface AdminIndexingUseCase {

	IndexingSummaryResponse getIndexingSummary();

	IndexingConsistencyResponse checkIndexingConsistency();

	void retryFailedIndexing();

	int reindexMissingDocuments();

	void reindexAllDocuments();

	class IndexingSummaryResponse {
		private final long pendingCount;
		private final long inProgressCount;
		private final long fullyIndexedCount;
		private final long partiallyIndexedCount;
		private final long failedCount;
		private final long totalCount;

		public IndexingSummaryResponse(long pendingCount, long inProgressCount,
								   long fullyIndexedCount, long partiallyIndexedCount,
								   long failedCount) {
			this.pendingCount = pendingCount;
			this.inProgressCount = inProgressCount;
			this.fullyIndexedCount = fullyIndexedCount;
			this.partiallyIndexedCount = partiallyIndexedCount;
			this.failedCount = failedCount;
			this.totalCount = pendingCount + inProgressCount + fullyIndexedCount + partiallyIndexedCount + failedCount;
		}

		public long getPendingCount() { return pendingCount; }
		public long getInProgressCount() { return inProgressCount; }
		public long getFullyIndexedCount() { return fullyIndexedCount; }
		public long getPartiallyIndexedCount() { return partiallyIndexedCount; }
		public long getFailedCount() { return failedCount; }
		public long getTotalCount() { return totalCount; }
		public double getSuccessRate() { return totalCount == 0 ? 0.0 : (double) fullyIndexedCount / totalCount * 100; }
	}

	class IndexingConsistencyResponse {
		private final int totalProcessedDocuments;
		private final int indexedDocuments;
		private final int missingDocuments;
		private final double consistencyPercentage;
		private final List<String> missingDocumentIds;

		public IndexingConsistencyResponse(int totalProcessedDocuments, int indexedDocuments, int missingDocuments,
									   double consistencyPercentage, List<String> missingDocumentIds) {
			this.totalProcessedDocuments = totalProcessedDocuments;
			this.indexedDocuments = indexedDocuments;
			this.missingDocuments = missingDocuments;
			this.consistencyPercentage = consistencyPercentage;
			this.missingDocumentIds = missingDocumentIds;
		}

		public int getTotalProcessedDocuments() { return totalProcessedDocuments; }
		public int getIndexedDocuments() { return indexedDocuments; }
		public int getMissingDocuments() { return missingDocuments; }
		public double getConsistencyPercentage() { return consistencyPercentage; }
		public List<String> getMissingDocumentIds() { return missingDocumentIds; }
	}
}


