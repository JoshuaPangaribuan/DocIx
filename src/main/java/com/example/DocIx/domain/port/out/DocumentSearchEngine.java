package com.example.DocIx.domain.port.out;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;

import java.util.List;

public interface DocumentSearchEngine {
    void indexDocument(Document document);
    void deleteDocument(DocumentId documentId);
    SearchResults search(String query, int page, int size);
    List<String> autocomplete(String query, int maxSuggestions);

    class SearchResult {
        private final DocumentId documentId;
        private final String fileName;
        private final String highlightedContent;
        private final double score;

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
    }

    class SearchResults {
        private final List<SearchResult> results;
        private final long totalHits;

        public SearchResults(List<SearchResult> results, long totalHits) {
            this.results = results;
            this.totalHits = totalHits;
        }

        public List<SearchResult> getResults() { return results; }
        public long getTotalHits() { return totalHits; }
    }
}
