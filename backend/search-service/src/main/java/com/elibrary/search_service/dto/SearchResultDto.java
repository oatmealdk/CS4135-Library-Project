package com.elibrary.search_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SearchResultDto(
    String querySummary,
    List<BookCatalogueDto> results,
    int page,
    int pageSize,
    long totalResults,
    int totalPages,
    LocalDateTime searchedAt
) {}
