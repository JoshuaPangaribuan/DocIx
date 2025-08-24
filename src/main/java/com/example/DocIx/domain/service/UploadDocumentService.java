package com.example.DocIx.domain.service;

import com.example.DocIx.config.ShutdownHookManager;
import com.example.DocIx.domain.mapper.DocumentServiceMapper;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.in.UploadDocumentUseCase;
import com.example.DocIx.domain.port.out.DocumentProcessingPublisher;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentStorage;
import com.example.DocIx.domain.util.FileNameEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service untuk mengelola proses unggah dokumen
 * Menangani validasi, penyimpanan, dan inisiasi pemrosesan dokumen baru
 */
@Service
public class UploadDocumentService implements UploadDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentProcessingPublisher processingPublisher;
    private final DocumentServiceMapper documentServiceMapper;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // Batas maksimal 50MB
    private static final String SUPPORTED_CONTENT_TYPE = "application/pdf";

    private static final Logger logger = LoggerFactory.getLogger(UploadDocumentService.class);

    /**
     * Konstruktor untuk dependency injection
     * @param documentRepository repository untuk operasi database dokumen
     * @param documentStorage storage untuk menyimpan file fisik
     * @param processingPublisher publisher untuk mengirim pesan pemrosesan
     * @param documentServiceMapper mapper untuk konversi objek
     */
    public UploadDocumentService(DocumentRepository documentRepository,
                               DocumentStorage documentStorage,
                               DocumentProcessingPublisher processingPublisher,
                               DocumentServiceMapper documentServiceMapper) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.processingPublisher = processingPublisher;
        this.documentServiceMapper = documentServiceMapper;
    }

    /**
     * Mengunggah dokumen baru ke sistem
     * @param command perintah unggah yang berisi data file dan metadata
     * @return hasil unggah yang berisi informasi sukses atau gagal
     */
    @Override
    public UploadResult uploadDocument(UploadCommand command) {
        validateUploadCommand(command);

        DocumentId documentId = DocumentId.generate();
        // Generate nama file terenkripsi untuk penyimpanan yang aman
        String encryptedFileName = FileNameEncryptionUtil.generateEncryptedFileName(
            command.getOriginalFileName(),
            documentId.getValue()
        );

        // Simpan file di object storage menggunakan nama terenkripsi
        String storagePath = documentStorage.store(
            encryptedFileName,
            command.getFileContent(),
            command.getFileSize(),
            command.getContentType()
        );

        // Buat dan simpan entitas dokumen dengan nama file terenkripsi dan asli
        Document document = new Document(
            documentId,
            encryptedFileName,  // Digunakan untuk identifikasi penyimpanan
            command.getOriginalFileName(),  // Digunakan untuk unduhan
            command.getFileSize(),
            command.getContentType(),
            storagePath,
            command.getUploader()
        );

        documentRepository.save(document);

        // Publish untuk pemrosesan asinkron
        processingPublisher.publishDocumentForProcessing(documentId);

        return documentServiceMapper.createSuccessResult(documentId);
    }

    /**
     * Memvalidasi perintah unggah dokumen
     * @param command perintah unggah yang akan divalidasi
     * @throws IllegalArgumentException jika validasi gagal
     */
    private void validateUploadCommand(UploadCommand command) {
        if (command.getFileSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
        }

        if (!isPdf(command.getContentType(), command.getOriginalFileName())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        if (command.getOriginalFileName() == null || command.getOriginalFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        if (command.getUploader() == null || command.getUploader().trim().isEmpty()) {
            throw new IllegalArgumentException("Uploader information is required");
        }
    }

    /**
     * Memeriksa apakah file adalah PDF yang valid
     * @param contentType tipe konten MIME
     * @param fileName nama file
     * @return true jika file adalah PDF
     */
    private boolean isPdf(String contentType, String fileName) {
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        if (ct.startsWith("application/pdf")) {
            return true;
        }
        if (ct.equals("application/x-pdf")) {
            return true;
        }
        if (ct.equals("application/octet-stream") && fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return false;
    }
}
