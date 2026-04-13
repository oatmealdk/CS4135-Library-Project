package com.elibrary.search_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mirrors book-service {@code BookDTO} JSON. Unknown fields ignored so API can evolve.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookCatalogueDto {
    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private Integer publishYear;
    private String description;
    private Integer totalCopies;
    private Integer availableCopies;
    private String status;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Integer getPublishYear() { return publishYear; }
    public void setPublishYear(Integer publishYear) { this.publishYear = publishYear; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
