package com.example.DocIx.domain.port.out;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.service.DocumentSegmentationService;

import java.util.List;

public interface DocumentSearchEngine {
    void indexDocument(Document document);
    void indexDocument(DocumentSegmentationService.DocumentSegment segment);
    void deleteDocument(DocumentId documentId);
    List<SearchResult> search(String query, int page, int size);
    List<String> autocomplete(String query, int maxSuggestions);

    class SearchResult {
        private final DocumentId documentId;
        private final String fileName;
        private final String highlightedContent;
        private final double score;
        private String downloadUrl;
        private int segmentNumber;
        private int totalSegments;

        public SearchResult(DocumentId documentId, String fileName, String highlightedContent, double score) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.highlightedContent = highlightedContent;
            this.score = score;
        }

        public DocumentId getDocumentId() { return documentId; }
        public String getFileName() { return fileName; }
        public String getHighlightedContent() { return highlightedContent; }
        public double getScore() { return score; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        public int getSegmentNumber() { return segmentNumber; }
        public int getTotalSegments() { return totalSegments; }
        public void setSegmentInfo(int segmentNumber, int totalSegments) {
            this.segmentNumber = segmentNumber;
            this.totalSegments = totalSegments;
        }

        public boolean isSegmented() {
            return totalSegments > 1;
        }
    }
}
