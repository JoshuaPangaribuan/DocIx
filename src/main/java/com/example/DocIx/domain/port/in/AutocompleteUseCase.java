package com.example.DocIx.domain.port.in;

import java.util.List;

public interface AutocompleteUseCase {

    List<String> getAutocompleteSuggestions(AutocompleteQuery query);

    class AutocompleteQuery {
        private final String query;
        private final int maxSuggestions;

        public AutocompleteQuery(String query, int maxSuggestions) {
            this.query = query;
            this.maxSuggestions = Math.min(Math.max(1, maxSuggestions), 5); // Max 5 suggestions
        }

        public String getQuery() { return query; }
        public int getMaxSuggestions() { return maxSuggestions; }
    }
}
