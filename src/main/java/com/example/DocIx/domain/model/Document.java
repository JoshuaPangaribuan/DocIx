package com.example.DocIx.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Document {
    private final DocumentId id;
    private final String fileName;
    private final String originalFileName;
    private final long fileSize;
    private final String contentType;
    private final String storagePath;
    private final String uploader;
    private final LocalDateTime uploadedAt;
    private String extractedContent;
    private DocumentStatus status;
    private String errorMessage;
    private LocalDateTime lastProcessedAt;

    /**
     * Full constructor used when rehydrating a {@link Document} from persistence.
     * All fields correspond to the columns in the database.
     */
    public Document(DocumentId id,
                    String fileName,
                    String originalFileName,
                    long fileSize,
                    String contentType,
                    String storagePath,
                    String uploader,
                    LocalDateTime uploadedAt,
                    String extractedContent,
                    DocumentStatus status,
                    String errorMessage,
                    LocalDateTime lastProcessedAt) {
        this.id = Objects.requireNonNull(id, "Document ID cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.originalFileName = Objects.requireNonNull(originalFileName, "Original file name cannot be null");
        this.fileSize = fileSize;
        this.contentType = Objects.requireNonNull(contentType, "Content type cannot be null");
        this.storagePath = Objects.requireNonNull(storagePath, "Storage path cannot be null");
        this.uploader = Objects.requireNonNull(uploader, "Uploader cannot be null");
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "Uploaded time cannot be null");
        this.extractedContent = extractedContent;
        this.status = Objects.requireNonNullElse(status, DocumentStatus.UPLOADED);
        this.errorMessage = errorMessage;
        this.lastProcessedAt = lastProcessedAt;
    }

    /**
     * Convenience constructor used when a new document is uploaded.
     * Populates default values for fields that are normally set during processing.
     */
    public Document(DocumentId id, String fileName, String originalFileName,
                   long fileSize, String contentType, String storagePath, String uploader) {
        this(id, fileName, originalFileName, fileSize, contentType, storagePath, uploader,
            LocalDateTime.now(), null, DocumentStatus.UPLOADED, null, null);
    }

    // Business methods
    public void markAsProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.lastProcessedAt = LocalDateTime.now();
    }

    public void markAsProcessed(String extractedContent) {
        this.extractedContent = extractedContent;
        this.status = DocumentStatus.PROCESSED;
        this.lastProcessedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastProcessedAt = LocalDateTime.now();
    }

    public boolean canBeProcessed() {
        return status == DocumentStatus.UPLOADED || status == DocumentStatus.FAILED;
    }

    public boolean isProcessed() {
        return status == DocumentStatus.PROCESSED;
    }

    // Getters
    public DocumentId getId() { return id; }
    public String getFileName() { return fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public long getFileSize() { return fileSize; }
    public String getContentType() { return contentType; }
    public String getStoragePath() { return storagePath; }
    public String getUploader() { return uploader; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public String getExtractedContent() { return extractedContent; }
    public DocumentStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getLastProcessedAt() { return lastProcessedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
