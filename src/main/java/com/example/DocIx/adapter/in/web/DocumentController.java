package com.example.DocIx.adapter.in.web;

import com.example.DocIx.domain.port.in.UploadDocumentUseCase;
import com.example.DocIx.domain.port.in.SearchDocumentUseCase;
import com.example.DocIx.domain.port.in.AutocompleteUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final SearchDocumentUseCase searchDocumentUseCase;
    private final AutocompleteUseCase autocompleteUseCase;

    public DocumentController(UploadDocumentUseCase uploadDocumentUseCase,
                            SearchDocumentUseCase searchDocumentUseCase,
                            AutocompleteUseCase autocompleteUseCase) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.searchDocumentUseCase = searchDocumentUseCase;
        this.autocompleteUseCase = autocompleteUseCase;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("uploader") @NotNull @Size(min = 1, max = 100) String uploader) throws IOException {

        UploadDocumentUseCase.UploadCommand command = new UploadDocumentUseCase.UploadCommand(
            file.getOriginalFilename(),
            file.getInputStream(),
            file.getSize(),
            file.getContentType(),
            uploader
        );

        UploadDocumentUseCase.UploadResult result = uploadDocumentUseCase.uploadDocument(command);

        return ResponseEntity.ok(new UploadResponse(
            result.getDocumentId().getValue(),
            result.getMessage()
        ));
    }

    @PostMapping("/upload/bulk")
    public ResponseEntity<BulkUploadResponse> uploadDocuments(
            @RequestParam("files") @NotNull MultipartFile[] files,
            @RequestParam("uploader") @NotNull @Size(min = 1, max = 100) String uploader) throws IOException {

        BulkUploadResponse response = new BulkUploadResponse();

        for (MultipartFile file : files) {
            try {
                UploadDocumentUseCase.UploadCommand command = new UploadDocumentUseCase.UploadCommand(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    uploader
                );

                UploadDocumentUseCase.UploadResult result = uploadDocumentUseCase.uploadDocument(command);
                response.addSuccess(file.getOriginalFilename(), result.getDocumentId().getValue(), result.getMessage());

            } catch (Exception e) {
                response.addError(file.getOriginalFilename(), e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchDocuments(
            @RequestParam("q") @NotNull @Size(min = 2, max = 200) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        SearchDocumentUseCase.SearchQuery searchQuery = new SearchDocumentUseCase.SearchQuery(query, page, size);
        SearchDocumentUseCase.SearchResponse result = searchDocumentUseCase.searchDocuments(searchQuery);

        return ResponseEntity.ok(new SearchResponse(
            result.getResults().stream()
                .map(r -> new SearchResultDto(
                    r.getDocumentId().getValue(),
                    r.getFileName(),
                    r.getHighlightedContent(),
                    r.getScore()
                ))
                .toList(),
            result.getTotalHits(),
            result.getPage(),
            result.getSize(),
            result.hasNext(),
            result.hasPrevious()
        ));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<AutocompleteResponse> getAutocompleteSuggestions(
            @RequestParam("q") @NotNull @Size(min = 1, max = 100) String query,
            @RequestParam(value = "max", defaultValue = "5") int maxSuggestions) {

        AutocompleteUseCase.AutocompleteQuery autocompleteQuery =
            new AutocompleteUseCase.AutocompleteQuery(query, maxSuggestions);

        List<String> suggestions = autocompleteUseCase.getAutocompleteSuggestions(autocompleteQuery);

        return ResponseEntity.ok(new AutocompleteResponse(suggestions));
    }

    // Response DTOs
    public static class UploadResponse {
        private String documentId;
        private String message;

        public UploadResponse(String documentId, String message) {
            this.documentId = documentId;
            this.message = message;
        }

        public String getDocumentId() { return documentId; }
        public String getMessage() { return message; }
    }

    public static class BulkUploadResponse {
        private List<UploadResult> successful = new java.util.ArrayList<>();
        private List<UploadError> failed = new java.util.ArrayList<>();

        public void addSuccess(String fileName, String documentId, String message) {
            successful.add(new UploadResult(fileName, documentId, message));
        }

        public void addError(String fileName, String error) {
            failed.add(new UploadError(fileName, error));
        }

        public List<UploadResult> getSuccessful() { return successful; }
        public List<UploadError> getFailed() { return failed; }
        public int getTotalSuccessful() { return successful.size(); }
        public int getTotalFailed() { return failed.size(); }

        public static class UploadResult {
            private String fileName;
            private String documentId;
            private String message;

            public UploadResult(String fileName, String documentId, String message) {
                this.fileName = fileName;
                this.documentId = documentId;
                this.message = message;
            }

            public String getFileName() { return fileName; }
            public String getDocumentId() { return documentId; }
            public String getMessage() { return message; }
        }

        public static class UploadError {
            private String fileName;
            private String error;

            public UploadError(String fileName, String error) {
                this.fileName = fileName;
                this.error = error;
            }

            public String getFileName() { return fileName; }
            public String getError() { return error; }
        }
    }

    public static class SearchResponse {
        private List<SearchResultDto> results;
        private long totalHits;
        private int page;
        private int size;
        private boolean hasNext;
        private boolean hasPrevious;

        public SearchResponse(List<SearchResultDto> results, long totalHits, int page, int size, boolean hasNext, boolean hasPrevious) {
            this.results = results;
            this.totalHits = totalHits;
            this.page = page;
            this.size = size;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        public List<SearchResultDto> getResults() { return results; }
        public long getTotalHits() { return totalHits; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public boolean isHasNext() { return hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
    }

    public static class SearchResultDto {
        private String documentId;
        private String fileName;
        private String highlightedContent;
        private double score;

        public SearchResultDto(String documentId, String fileName, String highlightedContent, double score) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.highlightedContent = highlightedContent;
            this.score = score;
        }

        public String getDocumentId() { return documentId; }
        public String getFileName() { return fileName; }
        public String getHighlightedContent() { return highlightedContent; }
        public double getScore() { return score; }
    }

    public static class AutocompleteResponse {
        private List<String> suggestions;

        public AutocompleteResponse(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public List<String> getSuggestions() { return suggestions; }
    }
}
