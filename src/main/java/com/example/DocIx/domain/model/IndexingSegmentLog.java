package com.example.DocIx.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kelas entitas untuk mencatat log pengindeksan setiap segmen individual dokumen
 * Melacak status, waktu, dan detail kesalahan untuk setiap bagian dokumen yang diindeks
 */
public class IndexingSegmentLog {
    private Long id;
    private Long indexingLogId;
    private final int segmentNumber;
    private SegmentStatus segmentStatus;
    private LocalDateTime indexedAt;
    private String errorMessage;
    private int retryCount;
    private final LocalDateTime createdAt;

    /**
     * Konstruktor untuk membuat log segmen baru
     * @param indexingLogId ID dari log pengindeksan utama
     * @param segmentNumber nomor urut segmen dalam dokumen
     */
    public IndexingSegmentLog(Long indexingLogId, int segmentNumber) {
        this.indexingLogId = indexingLogId;
        this.segmentNumber = segmentNumber;
        this.segmentStatus = SegmentStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Konstruktor lengkap untuk JPA/rekonstruksi dari database
     * @param id ID unik log segmen
     * @param indexingLogId ID log pengindeksan utama
     * @param segmentNumber nomor segmen
     * @param segmentStatus status pengindeksan segmen
     * @param indexedAt waktu berhasil diindeks
     * @param errorMessage pesan kesalahan jika ada
     * @param retryCount jumlah percobaan ulang
     * @param createdAt waktu pembuatan log
     */
    public IndexingSegmentLog(Long id, Long indexingLogId, int segmentNumber,
                             SegmentStatus segmentStatus, LocalDateTime indexedAt,
                             String errorMessage, int retryCount, LocalDateTime createdAt) {
        this.id = id;
        this.indexingLogId = indexingLogId;
        this.segmentNumber = segmentNumber;
        this.segmentStatus = segmentStatus;
        this.indexedAt = indexedAt;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
    }

    // Metode bisnis untuk mengelola status segmen

    /**
     * Menandai segmen sebagai berhasil diindeks
     * Mengatur status menjadi INDEXED dan mencatat waktu pengindeksan
     */
    public void markAsIndexed() {
        this.segmentStatus = SegmentStatus.INDEXED;
        this.indexedAt = LocalDateTime.now();
        this.errorMessage = null; // Hapus pesan kesalahan sebelumnya
    }

    /**
     * Menandai segmen sebagai gagal diindeks
     * @param errorMessage pesan kesalahan yang menjelaskan kegagalan
     */
    public void markAsFailed(String errorMessage) {
        this.segmentStatus = SegmentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Mereset segmen untuk percobaan ulang pengindeksan
     * Mengembalikan status ke PENDING dan menghapus data kesalahan
     */
    public void resetForRetry() {
        this.segmentStatus = SegmentStatus.PENDING;
        this.errorMessage = null;
        this.indexedAt = null;
    }

    /**
     * Memeriksa apakah segmen dapat dicoba ulang
     * @param maxRetries batas maksimum percobaan ulang
     * @return true jika masih bisa dicoba ulang
     */
    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries && segmentStatus == SegmentStatus.FAILED;
    }

    // Metode getter dan setter untuk akses properti

    /** Mendapatkan ID log segmen */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** Mendapatkan ID log pengindeksan utama */
    public Long getIndexingLogId() { return indexingLogId; }
    public void setIndexingLogId(Long indexingLogId) { this.indexingLogId = indexingLogId; }

    /** Mendapatkan nomor segmen */
    public int getSegmentNumber() { return segmentNumber; }

    /** Mendapatkan status segmen */
    public SegmentStatus getSegmentStatus() { return segmentStatus; }
    public void setSegmentStatus(SegmentStatus segmentStatus) { this.segmentStatus = segmentStatus; }

    /** Mendapatkan waktu pengindeksan */
    public LocalDateTime getIndexedAt() { return indexedAt; }
    public void setIndexedAt(LocalDateTime indexedAt) { this.indexedAt = indexedAt; }

    /** Mendapatkan pesan kesalahan */
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /** Mendapatkan jumlah percobaan ulang */
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    /** Mendapatkan waktu pembuatan */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Membandingkan kesamaan berdasarkan ID log pengindeksan dan nomor segmen
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexingSegmentLog that = (IndexingSegmentLog) o;
        return segmentNumber == that.segmentNumber &&
               Objects.equals(indexingLogId, that.indexingLogId);
    }

    /**
     * Menghasilkan hash code berdasarkan ID log pengindeksan dan nomor segmen
     */
    @Override
    public int hashCode() {
        return Objects.hash(indexingLogId, segmentNumber);
    }
}
