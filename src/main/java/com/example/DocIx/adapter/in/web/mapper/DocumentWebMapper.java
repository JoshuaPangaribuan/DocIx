package com.example.DocIx.adapter.in.web.mapper;

import com.example.DocIx.adapter.in.web.DocumentController.*;
import com.example.DocIx.domain.port.in.SearchDocumentUseCase;
import com.example.DocIx.domain.port.in.UploadDocumentUseCase;
import com.example.DocIx.domain.port.out.DocumentSearchEngine.SearchResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentWebMapper {

    DocumentWebMapper INSTANCE = Mappers.getMapper(DocumentWebMapper.class);

    // Upload mappings
    @Mapping(target = "documentId", source = "documentId.value")
    UploadResponse toUploadResponse(UploadDocumentUseCase.UploadResult result);

    // Helper methods for bulk upload - return correct nested class types
    default BulkUploadResponse.UploadResult toUploadResult(String fileName, String documentId, String message) {
        return new BulkUploadResponse.UploadResult(fileName, documentId, message);
    }

    default BulkUploadResponse.UploadError toUploadError(String fileName, String error) {
        return new BulkUploadResponse.UploadError(fileName, error);
    }

    // Search mappings - use default method to handle hasNext/hasPrevious properly
    default SearchResponse toSearchResponse(SearchDocumentUseCase.SearchResponse searchResponse) {
        return new SearchResponse(
                toSearchResultDtoList(searchResponse.getResults()),
                searchResponse.getTotalHits(),
                searchResponse.getPage(),
                searchResponse.getSize(),
                searchResponse.hasNext(),
                searchResponse.hasPrevious());
    }

    @Mapping(target = "documentId", source = "documentId.value")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "highlightedContent", source = "highlightedContent")
    @Mapping(target = "score", source = "score")
    SearchResultDto toSearchResultDto(SearchResult searchResult);

    List<SearchResultDto> toSearchResultDtoList(List<SearchResult> searchResults);

    // Autocomplete mappings
    default AutocompleteResponse toAutocompleteResponse(List<String> suggestions) {
        return new AutocompleteResponse(suggestions);
    }
}
