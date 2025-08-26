package com.example.DocIx.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Kelas entitas untuk mencatat log proses pengindeksan dokumen
 * Melacak status dan kemajuan pengindeksan halaman-halaman dokumen
 */
public class IndexingLog {
    private Long id;
    private final String documentId;
    private int totalPages;
    private int pagesIndexed;
    private int pagesFailed;
    private IndexingStatus indexingStatus;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorDetails;
    private List<IndexingPageLog> pageLogs;

    /**
     * Konstruktor untuk membuat IndexingLog baru
     * @param documentId ID dokumen yang akan diindeks
     */
    public IndexingLog(String documentId) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID tidak boleh null");
        this.totalPages = 0;
        this.pagesIndexed = 0;
        this.pagesFailed = 0;
        this.indexingStatus = IndexingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.pageLogs = new ArrayList<>();
    }

    /**
     * Konstruktor lengkap untuk JPA/rekonstruksi dari database
     * @param id ID unik log pengindeksan
     * @param documentId ID dokumen yang diindeks
     * @param totalPages total jumlah halaman dokumen
     * @param pagesIndexed jumlah halaman yang berhasil diindeks
     * @param pagesFailed jumlah halaman yang gagal diindeks
     * @param indexingStatus status pengindeksan saat ini
     * @param createdAt waktu pembuatan log
     * @param updatedAt waktu terakhir diperbarui
     * @param errorDetails detail kesalahan jika ada
     */
    public IndexingLog(Long id, String documentId, int totalPages, int pagesIndexed,
                      int pagesFailed, IndexingStatus indexingStatus, LocalDateTime createdAt,
                      LocalDateTime updatedAt, String errorDetails) {
        this.id = id;
        this.documentId = documentId;
        this.totalPages = totalPages;
        this.pagesIndexed = pagesIndexed;
        this.pagesFailed = pagesFailed;
        this.indexingStatus = indexingStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.errorDetails = errorDetails;
        this.pageLogs = new ArrayList<>();
    }

    // Metode bisnis untuk mengelola proses pengindeksan

    /**
     * Menginisialisasi proses pengindeksan dengan jumlah halaman tertentu
     * @param totalPages total halaman yang akan diindeks
     */
    public void initializePages(int totalPages) {
        this.totalPages = totalPages;
        this.indexingStatus = IndexingStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();

        // Inisialisasi page logs
        this.pageLogs.clear();
        for (int i = 1; i <= totalPages; i++) {
            this.pageLogs.add(new IndexingPageLog(this.id, i));
        }
    }

    /**
     * Menandai halaman tertentu sebagai berhasil diindeks
     * @param pageNumber nomor halaman yang berhasil diindeks
     */
    public void markPageAsIndexed(int pageNumber) {
        IndexingPageLog pageLog = findPageLog(pageNumber);
        if (pageLog != null && pageLog.getPageStatus() != PageStatus.INDEXED) {
            pageLog.markAsIndexed();
            this.pagesIndexed++;
            updateOverallStatus();
        }
    }

    /**
     * Menandai halaman tertentu sebagai gagal diindeks
     * @param pageNumber nomor halaman yang gagal diindeks
     * @param errorMessage pesan kesalahan
     */
    public void markPageAsFailed(int pageNumber, String errorMessage) {
        IndexingPageLog pageLog = findPageLog(pageNumber);
        if (pageLog != null && pageLog.getPageStatus() != PageStatus.FAILED) {
            pageLog.markAsFailed(errorMessage);
            this.pagesFailed++;
            updateOverallStatus();
        }
    }

    /**
     * Mencari log halaman berdasarkan nomor halaman
     * @param pageNumber nomor halaman yang dicari
     * @return IndexingPageLog jika ditemukan, null jika tidak
     */
    private IndexingPageLog findPageLog(int pageNumber) {
        return pageLogs.stream()
                .filter(log -> log.getPageNumber() == pageNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * Memperbarui status keseluruhan berdasarkan status halaman individual
     */
    private void updateOverallStatus() {
        this.updatedAt = LocalDateTime.now();

        if (pagesIndexed + pagesFailed >= totalPages) {
            if (pagesFailed == 0) {
                this.indexingStatus = IndexingStatus.FULLY_INDEXED;
            } else {
                this.indexingStatus = IndexingStatus.PARTIALLY_INDEXED;
            }
        }
    }

    /**
     * Menandai seluruh proses pengindeksan sebagai gagal
     * @param errorDetails detail kesalahan yang menyebabkan kegagalan
     */
    public void markAsFailed(String errorDetails) {
        this.indexingStatus = IndexingStatus.FAILED;
        this.errorDetails = errorDetails;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Memeriksa apakah proses pengindeksan telah selesai
     * @return true jika pengindeksan selesai (berhasil, sebagian, atau gagal)
     */
    public boolean isCompleted() {
        return indexingStatus == IndexingStatus.FULLY_INDEXED ||
               indexingStatus == IndexingStatus.PARTIALLY_INDEXED ||
               indexingStatus == IndexingStatus.FAILED;
    }

    /**
     * Memeriksa apakah pengindeksan selesai dengan sempurna
     * @return true jika semua halaman berhasil diindeks
     */
    public boolean isFullyIndexed() {
        return indexingStatus == IndexingStatus.FULLY_INDEXED;
    }

    /**
     * Menghitung persentase kemajuan pengindeksan
     * @return nilai antara 0.0 dan 1.0 yang menunjukkan kemajuan
     */
    public double getIndexingProgress() {
        if (totalPages == 0) return 0.0;
        return (double) pagesIndexed / totalPages;
    }

    // Metode getter dan setter untuk akses properti

    /** Mendapatkan ID log pengindeksan */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** Mendapatkan ID dokumen */
    public String getDocumentId() { return documentId; }

    /** Mendapatkan total halaman */
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    /** Mendapatkan jumlah halaman yang berhasil diindeks */
    public int getPagesIndexed() { return pagesIndexed; }
    public void setPagesIndexed(int pagesIndexed) { this.pagesIndexed = pagesIndexed; }

    /** Mendapatkan jumlah halaman yang gagal diindeks */
    public int getPagesFailed() { return pagesFailed; }
    public void setPagesFailed(int pagesFailed) { this.pagesFailed = pagesFailed; }

    /** Mendapatkan status pengindeksan */
    public IndexingStatus getIndexingStatus() { return indexingStatus; }
    public void setIndexingStatus(IndexingStatus indexingStatus) { this.indexingStatus = indexingStatus; }

    /** Mendapatkan waktu pembuatan */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Mendapatkan waktu terakhir diperbarui */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Mendapatkan detail kesalahan */
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

    /** Mendapatkan daftar log halaman */
    public List<IndexingPageLog> getPageLogs() { return pageLogs; }
    public void setPageLogs(List<IndexingPageLog> pageLogs) { this.pageLogs = pageLogs; }

    /**
     * Membandingkan kesamaan berdasarkan ID dokumen
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexingLog that = (IndexingLog) o;
        return Objects.equals(documentId, that.documentId);
    }

    /**
     * Menghasilkan hash code berdasarkan ID dokumen
     */
    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }
}
