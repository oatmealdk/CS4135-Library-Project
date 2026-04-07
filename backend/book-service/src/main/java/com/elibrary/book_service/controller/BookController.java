package com.elibrary.book_service.controller;

import com.elibrary.book_service.domain.model.BookStatus;
import com.elibrary.book_service.domain.service.CatalogueManagementService;
import com.elibrary.book_service.dto.BookDTO;
import com.elibrary.book_service.dto.PagedResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final CatalogueManagementService catalogueManagementService;

    public BookController(CatalogueManagementService catalogueManagementService) {
        this.catalogueManagementService = catalogueManagementService;
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookDTO> getBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(catalogueManagementService.getBook(bookId));
    }

    @GetMapping("/{bookId}/availability")
    public ResponseEntity<Map<String, Object>> getBookAvailability(@PathVariable Long bookId) {
        return ResponseEntity.ok(catalogueManagementService.getAvailability(bookId));
    }

    @PutMapping("/{bookId}/decrement-copies")
    public ResponseEntity<Map<String, Object>> decrementCopies(@PathVariable Long bookId) {
        return ResponseEntity.ok(catalogueManagementService.decrementCopies(bookId));
    }

    @PutMapping("/{bookId}/increment-copies")
    public ResponseEntity<Map<String, Object>> incrementCopies(@PathVariable Long bookId) {
        return ResponseEntity.ok(catalogueManagementService.incrementCopies(bookId));
    }

    @GetMapping
    public ResponseEntity<PagedResult<BookDTO>> searchBooks(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BookStatus status,
        @RequestParam(required = false) Integer publishYearFrom,
        @RequestParam(required = false) Integer publishYearTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            catalogueManagementService.searchBooks(
                title, author, categoryId, status, publishYearFrom, publishYearTo, page, size
            )
        );
    }

    @PostMapping
    public ResponseEntity<BookDTO> addBook(@Valid @RequestBody BookDTO request) {
        return ResponseEntity.ok(catalogueManagementService.addBook(request));
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long bookId, @Valid @RequestBody BookDTO request) {
        return ResponseEntity.ok(catalogueManagementService.updateBook(bookId, request));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromCatalogue(@PathVariable Long bookId) {
        catalogueManagementService.removeFromCatalogue(bookId);
        return ResponseEntity.noContent().build();
    }
}
