package com.example.DocIx.adapter.out.search;

/**
 * Exception thrown when the search engine encounters an error.
 */
public class SearchEngineException extends RuntimeException {
    public SearchEngineException(String message) {
        super(message);
    }

    public SearchEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
