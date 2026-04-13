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

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

        title = normalizeQueryTerm(title);
        author = normalizeQueryTerm(author);
        keyword = normalizeQueryTerm(keyword);

        // Keyword-only searches fetch via title filter on book-service (first page), then filter in memory.
        String titleForBooks = title != null ? title : keyword;

        UriComponentsBuilder b = UriComponentsBuilder
            .fromUriString(BOOK_SERVICE + "/api/books")
            .queryParam("page", page)
            .queryParam("size", size);

        if (titleForBooks != null) b.queryParam("title", titleForBooks);
        if (author != null) b.queryParam("author", author);
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
        if (keyword != null) {
            String needle = foldWhitespace(keyword.toLowerCase(Locale.ROOT));
            rows = rows.stream()
                .filter(book -> matchesKeyword(book, needle))
                .collect(Collectors.toList());
        }

        String summary = buildSummary(title, author, keyword);
        return new SearchResultDto(
            summary,
            rows,
            body.getPage(),
            body.getSize(),
            keyword != null ? rows.size() : body.getTotalElements(),
            body.getTotalPages(),
            LocalDateTime.now());
    }

    private static String foldWhitespace(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(
            s.replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .trim()
                .replaceAll("\\s+", " "),
            Normalizer.Form.NFC
        );
    }

    private static boolean matchesKeyword(BookCatalogueDto book, String needle) {
        return foldWhitespace(book.getTitle()).toLowerCase(Locale.ROOT).contains(needle)
            || foldWhitespace(book.getAuthor()).toLowerCase(Locale.ROOT).contains(needle)
            || foldWhitespace(book.getDescription()).toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String buildSummary(String title, String author, String keyword) {
        List<String> parts = new ArrayList<>();
        if (title != null) parts.add("title=" + title);
        if (author != null) parts.add("author=" + author);
        if (keyword != null) parts.add("keyword=" + keyword);
        return parts.isEmpty() ? "browse" : String.join(", ", parts);
    }

    private static String normalizeQueryTerm(String value) {
        if (value == null) {
            return null;
        }
        String collapsed = Normalizer.normalize(
            value
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .trim()
                .replaceAll("\\s+", " "),
            Normalizer.Form.NFC
        );
        return collapsed.isEmpty() ? null : collapsed;
    }

    public List<String> suggest(String q) {
        q = normalizeQueryTerm(q);
        if (q == null) {
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
