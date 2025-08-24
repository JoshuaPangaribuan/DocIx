package com.example.DocIx.domain.service;

import com.example.DocIx.adapter.out.persistence.IndexingSegmentLogJpaEntity;
import com.example.DocIx.domain.model.*;
import com.example.DocIx.domain.port.out.*;
import com.example.DocIx.adapter.out.persistence.IndexingSegmentLogJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final ContentExtractor contentExtractor;
    private final DocumentSegmentationService segmentationService;
    private final DocumentSearchEngine searchEngine;
    private final IndexingLogRepository indexingLogRepository;
    private final IndexingSegmentLogJpaRepository segmentLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${docix.indexing.max-retry:3}")
    private int maxRetryCount;

    public DocumentIndexingService(
            DocumentRepository documentRepository,
            IndexingLogRepository indexingLogRepository,
            IndexingSegmentLogJpaRepository segmentLogRepository,
            DocumentStorage documentStorage,
            ContentExtractor contentExtractor,
            DocumentSegmentationService segmentationService,
            DocumentSearchEngine searchEngine) {
        this.documentRepository = documentRepository;
        this.indexingLogRepository = indexingLogRepository;
        this.segmentLogRepository = segmentLogRepository;
        this.documentStorage = documentStorage;
        this.contentExtractor = contentExtractor;
        this.segmentationService = segmentationService;
        this.searchEngine = searchEngine;
    }

    /**
     * Proses pengindeksan dokumen secara asynchronous
     * 1. Ambil file PDF dari MinIO
     * 2. Ekstrak konten
     * 3. Pecah menjadi segmen
     * 4. Indeks setiap segmen ke Elasticsearch
     * 5. Update status indexing
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

            // 4. Ekstrak konten PDF
            String extractedText = extractContentFromPdf(fileContent, document);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                handleIndexingFailure(indexingLog, document, "Gagal mengekstrak konten dari PDF atau file kosong");
                return;
            }

            // 5. Pecah dokumen menjadi segmen
            List<DocumentSegmentationService.DocumentSegment> segments =
                segmentationService.segmentDocument(extractedText, document);

            logger.info("Document {} dipecah menjadi {} segmen", documentId, segments.size());

            // 6. Simpan IndexingLog dulu untuk mendapatkan ID, kemudian inisialisasi segmen
            indexingLog.setTotalSegments(segments.size());
            indexingLog.setIndexingStatus(IndexingStatus.IN_PROGRESS);
            indexingLog.setUpdatedAt(java.time.LocalDateTime.now());

            // Simpan dulu untuk mendapatkan ID
            indexingLog = indexingLogRepository.save(indexingLog);

            // Sekarang baru inisialisasi segment logs dengan ID yang sudah ada
            initializeSegmentLogs(indexingLog, segments.size());

            // 7. Indeks setiap segmen
            indexDocumentSegments(segments, indexingLog);

            // 8. Update final status document
            updateDocumentFinalStatus(document, indexingLog);

            logger.info("Proses indexing selesai untuk document: {} dengan status: {}",
                       documentId, indexingLog.getIndexingStatus());

        } catch (Exception e) {
            logger.error("Error saat memproses indexing untuk document {}: {}", documentId, e.getMessage(), e);
            handleUnexpectedError(documentId, e);
        }
    }

    private void initializeSegmentLogs(IndexingLog indexingLog, int totalSegments) {
        try {
            Long indexingLogId = indexingLog.getId();

            // Strategi yang lebih aman: hapus segment logs lama dengan native query
            logger.debug("Menghapus segment logs lama untuk indexing_log_id: {}", indexingLogId);

            // Hapus dari collection dalam memory terlebih dahulu
            if (indexingLog.getSegmentLogs() != null) {
                indexingLog.getSegmentLogs().clear();
            }

            // Simpan IndexingLog tanpa segment logs terlebih dahulu
            indexingLog = indexingLogRepository.save(indexingLog);

            // Flush menggunakan EntityManager
            entityManager.flush();

            // Hapus segment logs yang ada di database
            segmentLogRepository.deleteByIndexingLogId(indexingLogId);

            // Flush lagi untuk memastikan delete tereksekusi
            entityManager.flush();

            // Tunggu sebentar untuk memastikan delete commit
            Thread.sleep(100);

            // Buat segment logs baru
            logger.debug("Membuat {} segment logs baru untuk indexing_log_id: {}", totalSegments, indexingLogId);
            for (int i = 1; i <= totalSegments; i++) {
                IndexingSegmentLog segmentLog = new IndexingSegmentLog(indexingLogId, i);
                indexingLog.getSegmentLogs().add(segmentLog);
            }

            // Simpan dengan segment logs baru
            indexingLogRepository.save(indexingLog);

            logger.debug("Berhasil inisialisasi {} segment logs untuk indexing_log_id: {}", totalSegments, indexingLogId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted saat inisialisasi segment logs: {}", e.getMessage());
            throw new RuntimeException("Thread interrupted", e);
        } catch (Exception e) {
            logger.error("Error saat inisialisasi segment logs untuk indexing_log_id {}: {}",
                        indexingLog.getId(), e.getMessage(), e);
            throw new RuntimeException("Gagal inisialisasi segment logs", e);
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

    private void indexDocumentSegments(List<DocumentSegmentationService.DocumentSegment> segments,
                                     IndexingLog indexingLog) {
        int successCount = 0;
        int failureCount = 0;

        for (DocumentSegmentationService.DocumentSegment segment : segments) {
            try {
                // Index segmen ke Elasticsearch menggunakan method yang benar
                searchEngine.indexDocument(segment);

                // Update segment log di database
                updateSegmentLogStatus(indexingLog.getId(), segment.getSegmentNumber(), SegmentStatus.INDEXED, null);
                successCount++;

                logger.debug("Segmen {} berhasil diindeks untuk document: {}",
                           segment.getSegmentNumber(), segment.getDocumentId());

            } catch (Exception e) {
                // Update segment log di database
                updateSegmentLogStatus(indexingLog.getId(), segment.getSegmentNumber(), SegmentStatus.FAILED, e.getMessage());
                failureCount++;

                logger.error("Gagal mengindeks segmen {} untuk document {}: {}",
                           segment.getSegmentNumber(), segment.getDocumentId(), e.getMessage());
            }
        }

        // Update IndexingLog dengan count yang akurat
        indexingLog.setSegmentsIndexed(successCount);
        indexingLog.setSegmentsFailed(failureCount);
        indexingLog.setUpdatedAt(java.time.LocalDateTime.now());

        // Update status berdasarkan hasil
        if (successCount + failureCount >= indexingLog.getTotalSegments()) {
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

        logger.info("Proses indexing segmen selesai - Berhasil: {}, Gagal: {}, Total: {}",
                   successCount, failureCount, segments.size());
    }

    private void updateSegmentLogStatus(Long indexingLogId, int segmentNumber, SegmentStatus status, String errorMessage) {
        try {
            // Cari segment log berdasarkan indexing_log_id dan segment_number
            Optional<IndexingSegmentLogJpaEntity> segmentLogOpt = segmentLogRepository
                    .findByIndexingLogIdAndSegmentNumber(indexingLogId, segmentNumber);

            if (segmentLogOpt.isPresent()) {
                IndexingSegmentLogJpaEntity segmentLog = segmentLogOpt.get();

                // Konversi SegmentStatus ke SegmentStatusEnum
                IndexingSegmentLogJpaEntity.SegmentStatusEnum entityStatus;
                switch (status) {
                    case INDEXED:
                        entityStatus = IndexingSegmentLogJpaEntity.SegmentStatusEnum.INDEXED;
                        break;
                    case FAILED:
                        entityStatus = IndexingSegmentLogJpaEntity.SegmentStatusEnum.FAILED;
                        break;
                    default:
                        entityStatus = IndexingSegmentLogJpaEntity.SegmentStatusEnum.PENDING;
                        break;
                }

                segmentLog.setSegmentStatus(entityStatus);

                if (status == SegmentStatus.INDEXED) {
                    segmentLog.setIndexedAt(java.time.LocalDateTime.now());
                } else if (status == SegmentStatus.FAILED) {
                    segmentLog.setErrorMessage(errorMessage);
                }

                segmentLogRepository.save(segmentLog);
                logger.debug("Updated segment log untuk segment {} dengan status {}", segmentNumber, status);
            } else {
                logger.warn("Segment log tidak ditemukan untuk indexing_log_id: {} dan segment_number: {}",
                           indexingLogId, segmentNumber);
            }
        } catch (Exception e) {
            logger.error("Gagal update segment log status untuk segment {}: {}", segmentNumber, e.getMessage());
        }
    }

    private void updateDocumentFinalStatus(Document document, IndexingLog indexingLog) {
        try {
            if (indexingLog.isFullyIndexed()) {
                document.markAsProcessed(generateDownloadUrl(document.getId().getValue()));
                logger.info("Document {} berhasil diindeks sepenuhnya", document.getId().getValue());
            } else {
                document.markAsFailed("Sebagian segmen gagal diindeks. Status: " + indexingLog.getIndexingStatus());
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
            log.getTotalSegments(),
            log.getSegmentsIndexed(),
            log.getSegmentsFailed(),
            log.getIndexingProgress()
        );
    }

    // Response class untuk status indexing
    public static class IndexingStatusResponse {
        private final String documentId;
        private final IndexingStatus status;
        private final int totalSegments;
        private final int segmentsIndexed;
        private final int segmentsFailed;
        private final double progress;

        public IndexingStatusResponse(String documentId, IndexingStatus status,
                                    int totalSegments, int segmentsIndexed,
                                    int segmentsFailed, double progress) {
            this.documentId = documentId;
            this.status = status;
            this.totalSegments = totalSegments;
            this.segmentsIndexed = segmentsIndexed;
            this.segmentsFailed = segmentsFailed;
            this.progress = progress;
        }

        // Getters
        public String getDocumentId() { return documentId; }
        public IndexingStatus getStatus() { return status; }
        public int getTotalSegments() { return totalSegments; }
        public int getSegmentsIndexed() { return segmentsIndexed; }
        public int getSegmentsFailed() { return segmentsFailed; }
        public double getProgress() { return progress; }
    }
}
