package com.example.DocIx.adapter.in.messaging;

import com.example.DocIx.adapter.out.messaging.RabbitMQDocumentProcessingPublisher.DocumentProcessingMessage;
import com.example.DocIx.config.GracefulShutdownManager;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.ContentExtractor;
import com.example.DocIx.domain.port.out.DocumentRepository;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import com.example.DocIx.domain.port.out.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DocumentProcessingMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingMessageHandler.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final ContentExtractor contentExtractor;
    private final DocumentSearchEngine searchEngine;
    private final GracefulShutdownManager shutdownManager;

    // Track active processing tasks for graceful shutdown
    private final AtomicInteger activeProcessingTasks = new AtomicInteger(0);

    public DocumentProcessingMessageHandler(DocumentRepository documentRepository,
                                          DocumentStorage documentStorage,
                                          ContentExtractor contentExtractor,
                                          DocumentSearchEngine searchEngine,
                                          GracefulShutdownManager shutdownManager) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.contentExtractor = contentExtractor;
        this.searchEngine = searchEngine;
        this.shutdownManager = shutdownManager;
    }

    @RabbitListener(queues = "${docix.processing.queue.name}")
    public void handleDocumentProcessing(DocumentProcessingMessage message,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        // Check if shutdown is initiated - reject new messages
        if (shutdownManager.isShutdownInitiated()) {
            logger.warn("🛑 Rejecting new document processing request during shutdown: {}", message.getDocumentId());
            // In a real implementation, you would nack the message to requeue it
            return;
        }

        DocumentId documentId = DocumentId.of(message.getDocumentId());
        activeProcessingTasks.incrementAndGet();

        try {
            logger.info("🔄 Processing document: {} (Active tasks: {})", documentId, activeProcessingTasks.get());

            Optional<Document> documentOpt = documentRepository.findById(documentId);
            if (documentOpt.isEmpty()) {
                logger.error("❌ Document not found: {}", documentId);
                return;
            }

            Document document = documentOpt.get();

            if (!document.canBeProcessed()) {
                logger.warn("⚠️ Document cannot be processed in current state: {} - {}", documentId, document.getStatus());
                return;
            }

            // Mark as processing
            document.markAsProcessing();
            documentRepository.save(document);

            // Process document with shutdown checks
            processDocumentWithShutdownChecks(document);

        } catch (Exception e) {
            logger.error("❌ Failed to process document: {}", documentId, e);
            handleProcessingError(documentId, e.getMessage());
        } finally {
            int remainingTasks = activeProcessingTasks.decrementAndGet();
            logger.info("✅ Completed processing document: {} (Remaining active tasks: {})", documentId, remainingTasks);

            // Notify if this was the last task during shutdown
            if (remainingTasks == 0 && shutdownManager.isShutdownInitiated()) {
                logger.info("🎯 All document processing tasks completed during shutdown");
            }
        }
    }

    private void processDocumentWithShutdownChecks(Document document) {
        try {
            // Check shutdown status before starting intensive operations
            if (shutdownManager.isShutdownInitiated()) {
                logger.warn("🛑 Aborting document processing due to shutdown: {}", document.getId());
                return;
            }

            try (InputStream fileContent = documentStorage.retrieve(document.getStoragePath())) {

                // Check shutdown status before content extraction
                if (shutdownManager.isShutdownInitiated()) {
                    logger.warn("🛑 Aborting content extraction due to shutdown: {}", document.getId());
                    return;
                }

                // Extract text content
                String extractedContent = contentExtractor.extractText(fileContent, document.getFileName());

                // Check shutdown status before marking as processed
                if (shutdownManager.isShutdownInitiated()) {
                    logger.warn("🛑 Aborting final processing due to shutdown: {}", document.getId());
                    return;
                }

                // Mark as processed
                document.markAsProcessed(extractedContent);
                documentRepository.save(document);

                // Index in search engine (if not shutting down)
                if (!shutdownManager.isShutdownInitiated()) {
                    searchEngine.indexDocument(document);
                }

                logger.info("✅ Successfully processed document: {}", document.getId());

            }
        } catch (ContentExtractor.ContentExtractionException e) {
            logger.error("❌ Content extraction failed for document: {}", document.getId(), e);
            document.markAsFailed("Content extraction failed: " + e.getMessage());
            documentRepository.save(document);

        } catch (Exception e) {
            logger.error("❌ Unexpected error processing document: {}", document.getId(), e);
            document.markAsFailed("Processing failed: " + e.getMessage());
            documentRepository.save(document);
        }
    }

    private void handleProcessingError(DocumentId documentId, String errorMessage) {
        try {
            Optional<Document> documentOpt = documentRepository.findById(documentId);
            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();
                document.markAsFailed(errorMessage);
                documentRepository.save(document);
            }
        } catch (Exception e) {
            logger.error("Failed to update document status after error: {}", documentId, e);
        }
    }

    public int getActiveProcessingTasks() {
        return activeProcessingTasks.get();
    }
}
