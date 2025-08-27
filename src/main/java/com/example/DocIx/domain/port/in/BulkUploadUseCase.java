package com.example.DocIx.domain.port.in;

import java.util.List;

/**
 * Use case untuk operasi bulk/single upload dokumen secara atomik.
 */
public interface BulkUploadUseCase {

	BulkUploadResult uploadDocument(BulkUploadCommand command);

	List<BulkUploadResult> uploadMultipleDocuments(List<BulkUploadCommand> commands);

	class BulkUploadCommand {
		private final String originalFileName;
		private final byte[] fileContent;
		private final long fileSize;
		private final String contentType;
		private final String uploader;

		public BulkUploadCommand(String originalFileName, byte[] fileContent,
							 long fileSize, String contentType, String uploader) {
			this.originalFileName = originalFileName;
			this.fileContent = fileContent;
			this.fileSize = fileSize;
			this.contentType = contentType;
			this.uploader = uploader;
		}

		public String getOriginalFileName() { return originalFileName; }
		public byte[] getFileContent() { return fileContent; }
		public long getFileSize() { return fileSize; }
		public String getContentType() { return contentType; }
		public String getUploader() { return uploader; }
	}

	class BulkUploadResult {
		private final boolean success;
		private final String documentId;
		private final String message;
		private final String errorMessage;

		public BulkUploadResult(boolean success, String documentId, String message, String errorMessage) {
			this.success = success;
			this.documentId = documentId;
			this.message = message;
			this.errorMessage = errorMessage;
		}

		public boolean isSuccess() { return success; }
		public String getDocumentId() { return documentId; }
		public String getMessage() { return message; }
		public String getErrorMessage() { return errorMessage; }
	}
}


