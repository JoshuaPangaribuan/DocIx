package com.example.DocIx.adapter.out.search;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResults;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Component
public class ElasticsearchDocumentSearchAdapter implements DocumentSearchEngine {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "documents";

    public ElasticsearchDocumentSearchAdapter(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public void indexDocument(Document document) {
        try {
            if (!document.isProcessed() || document.getExtractedContent() == null) {
                return; // Only index processed documents with content
            }

            DocumentSearchDocument searchDoc = new DocumentSearchDocument(
                document.getId().getValue(),
                document.getFileName(),
                document.getOriginalFileName(),
                document.getExtractedContent(),
                document.getUploader(),
                document.getUploadedAt().toString()
            );

            IndexRequest<DocumentSearchDocument> request = IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .id(document.getId().getValue())
                .document(searchDoc)
            );

            elasticsearchClient.index(request);

        } catch (Exception e) {
            throw new SearchEngineException("Failed to index document: " + document.getId(), e);
        }
    }

    @Override
    public void deleteDocument(DocumentId documentId) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                .index(INDEX_NAME)
                .id(documentId.getValue())
            );

            elasticsearchClient.delete(request);

        } catch (Exception e) {
            throw new SearchEngineException("Failed to delete document from index: " + documentId, e);
        }
    }

    @Override
    public SearchResults search(String query, int page, int size) {
        try {
            Query searchQuery = Query.of(q -> q
                .multiMatch(m -> m
                    .query(query)
                    .fields("fileName^2", "originalFileName^2", "extractedContent")
                )
            );

            SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(searchQuery)
                .from(page * size)
                .size(size)
                .highlight(h -> h
                    .fields("extractedContent", hf -> hf
                        .fragmentSize(150)
                        .numberOfFragments(1)
                    )
                )
            );

            SearchResponse<DocumentSearchDocument> response = elasticsearchClient.search(request, DocumentSearchDocument.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<DocumentSearchDocument> hit : response.hits().hits()) {
                DocumentSearchDocument doc = hit.source();
                String highlightedContent = hit.highlight().get("extractedContent") != null
                    ? String.join(" ", hit.highlight().get("extractedContent"))
                    : doc.getExtractedContent().substring(0, Math.min(150, doc.getExtractedContent().length()));

                results.add(new SearchResult(
                    DocumentId.of(doc.getId()),
                    doc.getFileName(),
                    highlightedContent,
                    hit.score()
                ));
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : results.size();

            return new SearchResults(results, totalHits);

        } catch (Exception e) {
            throw new SearchEngineException("Failed to search documents", e);
        }
    }

    @Override
    public List<String> autocomplete(String query, int maxSuggestions) {
        try {
            Query autocompleteQuery = Query.of(q -> q
                .matchPhrase(m -> m
                    .field("extractedContent")
                    .query(query)
                )
            );

            SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(autocompleteQuery)
                .size(maxSuggestions * 2) // Get more to filter unique suggestions
                .source(src -> src.filter(f -> f.includes("extractedContent")))
            );

            SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);

            // Extract unique suggestions from content
            List<String> suggestions = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                String content = (String) source.get("extractedContent");

                // Simple extraction of phrases containing the query
                String[] words = content.toLowerCase().split("\\s+");
                for (int i = 0; i < words.length - 1; i++) {
                    if (words[i].contains(query.toLowerCase())) {
                        String suggestion = words[i] + " " + words[i + 1];
                        if (!suggestions.contains(suggestion) && suggestions.size() < maxSuggestions) {
                            suggestions.add(suggestion);
                        }
                    }
                }

                if (suggestions.size() >= maxSuggestions) break;
            }

            return suggestions;

        } catch (Exception e) {
            throw new SearchEngineException("Failed to get autocomplete suggestions", e);
        }
    }

    public static class SearchEngineException extends RuntimeException {
        public SearchEngineException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Inner class for Elasticsearch document structure
    public static class DocumentSearchDocument {
        private String id;
        private String fileName;
        private String originalFileName;
        private String extractedContent;
        private String uploader;
        private String uploadedAt;

        public DocumentSearchDocument() {}

        public DocumentSearchDocument(String id, String fileName, String originalFileName,
                                    String extractedContent, String uploader, String uploadedAt) {
            this.id = id;
            this.fileName = fileName;
            this.originalFileName = originalFileName;
            this.extractedContent = extractedContent;
            this.uploader = uploader;
            this.uploadedAt = uploadedAt;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

        public String getExtractedContent() { return extractedContent; }
        public void setExtractedContent(String extractedContent) { this.extractedContent = extractedContent; }

        public String getUploader() { return uploader; }
        public void setUploader(String uploader) { this.uploader = uploader; }

        public String getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
    }
}
