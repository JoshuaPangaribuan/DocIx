package com.example.DocIx.adapter.in.messaging;

import com.example.DocIx.adapter.out.messaging.RabbitMQDocumentProcessingPublisher.DocumentProcessingMessage;
import com.example.DocIx.domain.service.DocumentIndexingService;
import com.example.DocIx.domain.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import com.rabbitmq.client.Channel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DocumentProcessingMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingMessageHandler.class);

    private final DocumentIndexingService documentIndexingService;

    // Track active processing tasks for graceful shutdown
    private final AtomicInteger activeProcessingTasks = new AtomicInteger(0);

    public DocumentProcessingMessageHandler(DocumentIndexingService documentIndexingService) {
        this.documentIndexingService = documentIndexingService;
    }

    @RabbitListener(queues = "${docix.processing.queue.name}")
    public void handleDocumentProcessing(DocumentProcessingMessage message,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                       Channel channel) {
        String documentId = message.getDocumentId();

        activeProcessingTasks.incrementAndGet();

        try {
            logger.info("Memulai pemrosesan dokumen asinkron: {}", documentId);
            LoggingUtil.logPerformance("document_processing_start", documentId);

            // Proses indexing menggunakan DocumentIndexingService yang baru
            documentIndexingService.processDocumentIndexing(documentId);

            LoggingUtil.logPerformance("document_processing_complete", documentId);
            logger.info("Pemrosesan dokumen selesai: {}", documentId);
            // manual ack on success
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            logger.error("Error saat memproses dokumen {}: {}", documentId, e.getMessage(), e);
            LoggingUtil.logError("document_processing_error", documentId, e);
            // nack and requeue for retry
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackEx) {
                logger.error("Gagal melakukan NACK untuk deliveryTag {}: {}", deliveryTag, nackEx.getMessage(), nackEx);
            }
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
