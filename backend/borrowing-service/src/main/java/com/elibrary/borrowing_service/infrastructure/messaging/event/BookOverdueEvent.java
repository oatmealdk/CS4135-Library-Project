package com.elibrary.borrowing_service.infrastructure.messaging.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Published daily by OverdueScheduler for each record that has passed its due date.
 * Routing key: borrowing.book.overdue
 */
public class BookOverdueEvent {

    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate dueDate;
    private int daysOverdue;
    private LocalDateTime timestamp;

    public BookOverdueEvent() {}

    public BookOverdueEvent(Long recordId, Long userId, Long bookId,
                            LocalDate dueDate, int daysOverdue,
                            LocalDateTime timestamp) {
        this.recordId   = recordId;
        this.userId     = userId;
        this.bookId     = bookId;
        this.dueDate    = dueDate;
        this.daysOverdue = daysOverdue;
        this.timestamp  = timestamp;
    }

    public Long getRecordId()           { return recordId; }
    public Long getUserId()             { return userId; }
    public Long getBookId()             { return bookId; }
    public LocalDate getDueDate()       { return dueDate; }
    public int getDaysOverdue()         { return daysOverdue; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
