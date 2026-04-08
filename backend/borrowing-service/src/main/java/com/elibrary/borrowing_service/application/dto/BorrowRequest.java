package com.elibrary.borrowing_service.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for POST /api/borrows.
 * Only userId and bookId are accepted, again enforcing ACL.
 */
public class BorrowRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "bookId is required")
    private Long bookId;

    public BorrowRequest() {}

    public BorrowRequest(Long userId, Long bookId) {
        this.userId = userId;
        this.bookId = bookId;
    }

    public Long getUserId() { return userId; }
    public Long getBookId() { return bookId; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
}
