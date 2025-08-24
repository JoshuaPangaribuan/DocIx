package com.example.DocIx.domain.event;

import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.DocumentProcessingPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentUploadEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadEventListener.class);

    private final DocumentProcessingPublisher processingPublisher;

    public DocumentUploadEventListener(DocumentProcessingPublisher processingPublisher) {
        this.processingPublisher = processingPublisher;
    }

    /**
     * Event listener yang berjalan setelah transaksi commit berhasil
     * Memastikan event hanya dipublish jika upload berhasil
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploadedEvent(DocumentUploadedEvent event) {
        try {
            logger.info("Menerima event document uploaded untuk: {}", event.getDocumentId().getValue());

            // Publish event untuk async processing
            processingPublisher.publishDocumentForProcessing(event.getDocumentId());

            logger.debug("Event processing berhasil dipublish untuk document: {}", event.getDocumentId().getValue());
        } catch (Exception e) {
            logger.error("Gagal publish event processing untuk document {}: {}",
                        event.getDocumentId().getValue(), e.getMessage(), e);
            // Event gagal publish tidak membatalkan upload, tapi perlu monitoring
        }
    }

    /**
     * Event yang dipublish saat document berhasil diupload
     */
    public static class DocumentUploadedEvent {
        private final DocumentId documentId;
        private final String uploader;

        public DocumentUploadedEvent(DocumentId documentId, String uploader) {
            this.documentId = documentId;
            this.uploader = uploader;
        }

        public DocumentId getDocumentId() { return documentId; }
        public String getUploader() { return uploader; }
    }
}
