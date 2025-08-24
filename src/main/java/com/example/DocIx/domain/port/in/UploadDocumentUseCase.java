package com.example.DocIx.domain.port.in;

import com.example.DocIx.domain.model.DocumentId;

import java.io.InputStream;

/**
 * Interface use case untuk mengunggah dokumen ke sistem
 * Mendefinisikan kontrak untuk operasi unggah dokumen
 */
public interface UploadDocumentUseCase {

    /**
     * Mengunggah dokumen baru ke sistem
     * @param command perintah unggah yang berisi data file dan metadata
     * @return hasil unggah yang berisi ID dokumen dan pesan status
     */
    UploadResult uploadDocument(UploadCommand command);

    /**
     * Command object yang berisi data untuk operasi unggah dokumen
     */
    class UploadCommand {
        private final String originalFileName;
        private final InputStream fileContent;
        private final long fileSize;
        private final String contentType;
        private final String uploader;

        /**
         * Konstruktor untuk membuat perintah unggah dokumen
         * @param originalFileName nama file asli yang diunggah
         * @param fileContent stream konten file
         * @param fileSize ukuran file dalam bytes
         * @param contentType tipe MIME dari file
         * @param uploader nama pengguna yang mengunggah
         */
        public UploadCommand(String originalFileName, InputStream fileContent,
                           long fileSize, String contentType, String uploader) {
            this.originalFileName = originalFileName;
            this.fileContent = fileContent;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.uploader = uploader;
        }

        /** Mendapatkan nama file asli */
        public String getOriginalFileName() { return originalFileName; }
        /** Mendapatkan stream konten file */
        public InputStream getFileContent() { return fileContent; }
        /** Mendapatkan ukuran file */
        public long getFileSize() { return fileSize; }
        /** Mendapatkan tipe konten */
        public String getContentType() { return contentType; }
        /** Mendapatkan nama pengunggah */
        public String getUploader() { return uploader; }
    }

    /**
     * Result object yang berisi hasil operasi unggah dokumen
     */
    class UploadResult {
        private final DocumentId documentId;
        private final String message;

        /**
         * Konstruktor untuk membuat hasil unggah
         * @param documentId ID dokumen yang berhasil diunggah
         * @param message pesan status unggah
         */
        public UploadResult(DocumentId documentId, String message) {
            this.documentId = documentId;
            this.message = message;
        }

        /** Mendapatkan ID dokumen */
        public DocumentId getDocumentId() { return documentId; }
        /** Mendapatkan pesan status */
        public String getMessage() { return message; }
    }
}
