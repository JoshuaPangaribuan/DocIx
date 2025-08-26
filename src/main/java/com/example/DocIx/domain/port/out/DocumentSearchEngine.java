package com.example.DocIx.domain.port.out;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;

import java.util.List;

public interface DocumentSearchEngine {

    void indexDocument(Document document);
    void indexDocumentPage(PageExtractor.DocumentPage page);
    void deleteDocument(DocumentId documentId);
    List<SearchResult> search(String query, int page, int size);
    List<String> autocomplete(String query, int maxSuggestions);

    class SearchResult {
        private final DocumentId documentId;
        private final String fileName;
        private final String highlightedContent;
        private final double score;
        private String downloadUrl;
        private int pageNumber;

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

        public int getPageNumber() { return pageNumber; }
        public void setPageInfo(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public boolean isPaged() {
            return pageNumber > 0;
        }
    }
}
