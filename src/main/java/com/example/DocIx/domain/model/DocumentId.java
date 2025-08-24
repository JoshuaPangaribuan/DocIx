package com.example.DocIx.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object yang merepresentasikan ID unik untuk dokumen
 * Menggunakan UUID untuk memastikan keunikan ID di seluruh sistem
 */
public class DocumentId {
    private final String value;

    /**
     * Konstruktor untuk membuat DocumentId dengan nilai tertentu
     * @param value nilai string untuk ID dokumen
     * @throws IllegalArgumentException jika value null atau kosong
     */
    public DocumentId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("DocumentId cannot be null or empty");
        }
        this.value = value;
    }

    /**
     * Menghasilkan DocumentId baru secara otomatis menggunakan UUID
     * @return instance DocumentId baru dengan UUID unik
     */
    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID().toString());
    }

    /**
     * Membuat DocumentId dari string yang sudah ada
     * @param value nilai string untuk DocumentId
     * @return instance DocumentId dari nilai yang diberikan
     */
    public static DocumentId of(String value) {
        return new DocumentId(value);
    }

    /**
     * Mendapatkan nilai string dari DocumentId
     * @return nilai string ID dokumen
     */
    public String getValue() {
        return value;
    }

    /**
     * Membandingkan kesamaan dengan DocumentId lain berdasarkan nilai
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentId that = (DocumentId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * Menghasilkan hash code berdasarkan nilai DocumentId
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Mengkonversi DocumentId menjadi string
     * @return nilai string dari DocumentId
     */
    @Override
    public String toString() {
        return value;
    }
}
