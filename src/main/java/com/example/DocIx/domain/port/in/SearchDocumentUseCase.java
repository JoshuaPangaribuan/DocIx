package com.example.DocIx.domain.port.in;

import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;

import java.util.List;

public interface SearchDocumentUseCase {

    SearchResponse searchDocuments(SearchQuery query);

    class SearchQuery {
        private final String query;
        private final int page;
        private final int size;

        public SearchQuery(String query, int page, int size) {
            this.query = query;
            this.page = Math.max(0, page);
            this.size = Math.min(Math.max(1, size), 100); // Max 100 results per page
        }

        public String getQuery() { return query; }
        public int getPage() { return page; }
        public int getSize() { return size; }
    }

    class SearchResponse {
        private final List<SearchResult> results;
        private final long totalHits;
        private final int page;
        private final int size;

        public SearchResponse(List<SearchResult> results, long totalHits, int page, int size) {
            this.results = results;
            this.totalHits = totalHits;
            this.page = page;
            this.size = size;
        }

        public List<SearchResult> getResults() { return results; }
        public long getTotalHits() { return totalHits; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public boolean hasNext() { return (page + 1) * size < totalHits; }
        public boolean hasPrevious() { return page > 0; }
    }
}
