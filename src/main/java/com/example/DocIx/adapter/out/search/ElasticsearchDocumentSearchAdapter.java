package com.example.DocIx.adapter.out.search;

import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;
import com.example.DocIx.domain.port.out.PageExtractor;

import com.example.DocIx.domain.util.LoggingUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

@Component
public class ElasticsearchDocumentSearchAdapter implements DocumentSearchEngine {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDocumentSearchAdapter.class);
    private final ElasticsearchClient elasticsearchClient;
    private static final String PAGES_INDEX_NAME = "document_pages";

    // Metrics
    private final Counter searchRequestsTotal;
    private final Counter searchErrorsTotal;
    private final Timer searchLatencyTimer;
    private final Counter highlightEmptyRatio;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    @Autowired
    public ElasticsearchDocumentSearchAdapter(ElasticsearchClient elasticsearchClient,
            MeterRegistry meterRegistry,
            Tracer tracer) {
        this.elasticsearchClient = elasticsearchClient;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;

        // Initialize metrics
        this.searchRequestsTotal = Counter.builder("search_requests_total")
                .description("Total number of search requests")
                .register(meterRegistry);
        this.searchErrorsTotal = Counter.builder("search_errors_total")
                .description("Total number of search errors")
                .register(meterRegistry);
        this.searchLatencyTimer = Timer.builder("search_latency_ms")
                .description("Search operation latency in milliseconds")
                .register(meterRegistry);
        this.highlightEmptyRatio = Counter.builder("highlight_empty_ratio")
                .description("Ratio of search results with empty highlights")
                .register(meterRegistry);

        // Initialize indices on startup
        initializeIndices();
    }

    /**
     * Initialize Elasticsearch indices with proper mappings
     */
    private void initializeIndices() {
        try {
            createPagesIndexIfNotExists();
        } catch (Exception e) {
            logger.warn("Warning: Failed to initialize Elasticsearch indices: {}", e.getMessage());
        }
    }



    @Override
    public void indexDocument(Document document) {
        // This method is kept for backward compatibility but will be deprecated
        throw new UnsupportedOperationException(
                "Use indexDocument(Document, String) instead to provide extracted content");
    }

    /**
     * Index document page dengan retry mechanism
     */
    private void indexDocumentPageWithRetry(PageExtractor.DocumentPage page, int maxRetries) {
        long startTime = System.currentTimeMillis();
        String documentId = String.valueOf(page.getDocumentId());

        logger.info("Starting document page indexing - DocumentId: {}, Page: {}",
                documentId, page.getPageNumber());

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                DocumentPageDocument pageDoc = new DocumentPageDocument(
                        String.valueOf(page.getDocumentId()),
                        null,
                        null,
                        page.getContent(),
                        page.getPageNumber(),
                        null,
                        null,
                        null);

                String pageId = page.getDocumentId() + "_page_" + page.getPageNumber();

                IndexRequest<DocumentPageDocument> indexRequest = IndexRequest.of(i -> i
                        .index(PAGES_INDEX_NAME)
                        .id(pageId)
                        .document(pageDoc));

                IndexResponse response = elasticsearchClient.index(indexRequest);

                // Validate indexing success
                if (response.result() == null || (!response.result().toString().equalsIgnoreCase("created")
                        && !response.result().toString().equalsIgnoreCase("updated"))) {
                    throw new SearchEngineException(SearchEngineException.ErrorCode.INDEX_ERROR,
                            "Elasticsearch returned unexpected result: " + response.result(), null);
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Document page indexed successfully - DocumentId: {}, PageId: {}, " +
                        "Result: {}, Attempt: {}, Duration: {}ms",
                        documentId, pageId, response.result(), attempt, duration);

                return; // Success, exit retry loop

            } catch (Exception e) {
                lastException = e;
                String safeError = LoggingUtil.maskSensitiveData(e.getMessage());

                if (attempt < maxRetries) {
                    logger.warn("Failed to index document page (attempt {}/{}) - DocumentId: {}, Page: {}, " +
                            "Error: {}, Retrying...",
                            attempt, maxRetries, documentId, page.getPageNumber(), safeError);

                    // Wait before retry (exponential backoff)
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.error(
                            "Failed to index document page after {} attempts - DocumentId: {}, Page: {}, " +
                                    "Duration: {}ms, Error: {}",
                            maxRetries, documentId, page.getPageNumber(),
                            duration, safeError, e);
                }
            }
        }

        // If all attempts failed
        throw new SearchEngineException(SearchEngineException.ErrorCode.INDEX_ERROR,
                "Failed to index document page after " + maxRetries + " attempts: " +
                        (lastException != null ? lastException.getMessage() : "Unknown error"),
                lastException);
    }

    @Override
    public void deleteDocument(DocumentId documentId) {
        try {
            // Delete from pages index since all documents are now page-based
            deleteDocumentPages(documentId);
        } catch (Exception e) {
            throw new SearchEngineException("Failed to delete document: " + documentId, e);
        }
    }

    @Override
    public List<SearchResult> search(String query, int page, int size) {
        try {
            // Gunakan searchPages untuk konsistensi
            return searchPages(query, page, size);
        } catch (Exception e) {
            throw new SearchEngineException("Failed to search documents", e);
        }
    }

    @Override
    public List<String> autocomplete(String query, int maxSuggestions) {
        try {
            Query autocompleteQuery = Query.of(q -> q
                    .matchPhrase(m -> m
                            .field("content")
                            .query(query)));

            SearchRequest request = SearchRequest.of(s -> s
                    .index(PAGES_INDEX_NAME)
                    .query(autocompleteQuery)
                    .size(maxSuggestions * 2) // Get more to filter unique suggestions
                    .source(src -> src.filter(f -> f.includes("content"))));

            SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);

            // Extract unique suggestions from content
            List<String> suggestions = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                String content = (String) source.get("content");

                if (content != null) {
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
                }

                if (suggestions.size() >= maxSuggestions)
                    break;
            }

            return suggestions;

        } catch (Exception e) {
            throw new SearchEngineException("Failed to get autocomplete suggestions", e);
        }
    }





    /**
     * Create the document_pages index with proper mapping
     */
    private void createPagesIndexIfNotExists() {
        try {
            boolean indexExists = elasticsearchClient.indices().exists(e -> e.index(PAGES_INDEX_NAME)).value();

            if (!indexExists) {
                elasticsearchClient.indices().create(c -> c
                        .index(PAGES_INDEX_NAME)
                        .mappings(m -> m
                                .properties("documentId", p -> p.keyword(k -> k))
                                .properties("fileName", p -> p
                                        .text(t -> t.analyzer("standard")))
                                .properties("originalFileName", p -> p
                                        .text(t -> t.analyzer("standard")))
                                .properties("content", p -> p
                                        .text(t -> t.analyzer("standard")))
                                .properties("pageNumber", p -> p.integer(i -> i))
                                .properties("uploader", p -> p.keyword(k -> k))
                                .properties("uploadedAt", p -> p.date(d -> d))
                                .properties("downloadUrl", p -> p.keyword(k -> k))));
                logger.info("Created document_pages index with proper mapping");
            }
        } catch (Exception e) {
            throw new SearchEngineException(SearchEngineException.ErrorCode.MAPPING_ERROR,
                    "Failed to create document_pages index: " + e.getMessage(), e);
        }
    }

    /**
     * Index a single document page
     */
    @Override
    public void indexDocumentPage(PageExtractor.DocumentPage page) {
        // Ensure index exists with correct mappings before indexing
        createPagesIndexIfNotExists();
        indexDocumentPageWithRetry(page, 3);
    }

    /**
     * Index a document as pages for page-based indexing
     * 
     * @param document The document to index
     * @param pages List of DocumentPage objects to index separately
     */
    public void indexDocumentPages(Document document, List<PageExtractor.DocumentPage> pages) {
        try {
            if (!document.isProcessed() || pages == null || pages.isEmpty()) {
                return; // Only index processed documents with content
            }

            // Create pages index if not exists
            createPagesIndexIfNotExists();

            // First, delete any existing pages for this document
            deleteDocumentPages(document.getId());

            // Index each page
            for (PageExtractor.DocumentPage page : pages) {
                DocumentPageDocument pageDoc = new DocumentPageDocument(
                        String.valueOf(page.getDocumentId()),
                        document.getFileName(),
                        document.getOriginalFileName(),
                        page.getContent(),
                        page.getPageNumber(),
                        document.getUploader(),
                        document.getUploadedAt() != null ? document.getUploadedAt().toString() : null,
                        document.getDownloadUrl());

                String pageId = page.getDocumentId() + "_page_" + page.getPageNumber();

                IndexRequest<DocumentPageDocument> request = IndexRequest.of(idx -> idx
                        .index(PAGES_INDEX_NAME)
                        .id(pageId)
                        .document(pageDoc));

                elasticsearchClient.index(request);
            }

            logger.info("Successfully indexed {} pages for document: {}", pages.size(), document.getId().getValue());

        } catch (Exception e) {
            throw new SearchEngineException("Failed to index document pages: " + document.getId(), e);
        }
    }

    /**
     * Delete all pages for a specific document
     */
    public void deleteDocumentPages(DocumentId documentId) {
        try {
            // Check if the pages index exists first
            boolean indexExists = elasticsearchClient.indices().exists(e -> e.index(PAGES_INDEX_NAME)).value();

            if (!indexExists) {
                // Index doesn't exist yet, so no pages to delete
                return;
            }

            // Delete by query to remove all pages for this document
            Query deleteQuery = Query.of(q -> q
                    .term(t -> t
                            .field("documentId")
                            .value(documentId.getValue())));

            elasticsearchClient.deleteByQuery(dbq -> dbq
                    .index(PAGES_INDEX_NAME)
                    .query(deleteQuery));

        } catch (Exception e) {
            throw new SearchEngineException("Failed to delete document pages: " + documentId, e);
        }
    }

    /**
     * Search across document pages with enhanced relevance using field
     * collapsing
     */
    public List<SearchResult> searchPages(String query, int page, int size) {
        Span span = tracer.spanBuilder("searchPages")
                .setAttribute("query", query.length() > 50 ? query.substring(0, 50) + "..." : query)
                .setAttribute("page", page)
                .setAttribute("size", size)
                .startSpan();

        Timer.Sample sample = Timer.start(meterRegistry);
        searchRequestsTotal.increment();

        try (var scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();
            MDC.put("traceId", traceId);

            logger.info("Melakukan pencarian page - Query: '{}', Page: {}, Size: {}, TraceId: {}",
                    query.length() > 50 ? query.substring(0, 50) + "..." : query, page, size, traceId);

            createPagesIndexIfNotExists();

            boolean indexExists = elasticsearchClient.indices().exists(e -> e.index(PAGES_INDEX_NAME)).value();
            if (!indexExists) {
                logger.warn("Index document_pages belum ada, mengembalikan hasil kosong");
                return new ArrayList<>();
            }

            long documentCount = elasticsearchClient.count(c -> c.index(PAGES_INDEX_NAME)).count();
            if (documentCount == 0) {
                logger.warn("Tidak ada dokumen di index pages, mengembalikan hasil kosong");
                return new ArrayList<>();
            }

            logger.debug("Ditemukan {} dokumen di index pages", documentCount);

            // Build query
            final Query searchQuery;
            if ("*".equals(query.trim())) {
                searchQuery = Query.of(q -> q.matchAll(m -> m));
                logger.debug("Menggunakan match_all query untuk wildcard search");
            } else {
                logger.debug("Membangun query pencarian untuk: '{}'", query);

                // Query utama dengan multi_match yang lebih fleksibel
                Query primaryQuery = Query.of(q -> q
                        .multiMatch(m -> m
                                .query(query)
                                .fields("fileName^3", "originalFileName^3", "content^5")
                                .type(TextQueryType.BestFields)
                                .fuzziness("AUTO")
                                .minimumShouldMatch("50%") // Turunkan dari 75% ke 50%
                        ));

                // Query fallback dengan wildcard untuk kata-kata individual
                Query fallbackQuery = Query.of(q -> q
                        .bool(b -> b
                                .should(s -> s
                                        .wildcard(w -> w
                                                .field("content")
                                                .value("*" + query.toLowerCase() + "*")
                                                .boost(1.0f)))
                                .should(s -> s
                                        .wildcard(w -> w
                                                .field("fileName")
                                                .value("*" + query.toLowerCase() + "*")
                                                .boost(2.0f)))
                                .should(s -> s
                                        .wildcard(w -> w
                                                .field("originalFileName")
                                                .value("*" + query.toLowerCase() + "*")
                                                .boost(2.0f)))
                                .minimumShouldMatch("1")));

                // Gabungkan query utama dan fallback dengan bool should
                searchQuery = Query.of(q -> q
                        .bool(b -> b
                                .should(primaryQuery)
                                .should(fallbackQuery)
                                .minimumShouldMatch("1")));

                logger.debug(
                        "Query dikonfigurasi dengan primary query (minimum_should_match: 50%) dan fallback wildcard query");
            }

            // Build search request with field collapsing and stable sorting
            SearchRequest request = SearchRequest.of(s -> s
                    .index(PAGES_INDEX_NAME)
                    .query(searchQuery)
                    .from(page * size)
                    .size(size)
                    .sort(sort -> sort.score(sc -> sc.order(SortOrder.Desc)))
                    .sort(sort -> sort.field(f -> f.field("_id").order(SortOrder.Asc)))
                    .highlight(h -> h
                            .fields("content", hf -> hf
                                    .fragmentSize(300)
                                    .numberOfFragments(1)
                                    .preTags("<mark>")
                                    .postTags("</mark>"))
                            .fields("fileName", hf -> hf
                                    .fragmentSize(100)
                                    .numberOfFragments(1)
                                    .preTags("<mark>")
                                    .postTags("</mark>"))
                            .fields("originalFileName", hf -> hf
                                    .fragmentSize(100)
                                    .numberOfFragments(1)
                                    .preTags("<mark>")
                                    .postTags("</mark>"))));

            logger.debug("Menjalankan query Elasticsearch...");
            SearchResponse<DocumentPageDocument> response = elasticsearchClient.search(request,
                    DocumentPageDocument.class);

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
            logger.info("Query berhasil - Total hits: {}, Returned: {}", totalHits, response.hits().hits().size());

            List<SearchResult> results = new ArrayList<>();
            int emptyHighlights = 0;

            for (Hit<DocumentPageDocument> hit : response.hits().hits()) {
                DocumentPageDocument pageDoc = hit.source();

                if (pageDoc == null) {
                    logger.warn("Page null ditemukan, dilewati");
                    continue;
                }

                String highlightedContent = extractHighlightFromPage(hit, pageDoc, query);

                if (highlightedContent.isEmpty()) {
                    emptyHighlights++;
                }

                logger.debug("Highlighted content untuk page {}: '{}'",
                        pageDoc.getPageNumber(),
                        highlightedContent.length() > 50 ? highlightedContent.substring(0, 50) + "..."
                                : highlightedContent);

                SearchResult result = new SearchResult(
                        DocumentId.of(pageDoc.getDocumentId()),
                        pageDoc.getOriginalFileName() != null ? pageDoc.getOriginalFileName() : pageDoc.getFileName(),
                        highlightedContent,
                        hit.score() != null ? hit.score() : 0.0);

                // Set download URL
                if (pageDoc.getDownloadUrl() != null) {
                    result.setDownloadUrl(pageDoc.getDownloadUrl());
                } else {
                    result.setDownloadUrl("/api/documents/download/" + pageDoc.getDocumentId());
                }

                result.setPageInfo(pageDoc.getPageNumber());
                results.add(result);
            }

            // Update metrics
            if (!results.isEmpty()) {
                double emptyRatio = (double) emptyHighlights / results.size();
                highlightEmptyRatio.increment(emptyRatio);
            }

            logger.info("Pencarian selesai - Hasil dikembalikan: {}, Empty highlights: {}/{}",
                    results.size(), emptyHighlights, results.size());

            span.setAttribute("results_count", results.size());
            span.setAttribute("empty_highlights", emptyHighlights);

            return results;

        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            Counter.builder("search_errors_total")
                    .tag("type", "elasticsearch")
                    .register(meterRegistry)
                    .increment();
            logger.error("Elasticsearch error saat pencarian: {}", e.getMessage(), e);
            throw new SearchEngineException(SearchEngineException.ErrorCode.ES_UNAVAILABLE,
                    "Elasticsearch error: " + e.getMessage(), e);
        } catch (Exception e) {
            Counter.builder("search_errors_total")
                    .tag("type", "unknown")
                    .register(meterRegistry)
                    .increment();
            logger.error("Error tidak terduga saat pencarian: {}", e.getMessage(), e);
            throw new SearchEngineException(SearchEngineException.ErrorCode.UNKNOWN_ERROR,
                    "Gagal melakukan pencarian document pages: " + e.getMessage(), e);
        } finally {
            sample.stop(searchLatencyTimer);
            span.end();
            MDC.remove("traceId");
        }
    }

    /**
     * Extract highlight with consistent priority: content > fileName >
     * originalFileName
     */
    private String extractHighlightFromPage(Hit<DocumentPageDocument> hit, DocumentPageDocument page, String query) {
        String highlightedContent = "";

        // Priority: content > fileName > originalFileName
        if (hit.highlight() != null) {
            if (hit.highlight().get("content") != null && !hit.highlight().get("content").isEmpty()) {
                highlightedContent = String.join(" ", hit.highlight().get("content"));
            } else if (hit.highlight().get("fileName") != null && !hit.highlight().get("fileName").isEmpty()) {
                highlightedContent = String.join(" ", hit.highlight().get("fileName"));
            } else if (hit.highlight().get("originalFileName") != null
                    && !hit.highlight().get("originalFileName").isEmpty()) {
                highlightedContent = String.join(" ", hit.highlight().get("originalFileName"));
            }
        }

        // Snippet fallback if highlight is empty
        if (highlightedContent.isEmpty() && page.getContent() != null) {
            highlightedContent = createSnippet(page.getContent(), query, 300);
        }

        return highlightedContent;
    }

    /**
     * Create manual snippet from content with query context
     */
    private String createSnippet(String content, String query, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String lowercaseContent = content.toLowerCase();
        String lowercaseQuery = query.toLowerCase();

        // Find query position in content
        int queryPos = lowercaseContent.indexOf(lowercaseQuery);
        if (queryPos != -1) {
            // Extract context around query
            int start = Math.max(0, queryPos - 100);
            int end = Math.min(content.length(), queryPos + lowercaseQuery.length() + 100);
            String snippet = content.substring(start, end);

            // Add ellipsis if truncated
            if (start > 0)
                snippet = "..." + snippet;
            if (end < content.length())
                snippet = snippet + "...";

            return snippet;
        } else {
            // If query not found, take beginning of content
            String snippet = content.substring(0, Math.min(maxLength, content.length()));
            if (content.length() > maxLength)
                snippet += "...";
            return snippet;
        }
    }

    /**
     * Cek apakah dokumen ada di Elasticsearch
     */
    public boolean documentExists(String documentId) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(PAGES_INDEX_NAME)
                    .query(q -> q
                            .term(t -> t
                                    .field("documentId")
                                    .value(documentId)))
                    .size(1));

            SearchResponse<DocumentPageDocument> response = elasticsearchClient.search(searchRequest,
                    DocumentPageDocument.class);
            return response.hits().total().value() > 0;

        } catch (Exception e) {
            logger.error("Error saat mengecek keberadaan dokumen di Elasticsearch: {}", documentId, e);
            return false;
        }
    }



    /**
     * Inner class for Elasticsearch page document structure
     */
    public static class DocumentPageDocument {
        private String documentId;
        private String fileName;
        private String originalFileName;
        private String content;
        private int pageNumber;
        private String uploader;
        private String uploadedAt;
        private String downloadUrl;

        public DocumentPageDocument() {
        }

        public DocumentPageDocument(String documentId, String fileName, String originalFileName,
                String content, int pageNumber, String uploader, String uploadedAt, String downloadUrl) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.originalFileName = originalFileName;
            this.content = content;
            this.pageNumber = pageNumber;
            this.uploader = uploader;
            this.uploadedAt = uploadedAt;
            this.downloadUrl = downloadUrl;
        }

        // Getters and setters
        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public String getUploader() {
            return uploader;
        }

        public void setUploader(String uploader) {
            this.uploader = uploader;
        }

        public String getUploadedAt() {
            return uploadedAt;
        }

        public void setUploadedAt(String uploadedAt) {
            this.uploadedAt = uploadedAt;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }
}
