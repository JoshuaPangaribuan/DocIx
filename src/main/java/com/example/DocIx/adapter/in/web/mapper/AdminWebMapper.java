package com.example.DocIx.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.example.DocIx.domain.port.in.AdminIndexingUseCase.IndexingSummaryResponse;
import com.example.DocIx.domain.port.in.AdminIndexingUseCase.IndexingConsistencyResponse;

@Mapper(componentModel = "spring")
public interface AdminWebMapper {

	AdminWebMapper INSTANCE = Mappers.getMapper(AdminWebMapper.class);

	default IndexingSummaryResponse toWeb(IndexingSummaryResponse s) { return s; }

	default IndexingConsistencyResponse toWeb(IndexingConsistencyResponse s) { return s; }
}


