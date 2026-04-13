package com.elibrary.search_service.application;

import com.elibrary.search_service.dto.SearchResultDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogueSearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private CatalogueSearchService catalogueSearchService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        catalogueSearchService = new CatalogueSearchService(restTemplate, new ObjectMapper());
    }

    @Test
    void search_appliesKeywordFilterAndKeepsSummary() {
        String payload = """
            {
              "content": [
                {"bookId":1,"title":"Effective Java","author":"Joshua Bloch","description":"Java best practices"},
                {"bookId":2,"title":"Domain-Driven Design","author":"Eric Evans","description":"Architecture and modelling"}
              ],
              "page":0,
              "size":10,
              "totalElements":2,
              "totalPages":1
            }
            """;
        when(restTemplate.getForEntity(eq("http://book-service/api/books?page=0&size=10&title=Effective"), eq(String.class)))
            .thenReturn(ResponseEntity.ok(payload));

        SearchResultDto result = catalogueSearchService.search("Effective", null, null, null, null, null, "java", 0, 10);

        assertEquals("title=Effective, keyword=java", result.querySummary());
        assertEquals(1, result.results().size());
        assertEquals("Effective Java", result.results().getFirst().getTitle());
        assertEquals(1, result.totalResults());
    }

    @Test
    void suggest_returnsDistinctTitlesUpToEight() {
        String payload = """
            {
              "content": [
                {"bookId":1,"title":"Refactoring"},
                {"bookId":2,"title":"Refactoring"},
                {"bookId":3,"title":"Clean Code"}
              ],
              "page":0,
              "size":10,
              "totalElements":3,
              "totalPages":1
            }
            """;
        when(restTemplate.getForEntity(eq("http://book-service/api/books?page=0&size=10&title=ref"), eq(String.class)))
            .thenReturn(ResponseEntity.ok(payload));

        List<String> suggestions = catalogueSearchService.suggest("ref");

        assertEquals(List.of("Refactoring", "Clean Code"), suggestions);
    }

    @Test
    void search_throwsBadGatewayWhenBookServiceResponseIsInvalid() {
        when(restTemplate.getForEntity(eq("http://book-service/api/books?page=0&size=10"), eq(String.class)))
            .thenReturn(new ResponseEntity<>("not-json", HttpStatus.OK));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> catalogueSearchService.search(null, null, null, null, null, null, null, 0, 10));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }
}
