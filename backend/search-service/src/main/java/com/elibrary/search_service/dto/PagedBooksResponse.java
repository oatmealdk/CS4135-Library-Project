package com.elibrary.search_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedBooksResponse {
    private List<BookCatalogueDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<BookCatalogueDto> getContent() { return content; }
    public void setContent(List<BookCatalogueDto> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
