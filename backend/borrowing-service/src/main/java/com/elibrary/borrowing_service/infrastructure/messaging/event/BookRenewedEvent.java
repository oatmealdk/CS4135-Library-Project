package com.elibrary.borrowing_service.infrastructure.messaging.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Published when a borrow record is renewed.
 * Routing key: borrowing.book.renewed
 */
public class BookRenewedEvent {

    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate newDueDate;
    private int renewCount;
    private LocalDateTime timestamp;

    public BookRenewedEvent() {}

    public BookRenewedEvent(Long recordId, Long userId, Long bookId,
                            LocalDate newDueDate, int renewCount,
                            LocalDateTime timestamp) {
        this.recordId   = recordId;
        this.userId     = userId;
        this.bookId     = bookId;
        this.newDueDate = newDueDate;
        this.renewCount = renewCount;
        this.timestamp  = timestamp;
    }

    public Long getRecordId()           { return recordId; }
    public Long getUserId()             { return userId; }
    public Long getBookId()             { return bookId; }
    public LocalDate getNewDueDate()    { return newDueDate; }
    public int getRenewCount()          { return renewCount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
