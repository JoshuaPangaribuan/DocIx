package com.example.DocIx.domain.service;

import com.example.DocIx.domain.event.DocumentUploadEventListener;
import com.example.DocIx.domain.port.in.BulkUploadUseCase;
import com.example.DocIx.domain.model.*;
import com.example.DocIx.domain.port.out.*;
import com.example.DocIx.domain.util.FileNameEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BulkUploadService implements BulkUploadUseCase {

    private static final Logger logger = LoggerFactory.getLogger(BulkUploadService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final IndexingLogRepository indexingLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${MAX_FILE_SIZE:100MB}")
    private String maxFileSizeConfig;

    public BulkUploadService(DocumentRepository documentRepository,
                           DocumentStorage documentStorage,
                           IndexingLogRepository indexingLogRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.indexingLogRepository = indexingLogRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Upload dokumen dengan mekanisme transactional/atomic
     * Jika salah satu proses gagal, seluruh operasi akan dibatalkan (rollback)
     */
    private BulkUploadResult uploadDocumentInternal(BulkUploadCommand command) {
        String storagePathForCleanup = null;
        try {
            logger.info("Memulai proses upload dokumen: {}", command.getOriginalFileName());

            // 1. Validasi command
            validateUploadCommand(command);

            // 2. Generate document ID dan encrypted filename
            DocumentId documentId = DocumentId.generate();
            String encryptedFileName = FileNameEncryptionUtil.generateEncryptedFileName(
                command.getOriginalFileName(),
                documentId.getValue()
            );

            logger.debug("Generated document ID: {} untuk file: {}", documentId.getValue(), command.getOriginalFileName());

            // 3. Cek idempotency - pastikan document belum ada yang diproses
            if (indexingLogRepository.existsByDocumentId(documentId.getValue())) {
                logger.warn("Document sudah ada dalam proses indexing: {}", documentId.getValue());
                return BulkUploadResult.failure("Document sudah dalam proses");
            }

            // 4. Simpan ke MinIO dengan rollback capability
            String storagePath;
            try {
                storagePath = documentStorage.store(
                    encryptedFileName,
                    new java.io.ByteArrayInputStream(command.getFileContent()),
                    command.getFileSize(),
                    command.getContentType()
                );
                logger.debug("File berhasil disimpan ke MinIO: {}", storagePath);
                storagePathForCleanup = storagePath;
            } catch (Exception e) {
                logger.error("Gagal menyimpan file ke MinIO: {}", e.getMessage());
                throw new BulkUploadException("Gagal menyimpan file ke storage", e);
            }

            // 5. Simpan document entity ke database
            Document document;
            try {
                document = new Document(
                    documentId,
                    encryptedFileName,
                    command.getOriginalFileName(),
                    command.getFileSize(),
                    command.getContentType(),
                    storagePath,
                    command.getUploader()
                );

                documentRepository.save(document);
                logger.debug("Document entity berhasil disimpan ke database");
            } catch (Exception e) {
                logger.error("Gagal menyimpan document ke database: {}", e.getMessage());
                // Rollback storage jika database gagal
                try {
                    documentStorage.delete(storagePath);
                } catch (Exception storageException) {
                    logger.error("Gagal menghapus file dari storage saat rollback: {}", storageException.getMessage());
                }
                throw new BulkUploadException("Gagal menyimpan document ke database", e);
            }

            // 6. Buat indexing log untuk tracking
            try {
                IndexingLog indexingLog = new IndexingLog(documentId.getValue());
                indexingLogRepository.save(indexingLog);
                logger.debug("Indexing log berhasil dibuat untuk document: {}", documentId.getValue());
            } catch (Exception e) {
                logger.error("Gagal membuat indexing log: {}", e.getMessage(), e);
                // Pastikan langkah 1-3 transactional: gagalkan operasi agar rollback DB & hapus file di MinIO
                throw new BulkUploadException("Gagal membuat indexing log", e);
            }

            // 7. Publish event untuk async processing (setelah commit dengan TransactionalEventListener)
            eventPublisher.publishEvent(
                new DocumentUploadEventListener.DocumentUploadedEvent(documentId, command.getUploader())
            );

            logger.info("Upload dokumen berhasil: {} dengan ID: {}", command.getOriginalFileName(), documentId.getValue());

            return BulkUploadResult.success(documentId.getValue(), "Upload berhasil");

        } catch (BulkUploadException e) {
            logger.error("Bulk upload gagal: {}", e.getMessage());
            // Upaya cleanup file dari MinIO jika sudah terlanjur diupload
            if (storagePathForCleanup != null) {
                try {
                    documentStorage.delete(storagePathForCleanup);
                    logger.debug("Cleanup file MinIO berhasil: {}", storagePathForCleanup);
                } catch (Exception cleanupEx) {
                    logger.warn("Gagal cleanup file MinIO saat rollback: {}", cleanupEx.getMessage());
                }
            }
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error saat bulk upload: {}", e.getMessage(), e);
            if (storagePathForCleanup != null) {
                try {
                    documentStorage.delete(storagePathForCleanup);
                    logger.debug("Cleanup file MinIO berhasil: {}", storagePathForCleanup);
                } catch (Exception cleanupEx) {
                    logger.warn("Gagal cleanup file MinIO saat rollback: {}", cleanupEx.getMessage());
                }
            }
            throw new BulkUploadException("Terjadi kesalahan tidak terduga saat upload", e);
        }
    }

    /**
     * Bulk upload multiple documents dengan atomic operations
     */
    private List<BulkUploadResult> uploadMultipleDocumentsInternal(List<BulkUploadCommand> commands) {
        return commands.stream()
                .map(this::uploadDocumentInternal)
                .toList();
    }

    // Implement BulkUploadUseCase by adapting to internal command/result types
    @Override
    @Transactional
    public com.example.DocIx.domain.port.in.BulkUploadUseCase.BulkUploadResult uploadDocument(
            com.example.DocIx.domain.port.in.BulkUploadUseCase.BulkUploadCommand command) {
        BulkUploadCommand internal = new BulkUploadCommand(
                command.getOriginalFileName(),
                command.getFileContent(),
                command.getFileSize(),
                command.getContentType(),
                command.getUploader());
        BulkUploadResult result = uploadDocumentInternal(internal);
        return new com.example.DocIx.domain.port.in.BulkUploadUseCase.BulkUploadResult(
                result.isSuccess(), result.getDocumentId(), result.getMessage(), result.getErrorMessage());
    }

    @Override
    @Transactional
    public List<com.example.DocIx.domain.port.in.BulkUploadUseCase.BulkUploadResult> uploadMultipleDocuments(
            List<com.example.DocIx.domain.port.in.BulkUploadUseCase.BulkUploadCommand> commands) {
        List<BulkUploadCommand> internal = commands.stream()
                .map(c -> new BulkUploadCommand(c.getOriginalFileName(), c.getFileContent(), c.getFileSize(),
                        c.getContentType(), c.getUploader()))
                .toList();
        return uploadMultipleDocumentsInternal(internal).stream()
                .map(r -> new com.example.DocIx.domain.port.in.BulkUploadUseCase.BulkUploadResult(
                        r.isSuccess(), r.getDocumentId(), r.getMessage(), r.getErrorMessage()))
                .toList();
    }

    private void validateUploadCommand(BulkUploadCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Upload command tidak boleh null");
        }

        long maxFileSize = parseMaxFileSize(maxFileSizeConfig);

        if (command.getFileSize() > maxFileSize) {
            throw new IllegalArgumentException("Ukuran file melebihi batas maksimum " + maxFileSizeConfig);
        }

        if (!isPdf(command.getContentType(), command.getOriginalFileName())) {
            throw new IllegalArgumentException("Hanya file PDF yang didukung");
        }

        if (command.getOriginalFileName() == null || command.getOriginalFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nama file tidak boleh kosong");
        }

        if (command.getUploader() == null || command.getUploader().trim().isEmpty()) {
            throw new IllegalArgumentException("Informasi uploader diperlukan");
        }

        if (command.getFileContent() == null || command.getFileContent().length == 0) {
            throw new IllegalArgumentException("Konten file tidak boleh kosong");
        }
    }

    private long parseMaxFileSize(String maxFileSizeConfig) {
        try {
            if (maxFileSizeConfig == null || maxFileSizeConfig.trim().isEmpty()) {
                return 50 * 1024 * 1024; // Default 50MB
            }

            if (maxFileSizeConfig.endsWith("MB")) {
                String sizeStr = maxFileSizeConfig.substring(0, maxFileSizeConfig.length() - 2);
                return Long.parseLong(sizeStr) * 1024 * 1024;
            } else if (maxFileSizeConfig.endsWith("KB")) {
                String sizeStr = maxFileSizeConfig.substring(0, maxFileSizeConfig.length() - 2);
                return Long.parseLong(sizeStr) * 1024;
            } else if (maxFileSizeConfig.endsWith("GB")) {
                String sizeStr = maxFileSizeConfig.substring(0, maxFileSizeConfig.length() - 2);
                return Long.parseLong(sizeStr) * 1024 * 1024 * 1024;
            } else {
                return Long.parseLong(maxFileSizeConfig); // Anggap byte
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid max file size config, using default 50MB: {}", e.getMessage());
            return 50 * 1024 * 1024; // Default 50MB
        }
    }

    private boolean isPdf(String contentType, String fileName) {
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        return ct.startsWith("application/pdf") ||
               ct.equals("application/x-pdf") ||
               (ct.equals("application/octet-stream") && fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    // Inner classes untuk command dan result
    public static class BulkUploadCommand {
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

        // Getters
        public String getOriginalFileName() { return originalFileName; }
        public byte[] getFileContent() { return fileContent; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public String getUploader() { return uploader; }
    }

    public static class BulkUploadResult {
        private final boolean success;
        private final String documentId;
        private final String message;
        private final String errorMessage;

        private BulkUploadResult(boolean success, String documentId, String message, String errorMessage) {
            this.success = success;
            this.documentId = documentId;
            this.message = message;
            this.errorMessage = errorMessage;
        }

        public static BulkUploadResult success(String documentId, String message) {
            return new BulkUploadResult(true, documentId, message, null);
        }

        public static BulkUploadResult failure(String errorMessage) {
            return new BulkUploadResult(false, null, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getDocumentId() { return documentId; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class BulkUploadException extends RuntimeException {
        public BulkUploadException(String message) {
            super(message);
        }

        public BulkUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
