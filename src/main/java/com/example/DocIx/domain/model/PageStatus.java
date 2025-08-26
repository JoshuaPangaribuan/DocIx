package com.example.DocIx.domain.model;

/**
 * Enum yang mendefinisikan status pengindeksan untuk halaman individual dokumen
 * Melacak status setiap halaman dari dokumen yang diindeks
 */
public enum PageStatus {
    /** Halaman belum diproses, masih menunggu pengindeksan */
    PENDING,
    /** Halaman berhasil diindeks ke search engine */
    INDEXED,
    /** Pengindeksan halaman gagal */
    FAILED
}
