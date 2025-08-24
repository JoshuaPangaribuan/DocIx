package com.example.DocIx.domain.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Kelas utilitas untuk logging yang secara otomatis menyembunyikan informasi sensitif
 * Menyediakan metode untuk logging yang aman dengan masking data pribadi
 */
public class LoggingUtil {

    private static final Logger API_ACCESS_LOGGER = LoggerFactory.getLogger("API_ACCESS");

    // Pattern untuk deteksi data sensitif
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");

    /**
     * Menyembunyikan informasi sensitif dalam pesan log
     * @param message pesan yang akan di-masking
     * @return pesan dengan informasi sensitif yang disembunyikan
     */
    public static String maskSensitiveData(String message) {
        if (message == null) {
            return null;
        }

        String masked = message;

        // Sembunyikan alamat email (simpan 2 karakter pertama dan domain)
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(matchResult -> {
            String email = matchResult.group();
            String[] parts = email.split("@");
            if (parts.length == 2) {
                String localPart = parts[0];
                String domain = parts[1];
                String maskedLocal = localPart.length() > 2 ?
                    localPart.substring(0, 2) + "***" : "***";
                return maskedLocal + "@" + domain;
            }
            return "***@***";
        });

        // Sembunyikan nomor telepon
        masked = PHONE_PATTERN.matcher(masked).replaceAll("***-***-****");

        // Sembunyikan alamat IP (simpan oktet pertama)
        masked = IP_PATTERN.matcher(masked).replaceAll(matchResult -> {
            String ip = matchResult.group();
            String[] octets = ip.split("\\.");
            return octets[0] + ".***.***.***";
        });

        return masked;
    }

    /**
     * Menyembunyikan ringkasan konten file untuk logging (100 karakter pertama)
     * @param content konten file yang akan di-masking
     * @return konten yang dipotong dan di-masking
     */
    public static String maskFileContent(String content) {
        if (content == null) {
            return null;
        }

        if (content.length() <= 100) {
            return maskSensitiveData(content);
        }

        return maskSensitiveData(content.substring(0, 100)) + "... [CONTENT_TRUNCATED]";
    }

    /**
     * Logging nama file yang aman (hapus informasi jalur)
     * @param fileName nama file yang akan dibuat aman
     * @return nama file tanpa jalur dan dengan data sensitif yang disembunyikan
     */
    public static String safeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        // Ekstrak hanya nama file tanpa jalur
        String safeName = fileName.replaceAll(".*[/\\\\]", "");
        return maskSensitiveData(safeName);
    }

    /**
     * Log akses API dengan format standar
     * @param method metode HTTP yang digunakan
     * @param endpoint endpoint API yang diakses
     * @param userId ID pengguna yang mengakses
     * @param durationMs durasi pemrosesan dalam milidetik
     * @param statusCode kode status HTTP
     * @param additionalInfo informasi tambahan
     */
    public static void logApiAccess(String method, String endpoint, String userId,
                                   long durationMs, int statusCode, String additionalInfo) {
        String safeUserId = maskSensitiveData(userId);
        String safeAdditionalInfo = maskSensitiveData(additionalInfo);

        API_ACCESS_LOGGER.info("API_ACCESS | {} {} | User: {} | {}ms | Status: {} | Info: {}",
            method, endpoint, safeUserId, durationMs, statusCode, safeAdditionalInfo);
    }

    /**
     * Log kesalahan akses API
     * @param method metode HTTP yang digunakan
     * @param endpoint endpoint API yang diakses
     * @param userId ID pengguna yang mengakses
     * @param durationMs durasi pemrosesan dalam milidetik
     * @param error pesan kesalahan
     */
    public static void logApiError(String method, String endpoint, String userId,
                                  long durationMs, String error) {
        String safeUserId = maskSensitiveData(userId);
        String safeError = maskSensitiveData(error);

        API_ACCESS_LOGGER.error("API_ERROR | {} {} | User: {} | {}ms | Error: {}",
            method, endpoint, safeUserId, durationMs, safeError);
    }

    /**
     * Log metrik performa
     * @param operation nama operasi yang dijalankan
     * @param documentId ID dokumen yang diproses
     */
    public static void logPerformance(String operation, String documentId) {
        API_ACCESS_LOGGER.info("PERFORMANCE: {} - Document: {}", operation, maskSensitiveData(documentId));
    }

    /**
     * Log kesalahan dengan masking data sensitif
     * @param operation nama operasi yang mengalami kesalahan
     * @param documentId ID dokumen yang terkait
     * @param e exception yang terjadi
     */
    public static void logError(String operation, String documentId, Exception e) {
        API_ACCESS_LOGGER.error("ERROR: {} - Document: {} - Error: {}",
                               operation, maskSensitiveData(documentId), e.getMessage(), e);
    }
}
