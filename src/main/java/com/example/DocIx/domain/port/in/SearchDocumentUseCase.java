package com.example.DocIx.domain.port.in;

import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;

import java.util.List;

/**
 * Interface use case untuk pencarian dokumen dalam sistem
 * Mendefinisikan kontrak untuk operasi pencarian dan paginasi hasil
 */
public interface SearchDocumentUseCase {

    /**
     * Melakukan pencarian dokumen berdasarkan query
     * @param query query pencarian yang berisi kata kunci dan parameter paginasi
     * @return response yang berisi hasil pencarian dan metadata paginasi
     */
    SearchResponse searchDocuments(SearchQuery query);

    /**
     * Query object yang berisi parameter pencarian dokumen
     */
    class SearchQuery {
        private final String query;
        private final int page;
        private final int size;

        /**
         * Konstruktor untuk membuat query pencarian
         * @param query kata kunci pencarian
         * @param page nomor halaman (dimulai dari 0)
         * @param size jumlah hasil per halaman (maksimal 100)
         */
        public SearchQuery(String query, int page, int size) {
            this.query = query;
            this.page = Math.max(0, page);
            this.size = Math.min(Math.max(1, size), 100); // Maksimal 100 hasil per halaman
        }

        /** Mendapatkan kata kunci pencarian */
        public String getQuery() { return query; }
        /** Mendapatkan nomor halaman */
        public int getPage() { return page; }
        /** Mendapatkan ukuran halaman */
        public int getSize() { return size; }
    }

    /**
     * Response object yang berisi hasil pencarian dan metadata paginasi
     */
    class SearchResponse {
        private final List<SearchResult> results;
        private final long totalHits;
        private final int page;
        private final int size;

        /**
         * Konstruktor untuk membuat response pencarian
         * @param results daftar hasil pencarian
         * @param totalHits total jumlah hasil yang ditemukan
         * @param page nomor halaman saat ini
         * @param size ukuran halaman
         */
        public SearchResponse(List<SearchResult> results, long totalHits, int page, int size) {
            this.results = results;
            this.totalHits = totalHits;
            this.page = page;
            this.size = size;
        }

        /** Mendapatkan daftar hasil pencarian */
        public List<SearchResult> getResults() { return results; }
        /** Mendapatkan total jumlah hasil */
        public long getTotalHits() { return totalHits; }
        /** Mendapatkan nomor halaman saat ini */
        public int getPage() { return page; }
        /** Mendapatkan ukuran halaman */
        public int getSize() { return size; }
        /** Memeriksa apakah ada halaman selanjutnya */
        public boolean hasNext() { return (page + 1) * size < totalHits; }
        /** Memeriksa apakah ada halaman sebelumnya */
        public boolean hasPrevious() { return page > 0; }
    }
}
