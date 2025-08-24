package com.example.DocIx.domain.model;

/**
 * Enum yang mendefinisikan status pengindeksan untuk segmen individual dokumen
 * Melacak status setiap bagian kecil dari dokumen yang dipecah untuk pengindeksan
 */
public enum SegmentStatus {
    /** Segmen belum diproses, masih menunggu pengindeksan */
    PENDING,
    /** Segmen berhasil diindeks ke search engine */
    INDEXED,
    /** Pengindeksan segmen gagal */
    FAILED
}
