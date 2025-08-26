package com.example.DocIx.adapter.in.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DocIx.adapter.out.search.ElasticsearchDocumentSearchAdapter;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.IndexingStatus;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.IndexingLogRepository;
import com.example.DocIx.domain.service.DocumentIndexingService;
import com.example.DocIx.domain.service.DocumentReindexingService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final DocumentIndexingService documentIndexingService;
    private final IndexingLogRepository indexingLogRepository;
    private final DocumentRepository documentRepository;
    private final ElasticsearchDocumentSearchAdapter searchAdapter;
    private final DocumentReindexingService reindexingService;

    public AdminController(DocumentIndexingService documentIndexingService,
            IndexingLogRepository indexingLogRepository,
            DocumentRepository documentRepository,
            ElasticsearchDocumentSearchAdapter searchAdapter,
            DocumentReindexingService reindexingService) {
        this.documentIndexingService = documentIndexingService;
        this.indexingLogRepository = indexingLogRepository;
        this.documentRepository = documentRepository;
        this.searchAdapter = searchAdapter;
        this.reindexingService = reindexingService;
    }

    /**
     * Mendapatkan ringkasan status indexing untuk semua dokumen
     */
    @GetMapping("/indexing/summary")
    public ResponseEntity<IndexingSummaryResponse> getIndexingSummary() {
        try {
            logger.debug("Mengambil ringkasan status indexing");

            long pendingCount = indexingLogRepository.findByStatus(IndexingStatus.PENDING).size();
            long inProgressCount = indexingLogRepository.findByStatus(IndexingStatus.IN_PROGRESS).size();
            long fullyIndexedCount = indexingLogRepository.findByStatus(IndexingStatus.FULLY_INDEXED).size();
            long partiallyIndexedCount = indexingLogRepository.findByStatus(IndexingStatus.PARTIALLY_INDEXED).size();
            long failedCount = indexingLogRepository.findByStatus(IndexingStatus.FAILED).size();

            IndexingSummaryResponse summary = new IndexingSummaryResponse(
                    pendingCount,
                    inProgressCount,
                    fullyIndexedCount,
                    partiallyIndexedCount,
                    failedCount);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error saat mengambil ringkasan indexing", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Trigger manual retry untuk indexing yang gagal
     */
    @PostMapping("/indexing/retry-failed")
    public ResponseEntity<Map<String, String>> retryFailedIndexing() {
        try {
            logger.info("Memulai manual retry untuk indexing yang gagal");
            documentIndexingService.retryFailedIndexing();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Manual retry indexing berhasil dimulai"));
        } catch (Exception e) {
            logger.error("Error saat manual retry failed indexing", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai manual retry indexing: " + e.getMessage()));
        }
    }

    /**
     * Mengecek status konsistensi data antara database dan Elasticsearch
     */
    @GetMapping("/indexing/consistency-check")
    public ResponseEntity<IndexingConsistencyResponse> checkIndexingConsistency() {
        logger.info("Memulai pengecekan konsistensi indexing antara database dan Elasticsearch");

        try {
            // Ambil semua dokumen yang sudah diproses dari database
            List<Document> processedDocuments = documentRepository.findByProcessed(true);

            IndexingConsistencyResponse response = new IndexingConsistencyResponse();
            response.totalProcessedDocuments = processedDocuments.size();

            int indexedDocuments = 0;
            int missingDocuments = 0;
            List<String> missingDocumentIds = new ArrayList<>();

            for (Document doc : processedDocuments) {
                try {
                    // Cek apakah dokumen ada di Elasticsearch
                    boolean existsInElasticsearch = searchAdapter.documentExists(doc.getId().getValue());

                    if (existsInElasticsearch) {
                        indexedDocuments++;
                    } else {
                        missingDocuments++;
                        missingDocumentIds.add(doc.getId().getValue());
                        logger.warn("Dokumen hilang dari Elasticsearch: {}", doc.getId().getValue());
                    }
                } catch (Exception e) {
                    logger.error("Error saat mengecek dokumen di Elasticsearch: {}", doc.getId().getValue(), e);
                    missingDocuments++;
                    missingDocumentIds.add(doc.getId().getValue());
                }
            }

            response.indexedDocuments = indexedDocuments;
            response.missingDocuments = missingDocuments;
            response.missingDocumentIds = missingDocumentIds;
            response.consistencyPercentage = processedDocuments.size() > 0
                    ? (double) indexedDocuments / processedDocuments.size() * 100
                    : 100.0;

            logger.info("Konsistensi indexing: {}/{} dokumen konsisten ({}%)",
                    indexedDocuments, processedDocuments.size(), response.consistencyPercentage);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error saat mengecek konsistensi indexing", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reindex semua dokumen yang hilang dari Elasticsearch
     */
    @PostMapping("/indexing/reindex-missing")
    public ResponseEntity<Map<String, Object>> reindexMissingDocuments() {
        try {
            logger.info("Memulai reindex untuk dokumen yang hilang");

            int reindexedCount = reindexingService.reindexMissingDocuments();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Reindex dokumen yang hilang berhasil dimulai",
                    "reindexedCount", reindexedCount));
        } catch (Exception e) {
            logger.error("Error saat reindex missing documents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai reindex: " + e.getMessage()));
        }
    }

    /**
     * Reindex semua dokumen
     */
    @PostMapping("/indexing/reindex-all")
    public ResponseEntity<Map<String, String>> reindexAllDocuments() {
        try {
            logger.info("Memulai reindex untuk semua dokumen");
            reindexingService.reindexAllDocuments();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Reindex semua dokumen berhasil dimulai"));
        } catch (Exception e) {
            logger.error("Error saat reindex all documents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai reindex semua dokumen: " + e.getMessage()));
        }
    }

    // Response classes
    public static class IndexingSummaryResponse {
        private final long pendingCount;
        private final long inProgressCount;
        private final long fullyIndexedCount;
        private final long partiallyIndexedCount;
        private final long failedCount;
        private final long totalCount;

        public IndexingSummaryResponse(long pendingCount, long inProgressCount,
                long fullyIndexedCount, long partiallyIndexedCount,
                long failedCount) {
            this.pendingCount = pendingCount;
            this.inProgressCount = inProgressCount;
            this.fullyIndexedCount = fullyIndexedCount;
            this.partiallyIndexedCount = partiallyIndexedCount;
            this.failedCount = failedCount;
            this.totalCount = pendingCount + inProgressCount + fullyIndexedCount +
                    partiallyIndexedCount + failedCount;
        }

        // Getters
        public long getPendingCount() {
            return pendingCount;
        }

        public long getInProgressCount() {
            return inProgressCount;
        }

        public long getFullyIndexedCount() {
            return fullyIndexedCount;
        }

        public long getPartiallyIndexedCount() {
            return partiallyIndexedCount;
        }

        public long getFailedCount() {
            return failedCount;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public double getSuccessRate() {
            if (totalCount == 0)
                return 0.0;
            return (double) fullyIndexedCount / totalCount * 100;
        }
    }

    public static class IndexingConsistencyResponse {
        public int totalProcessedDocuments;
        public int indexedDocuments;
        public int missingDocuments;
        public double consistencyPercentage;
        public List<String> missingDocumentIds;

        // Default constructor
        public IndexingConsistencyResponse() {
        }

        // Getters
        public int getTotalProcessedDocuments() {
            return totalProcessedDocuments;
        }

        public int getIndexedDocuments() {
            return indexedDocuments;
        }

        public int getMissingDocuments() {
            return missingDocuments;
        }

        public double getConsistencyPercentage() {
            return consistencyPercentage;
        }

        public List<String> getMissingDocumentIds() {
            return missingDocumentIds;
        }
    }
}
