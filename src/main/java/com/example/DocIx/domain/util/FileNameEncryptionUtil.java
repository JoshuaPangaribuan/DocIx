package com.example.DocIx.domain.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Kelas utilitas untuk mengenkripsi nama file guna memastikan penyimpanan yang aman
 * sambil mempertahankan kemampuan untuk mengambil nama file asli untuk unduhan
 */
public class FileNameEncryptionUtil {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Menghasilkan nama file terenkripsi berdasarkan nama file asli,
     * timestamp, dan ID dokumen untuk keamanan dan keunikan
     *
     * @param originalFileName nama file asli
     * @param documentId ID dokumen untuk keunikan tambahan
     * @return nama file terenkripsi dengan ekstensi yang dipertahankan
     */
    public static String generateEncryptedFileName(String originalFileName, String documentId) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Original file name cannot be null or empty");
        }

        try {
            // Ekstrak ekstensi dari file asli
            String extension = getFileExtension(originalFileName);
            String baseNameWithoutExtension = getBaseNameWithoutExtension(originalFileName);

            // Buat konten untuk di-hash: nama file asli + timestamp + ID dokumen + UUID acak
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String contentToHash = baseNameWithoutExtension + "_" + timestamp + "_" + documentId + "_" + UUID.randomUUID().toString();

            // Generate hash SHA-256
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(contentToHash.getBytes(StandardCharsets.UTF_8));

            // Konversi ke Base64 dan buat aman untuk filesystem
            String hashedName = Base64.getEncoder()
                .encodeToString(hashBytes)
                .replaceAll("[/+=]", "") // Hapus karakter yang mungkin menyebabkan masalah di nama file
                .substring(0, 32); // Batasi panjang untuk kepraktisan

            // Tambahkan prefix untuk identifikasi sebagai terenkripsi dan tambahkan ekstensi
            return "enc_" + hashedName + extension;

        } catch (NoSuchAlgorithmException e) {
            // Fallback ke penamaan berbasis UUID jika SHA-256 tidak tersedia
            String fallbackName = "enc_" + UUID.randomUUID().toString().replaceAll("-", "");
            return fallbackName + getFileExtension(originalFileName);
        }
    }

    /**
     * Menghasilkan nama file terenkripsi sederhana menggunakan UUID untuk kasus dimana
     * keamanan tinggi diperlukan dan pola nama file asli harus sepenuhnya disembunyikan
     *
     * @param originalFileName nama file asli
     * @return nama file terenkripsi berbasis UUID dengan ekstensi yang dipertahankan
     */
    public static String generateSimpleEncryptedFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Original file name cannot be null or empty");
        }

        String extension = getFileExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");

        return "docix_" + timestamp + "_" + uuid + extension;
    }

    /**
     * Mengekstrak ekstensi file dari nama file (termasuk titik)
     *
     * @param fileName nama file
     * @return ekstensi file termasuk titik, atau string kosong jika tidak ada ekstensi
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 && lastDotIndex < fileName.length() - 1
            ? fileName.substring(lastDotIndex)
            : "";
    }

    /**
     * Mengekstrak nama dasar tanpa ekstensi dari nama file
     *
     * @param fileName nama file
     * @return nama dasar tanpa ekstensi
     */
    private static String getBaseNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    /**
     * Memvalidasi apakah nama file tampak terenkripsi oleh utilitas ini
     *
     * @param fileName nama file yang akan diperiksa
     * @return true jika nama file tampak terenkripsi
     */
    public static boolean isEncryptedFileName(String fileName) {
        return fileName != null && (fileName.startsWith("enc_") || fileName.startsWith("docix_"));
    }
}
