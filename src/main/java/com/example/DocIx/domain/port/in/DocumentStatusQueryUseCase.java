package com.example.DocIx.domain.port.in;

import java.util.List;
import java.util.Optional;

public interface DocumentStatusQueryUseCase {

	Optional<DocumentStatusResponse> getDocumentStatus(String documentId);

	List<DocumentStatusResponse> getDocumentsByStatus(String status);

	class DocumentStatusResponse {
		private final String documentId;
		private final String originalFileName;
		private final String status;
		private final String uploadedAt;
		private final String lastProcessedAt;
		private final String errorMessage;

		public DocumentStatusResponse(String documentId, String originalFileName, String status,
								   String uploadedAt, String lastProcessedAt, String errorMessage) {
			this.documentId = documentId;
			this.originalFileName = originalFileName;
			this.status = status;
			this.uploadedAt = uploadedAt;
			this.lastProcessedAt = lastProcessedAt;
			this.errorMessage = errorMessage;
		}

		public String getDocumentId() { return documentId; }
		public String getOriginalFileName() { return originalFileName; }
		public String getStatus() { return status; }
		public String getUploadedAt() { return uploadedAt; }
		public String getLastProcessedAt() { return lastProcessedAt; }
		public String getErrorMessage() { return errorMessage; }
	}
}


