package com.example.DocIx.domain.service;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.in.UploadDocumentUseCase;
import com.example.DocIx.domain.port.out.DocumentProcessingPublisher;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentStorage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UploadDocumentService implements UploadDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentProcessingPublisher processingPublisher;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String SUPPORTED_CONTENT_TYPE = "application/pdf";

    public UploadDocumentService(DocumentRepository documentRepository,
                               DocumentStorage documentStorage,
                               DocumentProcessingPublisher processingPublisher) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.processingPublisher = processingPublisher;
    }

    @Override
    public UploadResult uploadDocument(UploadCommand command) {
        validateUploadCommand(command);

        DocumentId documentId = DocumentId.generate();
        String fileName = generateUniqueFileName(command.getOriginalFileName());

        // Store file in object storage
        String storagePath = documentStorage.store(
            fileName,
            command.getFileContent(),
            command.getFileSize(),
            command.getContentType()
        );

        // Create and save document entity
        Document document = new Document(
            documentId,
            fileName,
            command.getOriginalFileName(),
            command.getFileSize(),
            command.getContentType(),
            storagePath,
            command.getUploader()
        );

        documentRepository.save(document);

        // Publish for async processing
        processingPublisher.publishDocumentForProcessing(documentId);

        return new UploadResult(documentId, "Document uploaded successfully and queued for processing");
    }

    private void validateUploadCommand(UploadCommand command) {
        if (command.getFileSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
        }

        if (!SUPPORTED_CONTENT_TYPE.equals(command.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        if (command.getOriginalFileName() == null || command.getOriginalFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        if (command.getUploader() == null || command.getUploader().trim().isEmpty()) {
            throw new IllegalArgumentException("Uploader information is required");
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(originalFileName);
        String baseName = getBaseName(originalFileName);
        return String.format("%s_%s_%s%s", baseName, timestamp, DocumentId.generate().getValue().substring(0, 8), extension);
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    private String getBaseName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        String baseName = lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
        return baseName.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
