package com.example.DocIx.domain.port.out;

import java.io.InputStream;
import java.util.List;

/**
 * Interface untuk ekstraksi teks PDF per halaman
 * Menggantikan pendekatan segmentasi dengan ekstraksi per halaman
 */
public interface PageExtractor {

    /**
     * Ekstrak teks dari PDF per halaman
     * 
     * @param fileContent InputStream dari file PDF
     * @param fileName    nama file PDF
     * @param documentId  ID dokumen dari database (UUID string)
     * @return List dari DocumentPage yang berisi informasi setiap halaman
     * @throws PageExtractionException jika terjadi error saat ekstraksi
     */
    List<DocumentPage> extractPages(InputStream fileContent, String fileName, String documentId)
            throws PageExtractionException;

    /**
     * Representasi halaman dokumen
     */
    class DocumentPage {
        private final String documentId;
        private final int pageNumber;
        private final String content;

        public DocumentPage(String documentId, int pageNumber, String content) {
            this.documentId = documentId;
            this.pageNumber = pageNumber;
            this.content = content;
        }

        public String getDocumentId() {
            return documentId;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "DocumentPage{" +
                    "documentId=" + documentId +
                    ", pageNumber=" + pageNumber +
                    ", contentLength=" + (content != null ? content.length() : 0) +
                    '}';
        }
    }

    /**
     * Exception untuk error ekstraksi halaman
     */
    class PageExtractionException extends Exception {
        public PageExtractionException(String message, Throwable cause) {
            super(message, cause);
        }

        public PageExtractionException(String message) {
            super(message);
        }
    }
}