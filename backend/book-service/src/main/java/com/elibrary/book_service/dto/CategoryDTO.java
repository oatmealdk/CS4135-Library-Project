package com.elibrary.book_service.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryDTO {
    private Long categoryId;

    @NotBlank
    private String name;
    private String description;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
