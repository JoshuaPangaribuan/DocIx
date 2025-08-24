package com.example.DocIx.application.service;

import com.example.DocIx.adapter.out.search.ElasticsearchDocumentSearchAdapter;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.ContentExtractor;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentStorage;
import com.example.DocIx.domain.service.DocumentSegmentationService;
import com.example.DocIx.domain.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentReindexingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentReindexingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final ContentExtractor contentExtractor;
    private final ElasticsearchDocumentSearchAdapter searchAdapter;
    private final DocumentSegmentationService segmentationService;

    public DocumentReindexingService(DocumentRepository documentRepository,
                                   DocumentStorage documentStorage,
                                   ContentExtractor contentExtractor,
                                   ElasticsearchDocumentSearchAdapter searchAdapter,
                                   DocumentSegmentationService segmentationService) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.contentExtractor = contentExtractor;
        this.searchAdapter = searchAdapter;
        this.segmentationService = segmentationService;
    }

    /**
     * Re-index dokumen yang sudah diproses tapi missing dari Elasticsearch
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

            // Ambil content dari storage dan ekstrak ulang
            InputStream inputStream = documentStorage.retrieve(document.getStoragePath());
            String extractedContent = contentExtractor.extractText(inputStream, document.getOriginalFileName());

            if (extractedContent == null || extractedContent.trim().isEmpty()) {
                logger.error("No content extracted during re-indexing - DocumentId: {}", documentId);
                return ReindexResult.error(documentId, "No content could be extracted");
            }

            // Segment ulang content
            List<DocumentSegmentationService.DocumentSegment> segments =
                segmentationService.segmentDocument(extractedContent, document);

            logger.info("Content segmented for re-indexing - DocumentId: {}, Segments: {}",
                       documentId, segments.size());

            // Hapus segments lama jika ada
            searchAdapter.deleteDocumentSegments(document.getId());

            // Index segments baru
            int successCount = 0;
            for (DocumentSegmentationService.DocumentSegment segment : segments) {
                try {
                    searchAdapter.indexDocument(segment);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to re-index segment - DocumentId: {}, Segment: {}, Error: {}",
                               documentId, segment.getSegmentNumber(), e.getMessage());
                }
            }

            if (successCount == segments.size()) {
                logger.info("Document re-indexed successfully - DocumentId: {}, Segments: {}",
                           documentId, successCount);
                return ReindexResult.success(documentId, "Successfully re-indexed " + successCount + " segments");
            } else {
                logger.warn("Document re-indexed with errors - DocumentId: {}, Success: {}, Total: {}",
                           documentId, successCount, segments.size());
                return ReindexResult.partial(documentId,
                    "Partially re-indexed: " + successCount + "/" + segments.size() + " segments");
            }

        } catch (Exception e) {
            logger.error("Failed to re-index document - DocumentId: {}, Error: {}",
                        documentId, e.getMessage(), e);
            return ReindexResult.error(documentId, "Re-indexing failed: " + e.getMessage());
        }
    }

    /**
     * Re-index dokumen yang hilang dari Elasticsearch
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
        public enum Status { SUCCESS, PARTIAL, SKIPPED, ERROR }

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
        public String getDocumentId() { return documentId; }
        public Status getStatus() { return status; }
        public String getMessage() { return message; }

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
