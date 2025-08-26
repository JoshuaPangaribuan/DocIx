package com.example.DocIx.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kelas entitas untuk mencatat log pengindeksan setiap halaman individual dokumen
 * Melacak status, waktu, dan detail kesalahan untuk setiap halaman dokumen yang diindeks
 */
public class IndexingPageLog {
    private Long id;
    private Long indexingLogId;
    private final int pageNumber;
    private PageStatus pageStatus;
    private LocalDateTime indexedAt;
    private String errorMessage;
    private int retryCount;
    private final LocalDateTime createdAt;

    /**
     * Konstruktor untuk membuat log halaman baru
     * @param indexingLogId ID dari log pengindeksan utama
     * @param pageNumber nomor urut halaman dalam dokumen
     */
    public IndexingPageLog(Long indexingLogId, int pageNumber) {
        this.indexingLogId = indexingLogId;
        this.pageNumber = pageNumber;
        this.pageStatus = PageStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Konstruktor lengkap untuk JPA/rekonstruksi dari database
     * @param id ID unik log halaman
     * @param indexingLogId ID log pengindeksan utama
     * @param pageNumber nomor halaman
     * @param pageStatus status pengindeksan halaman
     * @param indexedAt waktu berhasil diindeks
     * @param errorMessage pesan kesalahan jika ada
     * @param retryCount jumlah percobaan ulang
     * @param createdAt waktu pembuatan log
     */
    public IndexingPageLog(Long id, Long indexingLogId, int pageNumber,
                          PageStatus pageStatus, LocalDateTime indexedAt,
                          String errorMessage, int retryCount, LocalDateTime createdAt) {
        this.id = id;
        this.indexingLogId = indexingLogId;
        this.pageNumber = pageNumber;
        this.pageStatus = pageStatus;
        this.indexedAt = indexedAt;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
    }

    // Metode bisnis untuk mengelola status halaman

    /**
     * Menandai halaman sebagai berhasil diindeks
     * Mengatur status menjadi INDEXED dan mencatat waktu pengindeksan
     */
    public void markAsIndexed() {
        this.pageStatus = PageStatus.INDEXED;
        this.indexedAt = LocalDateTime.now();
        this.errorMessage = null; // Hapus pesan kesalahan sebelumnya
    }

    /**
     * Menandai halaman sebagai gagal diindeks
     * @param errorMessage pesan kesalahan yang menjelaskan kegagalan
     */
    public void markAsFailed(String errorMessage) {
        this.pageStatus = PageStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Mereset halaman untuk percobaan ulang pengindeksan
     * Mengembalikan status ke PENDING dan menghapus data kesalahan
     */
    public void resetForRetry() {
        this.pageStatus = PageStatus.PENDING;
        this.errorMessage = null;
        this.indexedAt = null;
    }

    /**
     * Memeriksa apakah halaman dapat dicoba ulang
     * @param maxRetries batas maksimum percobaan ulang
     * @return true jika masih bisa dicoba ulang
     */
    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries && pageStatus == PageStatus.FAILED;
    }

    // Metode getter dan setter untuk akses properti

    /** Mendapatkan ID log halaman */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** Mendapatkan ID log pengindeksan utama */
    public Long getIndexingLogId() { return indexingLogId; }
    public void setIndexingLogId(Long indexingLogId) { this.indexingLogId = indexingLogId; }

    /** Mendapatkan nomor halaman */
    public int getPageNumber() { return pageNumber; }

    /** Mendapatkan status halaman */
    public PageStatus getPageStatus() { return pageStatus; }
    public void setPageStatus(PageStatus pageStatus) { this.pageStatus = pageStatus; }

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
     * Membandingkan kesamaan berdasarkan ID log pengindeksan dan nomor halaman
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexingPageLog that = (IndexingPageLog) o;
        return pageNumber == that.pageNumber &&
               Objects.equals(indexingLogId, that.indexingLogId);
    }

    /**
     * Menghasilkan hash code berdasarkan ID log pengindeksan dan nomor halaman
     */
    @Override
    public int hashCode() {
        return Objects.hash(indexingLogId, pageNumber);
    }
}
