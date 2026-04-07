package com.elibrary.book_service.domain.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "book_category",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_book_category_unique", columnNames = {"book_id", "category_id"})
    }
)
public class BookCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    protected BookCategory() {
    }

    public BookCategory(Long bookId, Long categoryId) {
        this.bookId = bookId;
        this.categoryId = categoryId;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}
