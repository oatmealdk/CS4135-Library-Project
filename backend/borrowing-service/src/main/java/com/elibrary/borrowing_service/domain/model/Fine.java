package com.elibrary.borrowing_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entity representing a monetary penaltwhich represents a fine that's attached to a BorrowRecord (if the BorrowRecord is overdue)
 *
 * Lives within the BorrowRecord aggregate, so access should go through BorrowRecord
 *
 * Invariants that are enforced here:
 *   I'll put the invariants that are enforced here later, since some of them will be enforced upstream in later classes.
 */
@Entity
@Table(name = "fine", schema = "borrowing")
public class Fine {

    public static final double DAILY_RATE = 0.50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fineId;

    @NotNull
    @Column(nullable = false)
    private Long recordId;

    @NotNull
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private int daysOverdue;

    @Column(nullable = false)
    private double dailyRate;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private boolean isPaid = false;

    protected Fine() {}

    /**
     * Method to create a new instance of a Fine.
     *
     * @param recordId - the owning BorrowRecord
     * @param userId - the patron who owes the fine
     * @param daysOverdue - chargeable overdue days (already adjusted for grace period)
     */
    public static Fine create(Long recordId, Long userId, int daysOverdue) {
        if (daysOverdue <= 0) {
            throw new IllegalArgumentException(
                "Fine can only be created for a positive number of chargeable days overdue.");
        }
        Fine fine = new Fine();
        fine.recordId = recordId;
        fine.userId = userId;
        fine.daysOverdue = daysOverdue;
        fine.dailyRate = DAILY_RATE;
        fine.amount = fine.calculateAmount();
        fine.issuedAt = LocalDateTime.now();
        fine.isPaid = false;
        return fine;
    }

    public double calculateAmount() {
        return dailyRate * daysOverdue;
    }

    public void markAsPaid() {
        this.isPaid = true;
        this.paidAt = LocalDateTime.now();
    }

    public double getOutstandingBalance() {
        return isPaid ? 0.0 : amount;
    }

    public Long getFineId()          { return fineId; }
    public Long getRecordId()        { return recordId; }
    public Long getUserId()          { return userId; }
    public double getAmount()        { return amount; }
    public int getDaysOverdue()      { return daysOverdue; }
    public double getDailyRate()     { return dailyRate; }
    public LocalDateTime getIssuedAt()  { return issuedAt; }
    public LocalDateTime getPaidAt()    { return paidAt; }
    public boolean isPaid()          { return isPaid; }
}
