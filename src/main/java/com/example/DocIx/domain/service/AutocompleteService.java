package com.example.DocIx.domain.service;

import com.example.DocIx.domain.port.in.AutocompleteUseCase;
import com.example.DocIx.domain.port.out.DocumentSearchEngine;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutocompleteService implements AutocompleteUseCase {

    private final DocumentSearchEngine searchEngine;

    public AutocompleteService(DocumentSearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    @Override
    public List<String> getAutocompleteSuggestions(AutocompleteQuery query) {
        validateAutocompleteQuery(query);

        return searchEngine.autocomplete(query.getQuery(), query.getMaxSuggestions());
    }

    private void validateAutocompleteQuery(AutocompleteQuery query) {
        if (query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Autocomplete query cannot be empty");
        }

        if (query.getQuery().trim().length() < 1) {
            throw new IllegalArgumentException("Autocomplete query must be at least 1 character long");
        }
    }
}
