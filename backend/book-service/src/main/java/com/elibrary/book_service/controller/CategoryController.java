package com.elibrary.book_service.controller;

import com.elibrary.book_service.domain.service.CatalogueManagementService;
import com.elibrary.book_service.dto.CategoryDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CatalogueManagementService catalogueManagementService;

    public CategoryController(CatalogueManagementService catalogueManagementService) {
        this.catalogueManagementService = catalogueManagementService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(catalogueManagementService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO request) {
        return ResponseEntity.ok(catalogueManagementService.createCategory(request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        catalogueManagementService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
