package com.example.DocIx.application.service;

import com.example.DocIx.domain.service.DocumentIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IndexingRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(IndexingRetryScheduler.class);

    private final DocumentIndexingService documentIndexingService;

    @Value("${docix.indexing.retry.interval:300000}")
    private long retryInterval;

    public IndexingRetryScheduler(DocumentIndexingService documentIndexingService) {
        this.documentIndexingService = documentIndexingService;
    }

    /**
     * Scheduler untuk retry indexing yang gagal
     * Berjalan setiap 5 menit (default) atau sesuai konfigurasi
     */
    @Scheduled(fixedDelayString = "${docix.indexing.retry.interval:300000}")
    public void retryFailedIndexing() {
        try {
            logger.debug("Memulai scheduled retry untuk indexing yang gagal");
            documentIndexingService.retryFailedIndexing();
            logger.debug("Selesai scheduled retry untuk indexing yang gagal");
        } catch (Exception e) {
            logger.error("Error saat scheduled retry indexing: {}", e.getMessage(), e);
        }
    }

    /**
     * Health check untuk monitoring scheduler
     */
    @Scheduled(fixedRate = 60000) // Setiap 1 menit
    public void healthCheck() {
        logger.debug("IndexingRetryScheduler health check - interval: {} ms", retryInterval);
    }
}
