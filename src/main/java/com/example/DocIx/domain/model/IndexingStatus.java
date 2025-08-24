package com.example.DocIx.domain.model;

/**
 * Enum yang mendefinisikan status proses pengindeksan dokumen
 * Menunjukkan tahapan dan hasil dari proses pengindeksan segmen dokumen
 */
public enum IndexingStatus {
    /** Pengindeksan belum dimulai, masih menunggu */
    PENDING,
    /** Pengindeksan sedang berlangsung */
    IN_PROGRESS,
    /** Semua segmen berhasil diindeks */
    FULLY_INDEXED,
    /** Sebagian segmen berhasil diindeks, sebagian gagal */
    PARTIALLY_INDEXED,
    /** Proses pengindeksan gagal sepenuhnya */
    FAILED
}
