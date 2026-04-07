package com.elibrary.borrowing_service.infrastructure.messaging.event;

import java.time.LocalDateTime;

/**
 * Published when a fine is calculated and applied to a borrow record.
 * Routing key: borrowing.fine.applied
 */
public class FineAppliedEvent {

    private Long fineId;
    private Long recordId;
    private Long userId;
    private double amount;
    private int daysOverdue;
    private LocalDateTime timestamp;

    public FineAppliedEvent() {}

    public FineAppliedEvent(Long fineId, Long recordId, Long userId,
                            double amount, int daysOverdue,
                            LocalDateTime timestamp) {
        this.fineId      = fineId;
        this.recordId    = recordId;
        this.userId      = userId;
        this.amount      = amount;
        this.daysOverdue = daysOverdue;
        this.timestamp   = timestamp;
    }

    public Long getFineId()             { return fineId; }
    public Long getRecordId()           { return recordId; }
    public Long getUserId()             { return userId; }
    public double getAmount()           { return amount; }
    public int getDaysOverdue()         { return daysOverdue; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
