package com.example.DocIx.domain.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.DocIx.adapter.out.search.ElasticsearchDocumentSearchAdapter;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.ContentExtractor;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentStorage;
import com.example.DocIx.domain.port.out.PageExtractor;
import com.example.DocIx.domain.util.LoggingUtil;

@Service
public class DocumentReindexingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentReindexingService.class);

    private final DocumentRepository documentRepository;
    private final ElasticsearchDocumentSearchAdapter searchAdapter;
    private final DocumentIndexingService documentIndexingService;

    public DocumentReindexingService(DocumentRepository documentRepository,
            DocumentStorage documentStorage,
            ContentExtractor contentExtractor,
            ElasticsearchDocumentSearchAdapter searchAdapter,
            PageExtractor pageExtractor,
            DocumentIndexingService documentIndexingService) {
        this.documentRepository = documentRepository;
        this.searchAdapter = searchAdapter;
        this.documentIndexingService = documentIndexingService;
    }

    /**
     * Re-index dokumen yang sudah diproses tapi missing dari Elasticsearch
     * Menggunakan DocumentIndexingService untuk memastikan konsistensi dengan
     * indexing normal
     * dan populate IndexingPageLog dengan benar
     */
    public ReindexResult reindexDocument(String documentId) {
        logger.info("Starting re-indexing for document - DocumentId: {}", documentId);

        try {
            Optional<Document> documentOpt = documentRepository.findById(DocumentId.of(documentId));
            if (documentOpt.isEmpty()) {
                logger.error("Document not found for re-indexing - DocumentId: {}", documentId);
                return ReindexResult.error(documentId, "Document not found in database");
            }

            Document document = documentOpt.get();

            if (!document.isProcessed()) {
                logger.warn("Document not processed yet, skipping re-indexing - DocumentId: {}", documentId);
                return ReindexResult.skipped(documentId, "Document not processed yet");
            }

            String safeFileName = LoggingUtil.safeFileName(document.getOriginalFileName());
            logger.info("Re-indexing document - DocumentId: {}, File: {}", documentId, safeFileName);

            // Hapus pages lama dari Elasticsearch jika ada
            try {
                searchAdapter.deleteDocumentPages(document.getId());
                logger.debug("Deleted existing pages from Elasticsearch for document: {}", documentId);
            } catch (Exception e) {
                logger.warn("Failed to delete existing pages from Elasticsearch for document {}: {}",
                        documentId, e.getMessage());
            }

            // Gunakan DocumentIndexingService untuk memastikan konsistensi
            // dan populate IndexingPageLog dengan benar
            try {
                documentIndexingService.processDocumentIndexing(documentId);
                logger.info("Document re-indexed successfully using DocumentIndexingService - DocumentId: {}",
                        documentId);
                return ReindexResult.success(documentId, "Successfully re-indexed document with page logs");
            } catch (Exception e) {
                logger.error("Failed to re-index document using DocumentIndexingService - DocumentId: {}, Error: {}",
                        documentId, e.getMessage(), e);
                return ReindexResult.error(documentId, "Re-indexing failed: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Failed to re-index document - DocumentId: {}, Error: {}",
                    documentId, e.getMessage(), e);
            return ReindexResult.error(documentId, "Re-indexing failed: " + e.getMessage());
        }
    }

    /**
     * Re-index dokumen yang hilang dari Elasticsearch
     * Menggunakan page-based indexing
     */
    public int reindexMissingDocuments() {
        logger.info("Memulai reindex untuk dokumen yang hilang dari Elasticsearch");

        try {
            List<Document> processedDocuments = documentRepository.findByProcessed(true);
            int reindexedCount = 0;

            for (Document document : processedDocuments) {
                try {
                    // Cek apakah dokumen ada di Elasticsearch
                    boolean existsInElasticsearch = searchAdapter.documentExists(document.getId().getValue());

                    if (!existsInElasticsearch) {
                        logger.info("Reindexing missing document: {}", document.getId().getValue());
                        ReindexResult result = reindexDocument(document.getId().getValue());

                        if (result.isSuccess()) {
                            reindexedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error saat reindex document {}: {}", document.getId().getValue(), e.getMessage());
                }
            }

            logger.info("Reindex missing documents selesai. Total reindexed: {}", reindexedCount);
            return reindexedCount;

        } catch (Exception e) {
            logger.error("Error saat reindex missing documents", e);
            throw new RuntimeException("Gagal reindex missing documents", e);
        }
    }

    /**
     * Re-index semua dokumen
     * Menggunakan page-based indexing
     */
    public void reindexAllDocuments() {
        logger.info("Memulai reindex untuk semua dokumen");

        try {
            List<Document> allDocuments = documentRepository.findByProcessed(true);

            for (Document document : allDocuments) {
                try {
                    logger.info("Reindexing document: {}", document.getId().getValue());
                    reindexDocument(document.getId().getValue());
                } catch (Exception e) {
                    logger.error("Error saat reindex document {}: {}", document.getId().getValue(), e.getMessage());
                }
            }

            logger.info("Reindex all documents selesai. Total documents: {}", allDocuments.size());

        } catch (Exception e) {
            logger.error("Error saat reindex all documents", e);
            throw new RuntimeException("Gagal reindex all documents", e);
        }
    }

    /**
     * Result class untuk re-indexing operation
     */
    public static class ReindexResult {
        public enum Status {
            SUCCESS, PARTIAL, SKIPPED, ERROR
        }

        private final String documentId;
        private final Status status;
        private final String message;

        private ReindexResult(String documentId, Status status, String message) {
            this.documentId = documentId;
            this.status = status;
            this.message = message;
        }

        public static ReindexResult success(String documentId, String message) {
            return new ReindexResult(documentId, Status.SUCCESS, message);
        }

        public static ReindexResult partial(String documentId, String message) {
            return new ReindexResult(documentId, Status.PARTIAL, message);
        }

        public static ReindexResult skipped(String documentId, String message) {
            return new ReindexResult(documentId, Status.SKIPPED, message);
        }

        public static ReindexResult error(String documentId, String message) {
            return new ReindexResult(documentId, Status.ERROR, message);
        }

        // Getters
        public String getDocumentId() {
            return documentId;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public boolean isPartial() {
            return status == Status.PARTIAL;
        }

        public boolean isError() {
            return status == Status.ERROR;
        }

        public boolean isSkipped() {
            return status == Status.SKIPPED;
        }
    }
}
