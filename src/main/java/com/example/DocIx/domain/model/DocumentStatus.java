package com.example.DocIx.domain.model;

/**
 * Enum yang mendefinisikan status pemrosesan dokumen dalam sistem
 * Menunjukkan tahapan siklus hidup dokumen dari unggah hingga diproses
 */
public enum DocumentStatus {
    /** Dokumen telah diunggah tetapi belum diproses */
    UPLOADED,
    /** Dokumen sedang dalam proses pemrosesan (ekstraksi konten) */
    PROCESSING,
    /** Dokumen berhasil diproses dan telah diindeks */
    PROCESSED,
    /** Pemrosesan dokumen gagal */
    FAILED
}
