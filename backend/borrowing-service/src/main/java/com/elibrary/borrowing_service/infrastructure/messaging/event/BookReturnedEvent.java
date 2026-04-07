package com.elibrary.borrowing_service.infrastructure.messaging.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Published when a book is returned.
 * Routing key: borrowing.book.returned
 */
public class BookReturnedEvent {

    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate returnDate;
    private boolean wasOverdue;
    private LocalDateTime timestamp;

    public BookReturnedEvent() {}

    public BookReturnedEvent(Long recordId, Long userId, Long bookId,
                             LocalDate returnDate, boolean wasOverdue,
                             LocalDateTime timestamp) {
        this.recordId   = recordId;
        this.userId     = userId;
        this.bookId     = bookId;
        this.returnDate = returnDate;
        this.wasOverdue = wasOverdue;
        this.timestamp  = timestamp;
    }

    public Long getRecordId()           { return recordId; }
    public Long getUserId()             { return userId; }
    public Long getBookId()             { return bookId; }
    public LocalDate getReturnDate()    { return returnDate; }
    public boolean isWasOverdue()       { return wasOverdue; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
