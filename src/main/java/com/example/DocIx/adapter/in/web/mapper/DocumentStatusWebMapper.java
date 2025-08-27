package com.example.DocIx.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.example.DocIx.adapter.in.web.DocumentStatusController.DocumentStatusResponse;
import com.example.DocIx.domain.port.in.DocumentStatusQueryUseCase;

@Mapper(componentModel = "spring")
public interface DocumentStatusWebMapper {

	DocumentStatusWebMapper INSTANCE = Mappers.getMapper(DocumentStatusWebMapper.class);

	default DocumentStatusResponse toWeb(DocumentStatusQueryUseCase.DocumentStatusResponse s) {
		return new DocumentStatusResponse(
				s.getDocumentId(),
				s.getOriginalFileName(),
				s.getStatus(),
				s.getUploadedAt(),
				s.getLastProcessedAt(),
				s.getErrorMessage());
	}
}


