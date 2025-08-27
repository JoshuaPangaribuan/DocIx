package com.example.DocIx.domain.port.in;

import java.io.InputStream;
import java.util.Optional;

public interface DownloadDocumentUseCase {

	Optional<DownloadResult> downloadByDocumentId(String documentId);

	class DownloadResult {
		private final String originalFileName;
		private final String contentType;
		private final InputStream inputStream;

		public DownloadResult(String originalFileName, String contentType, InputStream inputStream) {
			this.originalFileName = originalFileName;
			this.contentType = contentType;
			this.inputStream = inputStream;
		}

		public String getOriginalFileName() { return originalFileName; }
		public String getContentType() { return contentType; }
		public InputStream getInputStream() { return inputStream; }
	}
}


