package com.example.DocIx.domain.port.in;

import java.util.List;

/**
 * Interface use case untuk fitur autocomplete/saran pencarian
 * Menyediakan saran kata kunci berdasarkan input pengguna
 */
public interface AutocompleteUseCase {

    /**
     * Mendapatkan saran autocomplete berdasarkan query
     * @param query query autocomplete yang berisi kata kunci dan batas saran
     * @return daftar saran kata kunci
     */
    List<String> getAutocompleteSuggestions(AutocompleteQuery query);

    /**
     * Query object untuk autocomplete yang berisi parameter pencarian saran
     */
    class AutocompleteQuery {
        private final String query;
        private final int maxSuggestions;

        /**
         * Konstruktor untuk membuat query autocomplete
         * @param query kata kunci untuk mencari saran
         * @param maxSuggestions jumlah maksimal saran (maksimal 5)
         */
        public AutocompleteQuery(String query, int maxSuggestions) {
            this.query = query;
            this.maxSuggestions = Math.min(Math.max(1, maxSuggestions), 5); // Maksimal 5 saran
        }

        /** Mendapatkan kata kunci query */
        public String getQuery() { return query; }
        /** Mendapatkan jumlah maksimal saran */
        public int getMaxSuggestions() { return maxSuggestions; }
    }
}
