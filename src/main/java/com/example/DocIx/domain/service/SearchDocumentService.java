package com.example.DocIx.domain.service;

import com.example.DocIx.adapter.out.search.ElasticsearchDocumentSearchAdapter;
import com.example.DocIx.adapter.out.search.SearchEngineException;
import com.example.DocIx.domain.port.in.SearchDocumentUseCase;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service untuk mengelola pencarian dokumen
 * Menangani validasi query, pencarian di search engine, dan paginasi hasil
 */
@Service
public class SearchDocumentService implements SearchDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SearchDocumentService.class);
    private final DocumentSearchEngine searchEngine;

    /**
     * Konstruktor untuk dependency injection
     * @param searchEngine search engine untuk melakukan pencarian dokumen
     */
    public SearchDocumentService(DocumentSearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    /**
     * Melakukan pencarian dokumen berdasarkan query
     * @param query query pencarian yang berisi kata kunci dan parameter paginasi
     * @return response pencarian yang berisi hasil dan metadata
     */
    @Override
    public SearchResponse searchDocuments(SearchQuery query) {
        validateSearchQuery(query);

        logger.info("Melakukan pencarian dokumen - Query: '{}', Page: {}, Size: {}",
                   query.getQuery().length() > 50 ? query.getQuery().substring(0, 50) + "..." : query.getQuery(),
                   query.getPage(), query.getSize());

        try {
            // Pencarian di indeks segmen dokumen
            List<SearchResult> results;
            long totalHits = 0;

            if (searchEngine instanceof ElasticsearchDocumentSearchAdapter elasticsearchAdapter) {
                results = elasticsearchAdapter.searchPages(
                    query.getQuery(),
                    query.getPage(),
                    query.getSize()
                );

                // Untuk totalHits, gunakan query count terpisah atau estimasi
                // untuk menghindari masalah performa dengan result set besar
                totalHits = estimateTotalHits(elasticsearchAdapter, query.getQuery());

                logger.info("Hasil pencarian - Estimated total: {}, Page results: {}", totalHits, results.size());
            } else {
                // Fallback ke pencarian reguler jika tidak menggunakan Elasticsearch adapter
                results = searchEngine.search(
                    query.getQuery(),
                    query.getPage(),
                    query.getSize()
                );
                totalHits = results.size();
            }

            return new SearchResponse(results, totalHits, query.getPage(), query.getSize());

        } catch (SearchEngineException e) {
            logger.error("SearchEngine error saat melakukan pencarian - Query: '{}', ErrorCode: {}, Error: {}",
                        query.getQuery().length() > 50 ? query.getQuery().substring(0, 50) + "..." : query.getQuery(),
                        e.getErrorCode(), e.getMessage());

            // Konversi SearchEngineException ke runtime exception yang sesuai
            // Controller layer akan menangani mapping HTTP
            throw new SearchServiceException("Pencarian gagal: " + e.getErrorCode().getDescription(), e);

        } catch (Exception e) {
            logger.error("Error tidak terduga saat melakukan pencarian - Query: '{}', Error: {}",
                        query.getQuery().length() > 50 ? query.getQuery().substring(0, 50) + "..." : query.getQuery(),
                        e.getMessage(), e);
            throw new SearchServiceException("Gagal melakukan pencarian: " + e.getMessage(), e);
        }
    }

    /**
     * Mengestimasi total hits dari query pencarian
     * @param adapter adapter Elasticsearch untuk pencarian
     * @param query query pencarian
     * @return estimasi jumlah total hasil
     */
    private long estimateTotalHits(ElasticsearchDocumentSearchAdapter adapter, String query) {
        try {
            // Saat ini, return count sederhana tanpa field collapsing
            // Di production, mungkin ingin menggunakan query count terpisah
            // atau implementasi search_after untuk result set besar
            List<SearchResult> countResults = adapter.searchPages(query, 0, 1);
            return countResults.isEmpty() ? 0 : 1000; // Estimasi sederhana
        } catch (Exception e) {
            logger.warn("Gagal mengestimasi total hits, menggunakan default: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Memvalidasi query pencarian
     * @param query query yang akan divalidasi
     * @throws IllegalArgumentException jika validasi gagal
     */
    private void validateSearchQuery(SearchQuery query) {
        if (query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Query pencarian tidak boleh kosong");
        }

        if (query.getQuery().trim().length() < 2) {
            throw new IllegalArgumentException("Query pencarian minimal 2 karakter");
        }

        if (query.getPage() < 0) {
            throw new IllegalArgumentException("Page tidak boleh negatif");
        }

        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("Size harus antara 1-100");
        }
    }

    /**
     * Exception khusus untuk search service
     * Digunakan untuk menandai kesalahan yang terjadi pada layer service pencarian
     */
    public static class SearchServiceException extends RuntimeException {
        public SearchServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
