package com.example.DocIx.domain.service;

import com.example.DocIx.domain.port.in.SearchDocumentUseCase;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchDocumentService implements SearchDocumentUseCase {

    private final DocumentSearchEngine searchEngine;

    public SearchDocumentService(DocumentSearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    @Override
    public SearchResponse searchDocuments(SearchQuery query) {
        validateSearchQuery(query);

        SearchResults searchResults = searchEngine.search(
            query.getQuery(),
            query.getPage(),
            query.getSize()
        );

        return new SearchResponse(
            searchResults.getResults(),
            searchResults.getTotalHits(),
            query.getPage(),
            query.getSize()
        );
    }

    private void validateSearchQuery(SearchQuery query) {
        if (query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        if (query.getQuery().trim().length() < 2) {
            throw new IllegalArgumentException("Search query must be at least 2 characters long");
        }
    }
}
