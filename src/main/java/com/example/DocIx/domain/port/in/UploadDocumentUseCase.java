package com.example.DocIx.domain.port.in;

import com.example.DocIx.domain.model.DocumentId;

import java.io.InputStream;

public interface UploadDocumentUseCase {

    UploadResult uploadDocument(UploadCommand command);

    class UploadCommand {
        private final String originalFileName;
        private final InputStream fileContent;
        private final long fileSize;
        private final String contentType;
        private final String uploader;

        public UploadCommand(String originalFileName, InputStream fileContent,
                           long fileSize, String contentType, String uploader) {
            this.originalFileName = originalFileName;
            this.fileContent = fileContent;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.uploader = uploader;
        }

        public String getOriginalFileName() { return originalFileName; }
        public InputStream getFileContent() { return fileContent; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public String getUploader() { return uploader; }
    }

    class UploadResult {
        private final DocumentId documentId;
        private final String message;

        public UploadResult(DocumentId documentId, String message) {
            this.documentId = documentId;
            this.message = message;
        }

        public DocumentId getDocumentId() { return documentId; }
        public String getMessage() { return message; }
    }
}
