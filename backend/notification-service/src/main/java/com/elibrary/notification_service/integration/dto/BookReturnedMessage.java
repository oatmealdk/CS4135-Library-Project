package com.elibrary.notification_service.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BookReturnedMessage {
    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate returnDate;
    private boolean wasOverdue;
    private LocalDateTime timestamp;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public boolean isWasOverdue() { return wasOverdue; }
    public void setWasOverdue(boolean wasOverdue) { this.wasOverdue = wasOverdue; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
