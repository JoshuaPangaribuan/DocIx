package com.example.DocIx.adapter.in.messaging;

import com.example.DocIx.adapter.out.messaging.RabbitMQDocumentProcessingPublisher.DocumentProcessingMessage;
import com.example.DocIx.config.GracefulShutdownManager;
import com.example.DocIx.domain.service.DocumentIndexingService;
import com.example.DocIx.domain.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DocumentProcessingMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingMessageHandler.class);

    private final DocumentIndexingService documentIndexingService;
    private final GracefulShutdownManager shutdownManager;

    // Track active processing tasks for graceful shutdown
    private final AtomicInteger activeProcessingTasks = new AtomicInteger(0);

    public DocumentProcessingMessageHandler(DocumentIndexingService documentIndexingService,
                                          GracefulShutdownManager shutdownManager) {
        this.documentIndexingService = documentIndexingService;
        this.shutdownManager = shutdownManager;
    }

    @RabbitListener(queues = "${docix.processing.queue.name}")
    public void handleDocumentProcessing(DocumentProcessingMessage message,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String documentId = message.getDocumentId();

        // Check if shutdown is in progress
        if (shutdownManager.isShuttingDown()) {
            logger.warn("Sistem sedang shutdown, menolak pemrosesan dokumen: {}", documentId);
            return;
        }

        activeProcessingTasks.incrementAndGet();

        try {
            logger.info("Memulai pemrosesan dokumen asinkron: {}", documentId);
            LoggingUtil.logPerformance("document_processing_start", documentId);

            // Proses indexing menggunakan DocumentIndexingService yang baru
            documentIndexingService.processDocumentIndexing(documentId);

            LoggingUtil.logPerformance("document_processing_complete", documentId);
            logger.info("Pemrosesan dokumen selesai: {}", documentId);

        } catch (Exception e) {
            logger.error("Error saat memproses dokumen {}: {}", documentId, e.getMessage(), e);
            LoggingUtil.logError("document_processing_error", documentId, e);
        } finally {
            activeProcessingTasks.decrementAndGet();
        }
    }

    /**
     * Get number of active processing tasks (for graceful shutdown dan monitoring)
     */
    public int getActiveProcessingTasksCount() {
        return activeProcessingTasks.get();
    }

    /**
     * Alias method untuk kompatibilitas dengan health indicator
     */
    public int getActiveProcessingTasks() {
        return getActiveProcessingTasksCount();
    }

    /**
     * Wait for all active processing tasks to complete
     */
    public void waitForProcessingCompletion(long maxWaitMs) {
        long startTime = System.currentTimeMillis();

        while (activeProcessingTasks.get() > 0 &&
               (System.currentTimeMillis() - startTime) < maxWaitMs) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("Menunggu penyelesaian {} tugas pemrosesan aktif", activeProcessingTasks.get());
    }
}
