package com.example.DocIx.adapter.in.web;

import com.example.DocIx.adapter.in.web.mapper.DocumentWebMapper;
import com.example.DocIx.domain.port.in.UploadDocumentUseCase;
import com.example.DocIx.domain.port.in.SearchDocumentUseCase;
import com.example.DocIx.domain.port.in.AutocompleteUseCase;
import com.example.DocIx.domain.port.in.BulkUploadUseCase;
import com.example.DocIx.domain.port.in.DocumentIndexingUseCase;
import com.example.DocIx.domain.port.in.DownloadDocumentUseCase;
import com.example.DocIx.domain.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final SearchDocumentUseCase searchDocumentUseCase;
    private final AutocompleteUseCase autocompleteUseCase;
    private final DocumentWebMapper documentWebMapper;
    private final BulkUploadUseCase bulkUploadUseCase;
    private final DocumentIndexingUseCase documentIndexingUseCase;
    private final DownloadDocumentUseCase downloadDocumentUseCase;

    public DocumentController(UploadDocumentUseCase uploadDocumentUseCase,
            SearchDocumentUseCase searchDocumentUseCase,
            AutocompleteUseCase autocompleteUseCase,
            DocumentWebMapper documentWebMapper,
            BulkUploadUseCase bulkUploadUseCase,
            DocumentIndexingUseCase documentIndexingUseCase,
            DownloadDocumentUseCase downloadDocumentUseCase) {
        this.searchDocumentUseCase = searchDocumentUseCase;
        this.autocompleteUseCase = autocompleteUseCase;
        this.documentWebMapper = documentWebMapper;
        this.bulkUploadUseCase = bulkUploadUseCase;
        this.documentIndexingUseCase = documentIndexingUseCase;
        this.downloadDocumentUseCase = downloadDocumentUseCase;
    }

    /**
     * Upload single document dengan atomic operations (Menggunakan
     * BulkUploadService yang baru)
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("uploader") @NotNull @Size(min = 1, max = 100) String uploader) throws IOException {

        long startTime = System.currentTimeMillis();
        String safeUploader = LoggingUtil.maskSensitiveData(uploader);
        String safeFileName = LoggingUtil.safeFileName(file.getOriginalFilename());

        logger.info("Memulai upload dokumen - File: {}, Size: {} bytes, Uploader: {}",
                safeFileName, file.getSize(), safeUploader);

        try {
            // Validasi basic file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, null, "File tidak boleh kosong", null));
            }

            // Gunakan BulkUploadService yang sudah robust dengan atomic operations
            BulkUploadUseCase.BulkUploadCommand command = new BulkUploadUseCase.BulkUploadCommand(
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getSize(),
                    file.getContentType(),
                    uploader);

            BulkUploadUseCase.BulkUploadResult result = bulkUploadUseCase.uploadDocument(command);

            long duration = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                logger.info("Upload dokumen berhasil - DocumentId: {}, Duration: {}ms",
                        result.getDocumentId(), duration);

                LoggingUtil.logApiAccess("POST", "/api/documents/upload", safeUploader,
                        duration, 200, "File: " + safeFileName);

                return ResponseEntity.ok(new UploadResponse(
                        true,
                        result.getDocumentId(),
                        result.getMessage(),
                        null));
            } else {
                logger.warn("Upload dokumen gagal - File: {}, Error: {}",
                        safeFileName, result.getErrorMessage());

                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, null, null, result.getErrorMessage()));
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Upload dokumen gagal - File: {}, Uploader: {}, Error: {}",
                    safeFileName, safeUploader, e.getMessage(), e);

            LoggingUtil.logApiError("POST", "/api/documents/upload", safeUploader,
                    duration, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(false, null, null, "Terjadi kesalahan saat upload"));
        }
    }

    /**
     * Upload multiple documents sekaligus dengan atomic operations
     */
    @PostMapping("/upload/bulk")
    public ResponseEntity<BulkUploadResponse> uploadDocuments(
            @RequestParam("files") @NotNull MultipartFile[] files,
            @RequestParam("uploader") @NotNull @Size(min = 1, max = 100) String uploader) throws IOException {

        long startTime = System.currentTimeMillis();
        String safeUploader = LoggingUtil.maskSensitiveData(uploader);

        logger.info("Memulai bulk upload dokumen - Files count: {}, Uploader: {}",
                files.length, safeUploader);

        try {
            List<BulkUploadUseCase.BulkUploadCommand> commands = new ArrayList<>();

            // Prepare commands
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    commands.add(new BulkUploadUseCase.BulkUploadCommand(
                            file.getOriginalFilename(),
                            file.getBytes(),
                            file.getSize(),
                            file.getContentType(),
                            uploader));
                }
            }

            // Process bulk upload dengan atomic operations
            List<BulkUploadUseCase.BulkUploadResult> results = bulkUploadUseCase.uploadMultipleDocuments(commands);

            // Convert results
            BulkUploadResponse response = new BulkUploadResponse();
            int successCount = 0;
            int errorCount = 0;

            for (int i = 0; i < results.size(); i++) {
                BulkUploadUseCase.BulkUploadResult result = results.get(i);
                String fileName = commands.get(i).getOriginalFileName();

                if (result.isSuccess()) {
                    response.addSuccess(new BulkUploadResponse.UploadResult(
                            fileName, result.getDocumentId(), result.getMessage()));
                    successCount++;
                } else {
                    response.addError(new BulkUploadResponse.UploadError(
                            fileName, result.getErrorMessage()));
                    errorCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Bulk upload selesai - Success: {}, Errors: {}, Duration: {}ms",
                    successCount, errorCount, duration);

            LoggingUtil.logApiAccess("POST", "/api/documents/upload/bulk", safeUploader,
                    duration, 200, String.format("Files: %d, Success: %d, Errors: %d",
                            files.length, successCount, errorCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Bulk upload gagal", e);

            BulkUploadResponse errorResponse = new BulkUploadResponse();
            errorResponse.addError(new BulkUploadResponse.UploadError(
                    "bulk_upload", "Terjadi kesalahan tidak terduga saat bulk upload"));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cek status indexing untuk document tertentu
     */
    @GetMapping("/{documentId}/indexing-status")
    public ResponseEntity<DocumentIndexingUseCase.IndexingStatusResponse> getIndexingStatus(
            @PathVariable String documentId) {

        try {
            DocumentIndexingUseCase.IndexingStatusResponse status = documentIndexingUseCase
                    .getIndexingStatus(documentId);

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error saat mengambil status indexing untuk document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Trigger reindex untuk dokumen tertentu
     */
    @PostMapping("/{documentId}/reindex")
    public ResponseEntity<Map<String, String>> reindexDocument(@PathVariable String documentId) {
        try {
            logger.info("Memulai reindex untuk document: {}", documentId);
            documentIndexingUseCase.processDocumentIndexing(documentId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Reindex dokumen berhasil dimulai"));
        } catch (Exception e) {
            logger.error("Error saat reindex document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Gagal memulai reindex dokumen: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchDocuments(
            @RequestParam("q") @NotNull @Size(min = 2, max = 200) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        long startTime = System.currentTimeMillis();
        String safeQuery = LoggingUtil.maskSensitiveData(query);

        logger.info("Starting document search - Query: '{}', Page: {}, Size: {}",
                safeQuery, page, size);

        try {
            SearchDocumentUseCase.SearchQuery searchQuery = new SearchDocumentUseCase.SearchQuery(query, page, size);
            SearchDocumentUseCase.SearchResponse result = searchDocumentUseCase.searchDocuments(searchQuery);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Document search completed - Query: '{}', Results: {}, Duration: {}ms",
                    safeQuery, result.getTotalHits(), duration);

            LoggingUtil.logApiAccess("GET", "/api/documents/search", "anonymous",
                    duration, 200, String.format("Query: '%s', Results: %d",
                            safeQuery, result.getTotalHits()));

            return ResponseEntity.ok(documentWebMapper.toSearchResponse(result));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Document search failed - Query: '{}', Error: {}",
                    safeQuery, e.getMessage(), e);

            LoggingUtil.logApiError("GET", "/api/documents/search", "anonymous",
                    duration, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<AutocompleteResponse> getAutocompleteSuggestions(
            @RequestParam("q") @NotNull @Size(min = 1, max = 100) String query,
            @RequestParam(value = "max", defaultValue = "5") int maxSuggestions) {

        long startTime = System.currentTimeMillis();
        String safeQuery = LoggingUtil.maskSensitiveData(query);

        logger.debug("Starting autocomplete request - Query: '{}', Max: {}",
                safeQuery, maxSuggestions);

        try {
            AutocompleteUseCase.AutocompleteQuery autocompleteQuery = new AutocompleteUseCase.AutocompleteQuery(query,
                    maxSuggestions);

            List<String> suggestions = autocompleteUseCase.getAutocompleteSuggestions(autocompleteQuery);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Autocomplete completed - Query: '{}', Suggestions: {}, Duration: {}ms",
                    safeQuery, suggestions.size(), duration);

            LoggingUtil.logApiAccess("GET", "/api/documents/autocomplete", "anonymous",
                    duration, 200, String.format("Query: '%s', Suggestions: %d",
                            safeQuery, suggestions.size()));

            return ResponseEntity.ok(documentWebMapper.toAutocompleteResponse(suggestions));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Autocomplete failed - Query: '{}', Error: {}",
                    safeQuery, e.getMessage(), e);

            LoggingUtil.logApiError("GET", "/api/documents/autocomplete", "anonymous",
                    duration, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable("documentId") String documentId) {

        long startTime = System.currentTimeMillis();

        logger.info("Starting document download - DocumentId: {}", documentId);

        try {
            var resultOpt = downloadDocumentUseCase.downloadByDocumentId(documentId);
            if (resultOpt.isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                logger.warn("Document download failed - DocumentId: {} not found or not processed", documentId);

                LoggingUtil.logApiAccess("GET", "/api/documents/download/" + documentId, "anonymous",
                        duration, 404, "Document not found or not processed");
                return ResponseEntity.notFound().build();
            }

            DownloadDocumentUseCase.DownloadResult result = resultOpt.get();
            String safeFileName = LoggingUtil.safeFileName(result.getOriginalFileName());
            InputStreamResource resource = new InputStreamResource(result.getInputStream());

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Document download completed - DocumentId: {}, File: {}, Duration: {}ms",
                    documentId, safeFileName, duration);

            LoggingUtil.logApiAccess("GET", "/api/documents/download/" + documentId, "anonymous",
                    duration, 200, "File: " + safeFileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + result.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .body(resource);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Document download failed - DocumentId: {}, Error: {}",
                    documentId, e.getMessage(), e);

            LoggingUtil.logApiError("GET", "/api/documents/download/" + documentId, "anonymous",
                    duration, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Debug endpoint dihapus untuk menjaga batasan layer web terhadap domain ports saja

    // Response DTOs
    public static class UploadResponse {
        private boolean success;
        private String documentId;
        private String message;
        private String error;

        public UploadResponse(boolean success, String documentId, String message, String error) {
            this.success = success;
            this.documentId = documentId;
            this.message = message;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getMessage() {
            return message;
        }

        public String getError() {
            return error;
        }
    }

    public static class BulkUploadResponse {
        private List<UploadResult> successful = new java.util.ArrayList<>();
        private List<UploadError> failed = new java.util.ArrayList<>();

        public void addSuccess(UploadResult result) {
            successful.add(result);
        }

        public void addError(UploadError error) {
            failed.add(error);
        }

        // Keep backward compatibility methods
        public void addSuccess(String fileName, String documentId, String message) {
            successful.add(new UploadResult(fileName, documentId, message));
        }

        public void addError(String fileName, String error) {
            failed.add(new UploadError(fileName, error));
        }

        public List<UploadResult> getSuccessful() {
            return successful;
        }

        public List<UploadError> getFailed() {
            return failed;
        }

        public int getTotalSuccessful() {
            return successful.size();
        }

        public int getTotalFailed() {
            return failed.size();
        }

        public static class UploadResult {
            private String fileName;
            private String documentId;
            private String message;

            public UploadResult(String fileName, String documentId, String message) {
                this.fileName = fileName;
                this.documentId = documentId;
                this.message = message;
            }

            public String getFileName() {
                return fileName;
            }

            public String getDocumentId() {
                return documentId;
            }

            public String getMessage() {
                return message;
            }
        }

        public static class UploadError {
            private String fileName;
            private String error;

            public UploadError(String fileName, String error) {
                this.fileName = fileName;
                this.error = error;
            }

            public String getFileName() {
                return fileName;
            }

            public String getError() {
                return error;
            }
        }
    }

    public static class SearchResponse {
        private List<SearchResultDto> results;
        private long totalHits;
        private int page;
        private int size;
        private boolean hasNext;
        private boolean hasPrevious;

        public SearchResponse(List<SearchResultDto> results, long totalHits, int page, int size, boolean hasNext,
                boolean hasPrevious) {
            this.results = results;
            this.totalHits = totalHits;
            this.page = page;
            this.size = size;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        public List<SearchResultDto> getResults() {
            return results;
        }

        public long getTotalHits() {
            return totalHits;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }
    }

    public static class SearchResultDto {
        private String documentId;
        private String fileName;
        private String highlightedContent;
        private double score;
        private String downloadUrl;

        public SearchResultDto(String documentId, String fileName, String highlightedContent, double score) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.highlightedContent = highlightedContent;
            this.score = score;
            this.downloadUrl = "/api/documents/download/" + documentId;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getFileName() {
            return fileName;
        }

        public String getHighlightedContent() {
            return highlightedContent;
        }

        public double getScore() {
            return score;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }
    }

    public static class AutocompleteResponse {
        private List<String> suggestions;

        public AutocompleteResponse(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }
    }
}
