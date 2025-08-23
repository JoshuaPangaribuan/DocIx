package com.example.DocIx.adapter.out.messaging;

import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.DocumentProcessingPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQDocumentProcessingPublisher implements DocumentProcessingPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public RabbitMQDocumentProcessingPublisher(RabbitTemplate rabbitTemplate,
                                             @Value("${docix.processing.exchange.name}") String exchangeName,
                                             @Value("${docix.processing.routing.key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    public void publishDocumentForProcessing(DocumentId documentId) {
        try {
            DocumentProcessingMessage message = new DocumentProcessingMessage(documentId.getValue());
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        } catch (Exception e) {
            throw new MessagingException("Failed to publish document for processing: " + documentId, e);
        }
    }

    public static class DocumentProcessingMessage {
        private String documentId;

        public DocumentProcessingMessage() {}

        public DocumentProcessingMessage(String documentId) {
            this.documentId = documentId;
        }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
    }

    public static class MessagingException extends RuntimeException {
        public MessagingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
