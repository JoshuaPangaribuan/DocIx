package com.example.DocIx.adapter.in.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DocIx.domain.port.in.AdminIndexingUseCase;
import com.example.DocIx.adapter.in.web.mapper.AdminWebMapper;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminIndexingUseCase adminIndexingUseCase;
    private final AdminWebMapper adminWebMapper;

    public AdminController(AdminIndexingUseCase adminIndexingUseCase, AdminWebMapper adminWebMapper) {
        this.adminIndexingUseCase = adminIndexingUseCase;
        this.adminWebMapper = adminWebMapper;
    }

    /**
     * Mendapatkan ringkasan status indexing untuk semua dokumen
     */
    @GetMapping("/indexing/summary")
    public ResponseEntity<AdminIndexingUseCase.IndexingSummaryResponse> getIndexingSummary() {
        try {
            logger.debug("Mengambil ringkasan status indexing");

            var summary = adminIndexingUseCase.getIndexingSummary();
            return ResponseEntity.ok(adminWebMapper.toWeb(summary));
        } catch (Exception e) {
            logger.error("Error saat mengambil ringkasan indexing", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Trigger manual retry untuk indexing yang gagal
     */
    @PostMapping("/indexing/retry-failed")
    public ResponseEntity<Map<String, String>> retryFailedIndexing() {
        try {
            logger.info("Memulai manual retry untuk indexing yang gagal");
            adminIndexingUseCase.retryFailedIndexing();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Manual retry indexing berhasil dimulai"));
        } catch (Exception e) {
            logger.error("Error saat manual retry failed indexing", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai manual retry indexing: " + e.getMessage()));
        }
    }

    /**
     * Mengecek status konsistensi data antara database dan Elasticsearch
     */
    @GetMapping("/indexing/consistency-check")
    public ResponseEntity<AdminIndexingUseCase.IndexingConsistencyResponse> checkIndexingConsistency() {
        logger.info("Memulai pengecekan konsistensi indexing antara database dan Elasticsearch");

        try {
            // Ambil semua dokumen yang sudah diproses dari database
            var response = adminIndexingUseCase.checkIndexingConsistency();
            logger.info("Konsistensi indexing: {}/{} dokumen konsisten ({}%)",
                    response.getIndexedDocuments(), response.getTotalProcessedDocuments(), response.getConsistencyPercentage());
            return ResponseEntity.ok(adminWebMapper.toWeb(response));

        } catch (Exception e) {
            logger.error("Error saat mengecek konsistensi indexing", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reindex semua dokumen yang hilang dari Elasticsearch
     */
    @PostMapping("/indexing/reindex-missing")
    public ResponseEntity<Map<String, Object>> reindexMissingDocuments() {
        try {
            logger.info("Memulai reindex untuk dokumen yang hilang");

            int reindexedCount = adminIndexingUseCase.reindexMissingDocuments();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Reindex dokumen yang hilang berhasil dimulai",
                    "reindexedCount", reindexedCount));
        } catch (Exception e) {
            logger.error("Error saat reindex missing documents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai reindex: " + e.getMessage()));
        }
    }

    /**
     * Reindex semua dokumen
     */
    @PostMapping("/indexing/reindex-all")
    public ResponseEntity<Map<String, String>> reindexAllDocuments() {
        try {
            logger.info("Memulai reindex untuk semua dokumen");
            adminIndexingUseCase.reindexAllDocuments();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Reindex semua dokumen berhasil dimulai"));
        } catch (Exception e) {
            logger.error("Error saat reindex all documents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai reindex semua dokumen: " + e.getMessage()));
        }
    }

    // Response classes dipindahkan ke AdminIndexingUseCase
}
