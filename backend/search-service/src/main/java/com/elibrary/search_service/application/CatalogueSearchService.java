package com.elibrary.search_service.application;

import com.elibrary.search_service.dto.BookCatalogueDto;
import com.elibrary.search_service.dto.PagedBooksResponse;
import com.elibrary.search_service.dto.SearchResultDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class CatalogueSearchService {

    private static final String BOOK_SERVICE = "http://book-service";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CatalogueSearchService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public SearchResultDto search(
            String title,
            String author,
            Long categoryId,
            String status,
            Integer publishYearFrom,
            Integer publishYearTo,
            String keyword,
            int page,
            int size) {

        UriComponentsBuilder b = UriComponentsBuilder
            .fromUriString(BOOK_SERVICE + "/api/books")
            .queryParam("page", page)
            .queryParam("size", size);

        if (title != null && !title.isBlank()) b.queryParam("title", title.trim());
        if (author != null && !author.isBlank()) b.queryParam("author", author.trim());
        if (categoryId != null) b.queryParam("categoryId", categoryId);
        if (status != null && !status.isBlank()) b.queryParam("status", status.trim());
        if (publishYearFrom != null) b.queryParam("publishYearFrom", publishYearFrom);
        if (publishYearTo != null) b.queryParam("publishYearTo", publishYearTo);

        String url = b.toUriString();
        PagedBooksResponse body = fetchPagedBooks(url);
        if (body == null || body.getContent() == null) {
            return new SearchResultDto(
                "empty", List.of(), page, size, 0, 0, LocalDateTime.now());
        }

        List<BookCatalogueDto> rows = body.getContent();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.toLowerCase();
            rows = rows.stream()
                .filter(book -> matchesKeyword(book, k))
                .collect(Collectors.toList());
        }

        String summary = buildSummary(title, author, keyword);
        return new SearchResultDto(
            summary,
            rows,
            body.getPage(),
            body.getSize(),
            keyword != null && !keyword.isBlank() ? rows.size() : body.getTotalElements(),
            body.getTotalPages(),
            LocalDateTime.now());
    }

    private static boolean matchesKeyword(BookCatalogueDto book, String k) {
        return (book.getTitle() != null && book.getTitle().toLowerCase().contains(k))
            || (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(k))
            || (book.getDescription() != null && book.getDescription().toLowerCase().contains(k));
    }

    private static String buildSummary(String title, String author, String keyword) {
        List<String> parts = new ArrayList<>();
        if (title != null && !title.isBlank()) parts.add("title=" + title);
        if (author != null && !author.isBlank()) parts.add("author=" + author);
        if (keyword != null && !keyword.isBlank()) parts.add("keyword=" + keyword);
        return parts.isEmpty() ? "browse" : String.join(", ", parts);
    }

    public List<String> suggest(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        SearchResultDto result = search(q, null, null, null, null, null, null, 0, 10);
        return result.results().stream()
            .map(BookCatalogueDto::getTitle)
            .distinct()
            .limit(8)
            .toList();
    }

    /**
     * Fetch JSON as text then bind with Jackson so {@code List<BookCatalogueDto>} deserialises
     * correctly (RestTemplate#getForObject(Class) can fail on nested generics).
     */
    private PagedBooksResponse fetchPagedBooks(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(BAD_GATEWAY,
                    "book-service returned " + response.getStatusCode());
            }
            return objectMapper.readValue(response.getBody(), new TypeReference<PagedBooksResponse>() {});
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(BAD_GATEWAY,
                "Cannot reach book-service via Eureka (is it registered and healthy?). " + e.getMessage(),
                e);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY,
                "Failed to parse book catalogue response: " + e.getMessage(), e);
        }
    }
}
