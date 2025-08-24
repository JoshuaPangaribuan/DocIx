package com.example.DocIx.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Kelas entitas untuk mencatat log proses pengindeksan dokumen
 * Melacak status dan kemajuan pengindeksan segmen-segmen dokumen
 */
public class IndexingLog {
    private Long id;
    private final String documentId;
    private int totalSegments;
    private int segmentsIndexed;
    private int segmentsFailed;
    private IndexingStatus indexingStatus;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorDetails;
    private List<IndexingSegmentLog> segmentLogs;

    /**
     * Konstruktor untuk membuat IndexingLog baru
     * @param documentId ID dokumen yang akan diindeks
     */
    public IndexingLog(String documentId) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID tidak boleh null");
        this.totalSegments = 0;
        this.segmentsIndexed = 0;
        this.segmentsFailed = 0;
        this.indexingStatus = IndexingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.segmentLogs = new ArrayList<>();
    }

    /**
     * Konstruktor lengkap untuk JPA/rekonstruksi dari database
     * @param id ID unik log pengindeksan
     * @param documentId ID dokumen yang diindeks
     * @param totalSegments total jumlah segmen dokumen
     * @param segmentsIndexed jumlah segmen yang berhasil diindeks
     * @param segmentsFailed jumlah segmen yang gagal diindeks
     * @param indexingStatus status pengindeksan saat ini
     * @param createdAt waktu pembuatan log
     * @param updatedAt waktu terakhir diperbarui
     * @param errorDetails detail kesalahan jika ada
     */
    public IndexingLog(Long id, String documentId, int totalSegments, int segmentsIndexed,
                      int segmentsFailed, IndexingStatus indexingStatus, LocalDateTime createdAt,
                      LocalDateTime updatedAt, String errorDetails) {
        this.id = id;
        this.documentId = documentId;
        this.totalSegments = totalSegments;
        this.segmentsIndexed = segmentsIndexed;
        this.segmentsFailed = segmentsFailed;
        this.indexingStatus = indexingStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.errorDetails = errorDetails;
        this.segmentLogs = new ArrayList<>();
    }

    // Metode bisnis untuk mengelola proses pengindeksan

    /**
     * Menginisialisasi proses pengindeksan dengan jumlah segmen tertentu
     * @param totalSegments total segmen yang akan diindeks
     */
    public void initializeSegments(int totalSegments) {
        this.totalSegments = totalSegments;
        this.indexingStatus = IndexingStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();

        // Inisialisasi segment logs
        this.segmentLogs.clear();
        for (int i = 1; i <= totalSegments; i++) {
            this.segmentLogs.add(new IndexingSegmentLog(this.id, i));
        }
    }

    /**
     * Menandai segmen tertentu sebagai berhasil diindeks
     * @param segmentNumber nomor segmen yang berhasil diindeks
     */
    public void markSegmentAsIndexed(int segmentNumber) {
        IndexingSegmentLog segmentLog = findSegmentLog(segmentNumber);
        if (segmentLog != null && segmentLog.getSegmentStatus() != SegmentStatus.INDEXED) {
            segmentLog.markAsIndexed();
            this.segmentsIndexed++;
            updateOverallStatus();
        }
    }

    /**
     * Menandai segmen tertentu sebagai gagal diindeks
     * @param segmentNumber nomor segmen yang gagal diindeks
     * @param errorMessage pesan kesalahan
     */
    public void markSegmentAsFailed(int segmentNumber, String errorMessage) {
        IndexingSegmentLog segmentLog = findSegmentLog(segmentNumber);
        if (segmentLog != null && segmentLog.getSegmentStatus() != SegmentStatus.FAILED) {
            segmentLog.markAsFailed(errorMessage);
            this.segmentsFailed++;
            updateOverallStatus();
        }
    }

    /**
     * Mencari log segmen berdasarkan nomor segmen
     * @param segmentNumber nomor segmen yang dicari
     * @return IndexingSegmentLog jika ditemukan, null jika tidak
     */
    private IndexingSegmentLog findSegmentLog(int segmentNumber) {
        return segmentLogs.stream()
                .filter(log -> log.getSegmentNumber() == segmentNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * Memperbarui status keseluruhan berdasarkan status segmen individual
     */
    private void updateOverallStatus() {
        this.updatedAt = LocalDateTime.now();

        if (segmentsIndexed + segmentsFailed >= totalSegments) {
            if (segmentsFailed == 0) {
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
     * @return true jika semua segmen berhasil diindeks
     */
    public boolean isFullyIndexed() {
        return indexingStatus == IndexingStatus.FULLY_INDEXED;
    }

    /**
     * Menghitung persentase kemajuan pengindeksan
     * @return nilai antara 0.0 dan 1.0 yang menunjukkan kemajuan
     */
    public double getIndexingProgress() {
        if (totalSegments == 0) return 0.0;
        return (double) segmentsIndexed / totalSegments;
    }

    // Metode getter dan setter untuk akses properti

    /** Mendapatkan ID log pengindeksan */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** Mendapatkan ID dokumen */
    public String getDocumentId() { return documentId; }

    /** Mendapatkan total segmen */
    public int getTotalSegments() { return totalSegments; }
    public void setTotalSegments(int totalSegments) { this.totalSegments = totalSegments; }

    /** Mendapatkan jumlah segmen yang berhasil diindeks */
    public int getSegmentsIndexed() { return segmentsIndexed; }
    public void setSegmentsIndexed(int segmentsIndexed) { this.segmentsIndexed = segmentsIndexed; }

    /** Mendapatkan jumlah segmen yang gagal diindeks */
    public int getSegmentsFailed() { return segmentsFailed; }
    public void setSegmentsFailed(int segmentsFailed) { this.segmentsFailed = segmentsFailed; }

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

    /** Mendapatkan daftar log segmen */
    public List<IndexingSegmentLog> getSegmentLogs() { return segmentLogs; }
    public void setSegmentLogs(List<IndexingSegmentLog> segmentLogs) { this.segmentLogs = segmentLogs; }

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
