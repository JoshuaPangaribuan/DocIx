package com.example.DocIx.domain.port.out;

import java.io.InputStream;

public interface ContentExtractor {
    String extractText(InputStream fileContent, String fileName) throws ContentExtractionException;

    class ContentExtractionException extends Exception {
        public ContentExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
