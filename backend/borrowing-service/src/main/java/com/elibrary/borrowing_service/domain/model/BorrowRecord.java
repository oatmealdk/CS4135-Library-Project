package com.elibrary.borrowing_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * This class is the aggregate root for the Borrowing bounded context.
 *
 * All modifications to Fine and RenewalRecord entities are routed through
 * this root to preserve transactional consistency.
 *
 * Invariants that are enforced here:
 *   I'll put the invariants that are enforced here later, since some of them will be enforced upstream in later classes.
 */
@Entity
@Table(name = "borrow_record", schema = "borrowing")
public class BorrowRecord {

    public static final int MAX_RENEWALS = 3;
    public static final int LOAN_PERIOD_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @NotNull
    @Column(nullable = false)
    private Long userId;

    @NotNull
    @Column(nullable = false)
    private Long bookId;

    @NotNull
    @Column(nullable = false)
    private LocalDate borrowDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @PositiveOrZero
    @Column(nullable = false)
    private int renewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status = BorrowStatus.ACTIVE;

    protected BorrowRecord() {}

    /**
     * This method creates a new, ACTIVE borrow record.
     * Availability and user-existence checks are the responsibility of the
     * application service (BorrowingService) before calling this.
     */
    public static BorrowRecord create(Long userId, Long bookId) {
        BorrowRecord record = new BorrowRecord();
        record.userId = userId;
        record.bookId = bookId;
        record.borrowDate = LocalDate.now();
        record.dueDate = LocalDate.now().plusDays(LOAN_PERIOD_DAYS);
        record.status = BorrowStatus.ACTIVE;
        record.renewCount = 0;
        return record;
    }

    /**
     * Marks the record as RETURNED and sets returnDate to today's date.
     * Fines are calculated by the FineCalculationService class.
     */
    public BorrowRecord returnBook() {
        transitionTo(BorrowStatus.RETURNED);
        this.returnDate = LocalDate.now();
        return this;
    }

    /**
     * Extends the due date by LOAN_PERIOD_DAYS from today.
     *
     * @throws IllegalStateException if INV-B3 or INV-B4 are violated.
     */
    public BorrowRecord renewBook() {
        if (this.status == BorrowStatus.OVERDUE) {
            throw new IllegalStateException(
                "A book cannot be renewed when its status is OVERDUE.");
        }
        if (this.renewCount >= MAX_RENEWALS) {
            throw new IllegalStateException(
                "Maximum renewal limit of " + MAX_RENEWALS + " has been reached.");
        }
        transitionTo(BorrowStatus.RENEWED);
        this.dueDate = LocalDate.now().plusDays(LOAN_PERIOD_DAYS);
        this.renewCount++;
        return this;
    }

    /** Transitions status to OVERDUE. Called by the scheduled overdue detector. */
    public void markOverdue() {
        transitionTo(BorrowStatus.OVERDUE);
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate) &&
               (status == BorrowStatus.ACTIVE || status == BorrowStatus.RENEWED);
    }

    public int getDaysOverdue() {
        if (!isOverdue()) return 0;
        return (int) (LocalDate.now().toEpochDay() - dueDate.toEpochDay());
    }

    private void transitionTo(BorrowStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                "Invalid status transition from " + this.status + " to " + target);
        }
        this.status = target;
    }

    public Long getRecordId()    { return recordId; }
    public Long getUserId()      { return userId; }
    public Long getBookId()      { return bookId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate()    { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getRenewCount()   { return renewCount; }
    public BorrowStatus getStatus() { return status; }
}
