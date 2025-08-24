package com.example.DocIx.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kelas entitas Document yang merepresentasikan dokumen dalam sistem
 * Berisi informasi lengkap tentang dokumen yang diunggah termasuk metadata dan status pemrosesan
 */
public class Document {
    private final DocumentId id;
    private final String fileName;
    private final String originalFileName;
    private final long fileSize;
    private final String contentType;
    private final String storagePath;
    private final String uploader;
    private final LocalDateTime uploadedAt;
    private String downloadUrl;
    private DocumentStatus status;
    private String errorMessage;
    private LocalDateTime lastProcessedAt;

    /**
     * Konstruktor untuk membuat instance Document baru
     * @param id ID unik dokumen
     * @param fileName nama file yang telah dienkripsi/dimodifikasi
     * @param originalFileName nama file asli yang diunggah
     * @param fileSize ukuran file dalam bytes
     * @param contentType tipe MIME dari file
     * @param storagePath jalur penyimpanan file di storage
     * @param uploader nama pengguna yang mengunggah dokumen
     */
    public Document(DocumentId id, String fileName, String originalFileName,
                   long fileSize, String contentType, String storagePath, String uploader) {
        this.id = Objects.requireNonNull(id, "Document ID cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.originalFileName = Objects.requireNonNull(originalFileName, "Original file name cannot be null");
        this.fileSize = fileSize;
        this.contentType = Objects.requireNonNull(contentType, "Content type cannot be null");
        this.storagePath = Objects.requireNonNull(storagePath, "Storage path cannot be null");
        this.uploader = Objects.requireNonNull(uploader, "Uploader cannot be null");
        this.uploadedAt = LocalDateTime.now();
        this.status = DocumentStatus.UPLOADED;
    }

    // Metode bisnis untuk mengelola status dokumen

    /**
     * Menandai dokumen sedang dalam proses pemrosesan
     * Mengubah status menjadi PROCESSING dan memperbarui waktu pemrosesan
     */
    public void markAsProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.lastProcessedAt = LocalDateTime.now();
    }

    /**
     * Menandai dokumen telah berhasil diproses
     * @param downloadUrl URL untuk mengunduh dokumen yang telah diproses
     */
    public void markAsProcessed(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        this.status = DocumentStatus.PROCESSED;
        this.lastProcessedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Menandai dokumen gagal diproses
     * @param errorMessage pesan kesalahan yang menjelaskan kegagalan
     */
    public void markAsFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastProcessedAt = LocalDateTime.now();
    }

    /**
     * Memeriksa apakah dokumen dapat diproses
     * @return true jika dokumen berstatus UPLOADED atau FAILED
     */
    public boolean canBeProcessed() {
        return status == DocumentStatus.UPLOADED || status == DocumentStatus.FAILED;
    }

    /**
     * Memeriksa apakah dokumen telah diproses
     * @return true jika dokumen berstatus PROCESSED
     */
    public boolean isProcessed() {
        return status == DocumentStatus.PROCESSED;
    }

    // Metode getter untuk mengakses properti dokumen

    /** Mendapatkan ID dokumen */
    public DocumentId getId() { return id; }

    /** Mendapatkan nama file yang dienkripsi */
    public String getFileName() { return fileName; }

    /** Mendapatkan nama file asli */
    public String getOriginalFileName() { return originalFileName; }

    /** Mendapatkan ukuran file dalam bytes */
    public long getFileSize() { return fileSize; }

    /** Mendapatkan tipe konten file */
    public String getContentType() { return contentType; }

    /** Mendapatkan jalur penyimpanan file */
    public String getStoragePath() { return storagePath; }

    /** Mendapatkan nama pengunggah */
    public String getUploader() { return uploader; }

    /** Mendapatkan waktu unggah */
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    /** Mendapatkan URL unduhan */
    public String getDownloadUrl() { return downloadUrl; }

    /** Mendapatkan status dokumen */
    public DocumentStatus getStatus() { return status; }

    /** Mendapatkan pesan kesalahan */
    public String getErrorMessage() { return errorMessage; }

    /** Mendapatkan waktu pemrosesan terakhir */
    public LocalDateTime getLastProcessedAt() { return lastProcessedAt; }

    /**
     * Membandingkan kesamaan dengan objek Document lain berdasarkan ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id);
    }

    /**
     * Menghasilkan hash code berdasarkan ID dokumen
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
