package com.example.DocIx.domain.service;

import com.example.DocIx.adapter.out.persistence.IndexingPageLogJpaEntity;
import com.example.DocIx.domain.model.*;
import com.example.DocIx.domain.port.out.*;
import com.example.DocIx.adapter.out.persistence.IndexingPageLogJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final ContentExtractor contentExtractor;
    private final PageExtractor pageExtractor;
    private final DocumentSearchEngine searchEngine;
    private final IndexingLogRepository indexingLogRepository;
    private final IndexingPageLogJpaRepository pageLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${docix.indexing.max-retry:3}")
    private int maxRetryCount;

    public DocumentIndexingService(
            DocumentRepository documentRepository,
            IndexingLogRepository indexingLogRepository,
            IndexingPageLogJpaRepository pageLogRepository,
            DocumentStorage documentStorage,
            ContentExtractor contentExtractor,
            PageExtractor pageExtractor,
            DocumentSearchEngine searchEngine) {
        this.documentRepository = documentRepository;
        this.indexingLogRepository = indexingLogRepository;
        this.pageLogRepository = pageLogRepository;
        this.documentStorage = documentStorage;
        this.contentExtractor = contentExtractor;
        this.pageExtractor = pageExtractor;
        this.searchEngine = searchEngine;
    }

    /**
     * Proses pengindeksan dokumen secara asynchronous
     * 1. Ambil file PDF dari MinIO
     * 2. Ekstrak konten per halaman
     * 3. Indeks setiap halaman ke Elasticsearch
     * 4. Update status indexing
     */
    @Transactional
    public void processDocumentIndexing(String documentId) {
        logger.info("Memulai proses indexing untuk document: {}", documentId);

        try {
            // 1. Cari atau buat indexing log
            IndexingLog indexingLog = getOrCreateIndexingLog(documentId);

            // 2. Ambil document dari database
            Optional<Document> documentOpt = documentRepository.findById(new DocumentId(documentId));
            if (documentOpt.isEmpty()) {
                logger.error("Document tidak ditemukan: {}", documentId);
                indexingLog.markAsFailed("Document tidak ditemukan di database");
                indexingLogRepository.save(indexingLog);
                return;
            }

            Document document = documentOpt.get();
            document.markAsProcessing();
            documentRepository.save(document);

            // 3. Ambil file dari MinIO
            byte[] fileContent = retrieveFileFromStorage(document);
            if (fileContent == null) {
                handleIndexingFailure(indexingLog, document, "Gagal mengambil file dari storage");
                return;
            }

            // 4. Ekstrak konten PDF per halaman
            List<PageExtractor.DocumentPage> pages = extractPagesFromPdf(fileContent, document);
            if (pages == null || pages.isEmpty()) {
                handleIndexingFailure(indexingLog, document, "Gagal mengekstrak halaman dari PDF atau file kosong");
                return;
            }

            logger.info("Document {} berhasil diekstrak menjadi {} halaman", documentId, pages.size());

            // 5. Simpan IndexingLog dulu untuk mendapatkan ID, kemudian inisialisasi page logs
            indexingLog.setTotalPages(pages.size());
            indexingLog.setIndexingStatus(IndexingStatus.IN_PROGRESS);
            indexingLog.setUpdatedAt(java.time.LocalDateTime.now());

            // Simpan dulu untuk mendapatkan ID
            indexingLog = indexingLogRepository.save(indexingLog);

            // Sekarang baru inisialisasi page logs dengan ID yang sudah ada
            initializePageLogs(indexingLog, pages.size());

            // 6. Indeks setiap halaman
            indexDocumentPages(pages, indexingLog);

            // 7. Update final status document
            updateDocumentFinalStatus(document, indexingLog);

            logger.info("Proses indexing selesai untuk document: {} dengan status: {}",
                       documentId, indexingLog.getIndexingStatus());

        } catch (Exception e) {
            logger.error("Error saat memproses indexing untuk document {}: {}", documentId, e.getMessage(), e);
            handleUnexpectedError(documentId, e);
        }
    }

    private void initializePageLogs(IndexingLog indexingLog, int totalPages) {
        try {
            Long indexingLogId = indexingLog.getId();

            // Strategi yang lebih aman: hapus page logs lama dengan native query
            logger.debug("Menghapus page logs lama untuk indexing_log_id: {}", indexingLogId);

            // Hapus dari collection dalam memory terlebih dahulu
            if (indexingLog.getPageLogs() != null) {
                indexingLog.getPageLogs().clear();
            }

            // Simpan IndexingLog tanpa page logs terlebih dahulu
            indexingLog = indexingLogRepository.save(indexingLog);

            // Flush menggunakan EntityManager
            entityManager.flush();

            // Hapus page logs yang ada di database
            pageLogRepository.deleteByIndexingLogId(indexingLogId);

            // Flush lagi untuk memastikan delete tereksekusi
            entityManager.flush();

            // Tunggu sebentar untuk memastikan delete commit
            Thread.sleep(100);

            // Buat page logs baru
            logger.debug("Membuat {} page logs baru untuk indexing_log_id: {}", totalPages, indexingLogId);
            for (int i = 1; i <= totalPages; i++) {
                IndexingPageLog pageLog = new IndexingPageLog(indexingLogId, i);
                indexingLog.getPageLogs().add(pageLog);
            }

            // Simpan dengan page logs baru
            indexingLogRepository.save(indexingLog);

            logger.debug("Berhasil inisialisasi {} page logs untuk indexing_log_id: {}", totalPages, indexingLogId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted saat inisialisasi page logs: {}", e.getMessage());
            throw new RuntimeException("Thread interrupted", e);
        } catch (Exception e) {
            logger.error("Error saat inisialisasi page logs untuk indexing_log_id {}: {}",
                        indexingLog.getId(), e.getMessage(), e);
            throw new RuntimeException("Gagal inisialisasi page logs", e);
        }
    }

    private IndexingLog getOrCreateIndexingLog(String documentId) {
        return indexingLogRepository.findByDocumentId(documentId)
                .orElseGet(() -> {
                    IndexingLog newLog = new IndexingLog(documentId);
                    return indexingLogRepository.save(newLog);
                });
    }

    private byte[] retrieveFileFromStorage(Document document) {
        try {
            InputStream inputStream = documentStorage.retrieve(document.getStoragePath());
            return inputStream.readAllBytes();
        } catch (Exception e) {
            logger.error("Gagal mengambil file dari storage untuk document {}: {}",
                        document.getId().getValue(), e.getMessage());
            return null;
        }
    }

    private String extractContentFromPdf(byte[] fileContent, Document document) {
        try {
            return contentExtractor.extractText(
                new java.io.ByteArrayInputStream(fileContent),
                document.getOriginalFileName()
            );
        } catch (Exception e) {
            logger.error("Gagal mengekstrak konten dari PDF untuk document {}: {}",
                        document.getId().getValue(), e.getMessage());
            return null;
        }
    }

    private List<PageExtractor.DocumentPage> extractPagesFromPdf(byte[] fileContent, Document document) {
        try {
            return pageExtractor.extractPages(
                new java.io.ByteArrayInputStream(fileContent),
                document.getOriginalFileName(),
                document.getId().getValue()
            );
        } catch (Exception e) {
            logger.error("Gagal mengekstrak halaman dari PDF untuk document {}: {}",
                        document.getId().getValue(), e.getMessage());
            return null;
        }
    }

    private void indexDocumentPages(List<PageExtractor.DocumentPage> pages,
                                   IndexingLog indexingLog) {
        int successCount = 0;
        int failureCount = 0;

        for (PageExtractor.DocumentPage page : pages) {
            try {
                // Index halaman ke Elasticsearch
                searchEngine.indexDocumentPage(page);

                // Update page log di database
                updatePageLogStatus(indexingLog.getId(), page.getPageNumber(), PageStatus.INDEXED, null);
                successCount++;

                logger.debug("Halaman {} berhasil diindeks untuk document: {}",
                           page.getPageNumber(), page.getDocumentId());

            } catch (Exception e) {
                // Update page log di database
                updatePageLogStatus(indexingLog.getId(), page.getPageNumber(), PageStatus.FAILED, e.getMessage());
                failureCount++;

                logger.error("Gagal mengindeks halaman {} untuk document {}: {}",
                           page.getPageNumber(), page.getDocumentId(), e.getMessage());
            }
        }

        // Update IndexingLog dengan count yang akurat
        indexingLog.setPagesIndexed(successCount);
        indexingLog.setPagesFailed(failureCount);
        indexingLog.setUpdatedAt(java.time.LocalDateTime.now());

        // Update status berdasarkan hasil
        if (successCount + failureCount >= indexingLog.getTotalPages()) {
            if (failureCount == 0) {
                indexingLog.setIndexingStatus(IndexingStatus.FULLY_INDEXED);
            } else if (successCount > 0) {
                indexingLog.setIndexingStatus(IndexingStatus.PARTIALLY_INDEXED);
            } else {
                indexingLog.setIndexingStatus(IndexingStatus.FAILED);
            }
        }

        // Simpan perubahan indexing log ke database
        indexingLogRepository.save(indexingLog);

        logger.info("Proses indexing halaman selesai - Berhasil: {}, Gagal: {}, Total: {}",
                   successCount, failureCount, pages.size());
    }

    private void updatePageLogStatus(Long indexingLogId, int pageNumber, PageStatus status, String errorMessage) {
        try {
            // Cari page log berdasarkan indexing_log_id dan page_number
            Optional<IndexingPageLogJpaEntity> pageLogOpt = pageLogRepository
                .findByIndexingLogIdAndPageNumber(indexingLogId, pageNumber);

            if (pageLogOpt.isPresent()) {
                IndexingPageLogJpaEntity pageLog = pageLogOpt.get();

                // Konversi PageStatus ke PageStatusEnum
                IndexingPageLogJpaEntity.PageStatusEnum entityStatus;
                switch (status) {
                    case INDEXED:
                        entityStatus = IndexingPageLogJpaEntity.PageStatusEnum.INDEXED;
                        break;
                    case FAILED:
                        entityStatus = IndexingPageLogJpaEntity.PageStatusEnum.FAILED;
                        break;
                    default:
                        entityStatus = IndexingPageLogJpaEntity.PageStatusEnum.PENDING;
                        break;
                }

                pageLog.setPageStatus(entityStatus);
                pageLog.setErrorMessage(errorMessage);
                if (status == PageStatus.INDEXED) {
                    pageLog.setIndexedAt(LocalDateTime.now());
                } else if (status == PageStatus.FAILED) {
                    pageLog.setRetryCount(pageLog.getRetryCount() + 1);
                }

                pageLogRepository.save(pageLog);
                logger.debug("Updated page log untuk halaman {} dengan status {}", pageNumber, status);
            } else {
                logger.warn("Page log tidak ditemukan untuk indexing_log_id: {} dan page_number: {}",
                           indexingLogId, pageNumber);
            }
        } catch (Exception e) {
            logger.error("Gagal update page log status untuk halaman {}: {}", pageNumber, e.getMessage());
        }
    }

    private void updateDocumentFinalStatus(Document document, IndexingLog indexingLog) {
        try {
            if (indexingLog.isFullyIndexed()) {
                document.markAsProcessed(generateDownloadUrl(document.getId().getValue()));
                logger.info("Document {} berhasil diindeks sepenuhnya", document.getId().getValue());
            } else {
                document.markAsFailed("Sebagian halaman gagal diindeks. Status: " + indexingLog.getIndexingStatus());
                logger.warn("Document {} hanya sebagian berhasil diindeks", document.getId().getValue());
            }

            documentRepository.save(document);
        } catch (Exception e) {
            logger.error("Gagal update final status untuk document {}: {}",
                        document.getId().getValue(), e.getMessage());
        }
    }

    private void handleIndexingFailure(IndexingLog indexingLog, Document document, String errorMessage) {
        indexingLog.markAsFailed(errorMessage);
        indexingLogRepository.save(indexingLog);

        document.markAsFailed(errorMessage);
        documentRepository.save(document);

        logger.error("Indexing gagal untuk document {}: {}", document.getId().getValue(), errorMessage);
    }

    private void handleUnexpectedError(String documentId, Exception e) {
        try {
            IndexingLog indexingLog = getOrCreateIndexingLog(documentId);
            indexingLog.markAsFailed("Unexpected error: " + e.getMessage());
            indexingLogRepository.save(indexingLog);

            // Update document status jika ada
            documentRepository.findById(new DocumentId(documentId))
                    .ifPresent(document -> {
                        document.markAsFailed("Unexpected error during indexing: " + e.getMessage());
                        documentRepository.save(document);
                    });
        } catch (Exception ex) {
            logger.error("Gagal menangani unexpected error untuk document {}: {}", documentId, ex.getMessage());
        }
    }

    private String generateDownloadUrl(String documentId) {
        return "/api/documents/download/" + documentId;
    }

    /**
     * Retry indexing untuk dokumen yang gagal
     */
    @Transactional
    public void retryFailedIndexing() {
        logger.info("Memulai retry untuk indexing yang gagal");

        List<IndexingLog> failedLogs = indexingLogRepository.findFailedIndexingForRetry(maxRetryCount);

        for (IndexingLog failedLog : failedLogs) {
            try {
                logger.info("Mencoba ulang indexing untuk document: {}", failedLog.getDocumentId());
                processDocumentIndexing(failedLog.getDocumentId());
            } catch (Exception e) {
                logger.error("Retry indexing gagal untuk document {}: {}",
                           failedLog.getDocumentId(), e.getMessage());
            }
        }

        logger.info("Selesai retry untuk {} dokumen yang gagal", failedLogs.size());
    }

    /**
     * Mendapatkan status indexing untuk document
     */
    public IndexingStatusResponse getIndexingStatus(String documentId) {
        Optional<IndexingLog> logOpt = indexingLogRepository.findByDocumentId(documentId);

        if (logOpt.isEmpty()) {
            return new IndexingStatusResponse(documentId, IndexingStatus.PENDING, 0, 0, 0, 0.0);
        }

        IndexingLog log = logOpt.get();
        return new IndexingStatusResponse(
            documentId,
            log.getIndexingStatus(),
            log.getTotalPages(),
            log.getPagesIndexed(),
            log.getPagesFailed(),
            log.getIndexingProgress()
        );
    }

    // Response class untuk status indexing
    public static class IndexingStatusResponse {
        private final String documentId;
        private final IndexingStatus status;
        private final int totalPages;
        private final int pagesIndexed;
        private final int pagesFailed;
        private final double progress;

        public IndexingStatusResponse(String documentId, IndexingStatus status,
                                    int totalPages, int pagesIndexed,
                                    int pagesFailed, double progress) {
            this.documentId = documentId;
            this.status = status;
            this.totalPages = totalPages;
            this.pagesIndexed = pagesIndexed;
            this.pagesFailed = pagesFailed;
            this.progress = progress;
        }

        // Getters
        public String getDocumentId() { return documentId; }
        public IndexingStatus getStatus() { return status; }
        public int getTotalPages() { return totalPages; }
        public int getPagesIndexed() { return pagesIndexed; }
        public int getPagesFailed() { return pagesFailed; }
        public double getProgress() { return progress; }
    }
}
