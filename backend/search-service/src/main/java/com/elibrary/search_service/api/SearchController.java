package com.elibrary.search_service.api;

import com.elibrary.search_service.application.CatalogueSearchService;
import com.elibrary.search_service.dto.SearchResultDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final CatalogueSearchService catalogueSearchService;

    public SearchController(CatalogueSearchService catalogueSearchService) {
        this.catalogueSearchService = catalogueSearchService;
    }

    /**
     * Search / browse catalogue via book-service (customer–supplier).
     * Browse = call with no filters (full first page of catalogue).
     */
    @GetMapping
    public SearchResultDto search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer publishYearFrom,
            @RequestParam(required = false) Integer publishYearTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return catalogueSearchService.search(
            title, author, categoryId, status, publishYearFrom, publishYearTo, keyword, page, size);
    }

    @GetMapping("/suggestions")
    public List<String> suggestions(@RequestParam String q) {
        return catalogueSearchService.suggest(q);
    }
}
