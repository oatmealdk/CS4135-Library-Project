package com.elibrary.borrowing_service.infrastructure.messaging.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Published when a BorrowRecord is created.
 * Routing key: borrowing.book.borrowed
 */
public class BookBorrowedEvent {

    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate dueDate;
    private LocalDateTime timestamp;

    public BookBorrowedEvent() {}

    public BookBorrowedEvent(Long recordId, Long userId, Long bookId,
                             LocalDate dueDate, LocalDateTime timestamp) {
        this.recordId  = recordId;
        this.userId    = userId;
        this.bookId    = bookId;
        this.dueDate   = dueDate;
        this.timestamp = timestamp;
    }

    public Long getRecordId()          { return recordId; }
    public Long getUserId()            { return userId; }
    public Long getBookId()            { return bookId; }
    public LocalDate getDueDate()      { return dueDate; }
    public LocalDateTime getTimestamp(){ return timestamp; }
}
