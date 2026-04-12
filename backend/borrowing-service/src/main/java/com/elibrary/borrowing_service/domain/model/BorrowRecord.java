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
 * Invariants enforced directly here:
 *   INV-B3: renewCount cannot exceed MAX_RENEWALS (enforced in renewBook)
 *   INV-B4: a book cannot be renewed if status is OVERDUE (enforced in renewBook)
 *   INV-B5: only valid BorrowStatus transitions are allowed (enforced in transitionTo)
 *   INV-B6: returnDate is null while ACTIVE or RENEWED; set on RETURNED (enforced in returnBook)
 *
 * Invariants enforced upstream:
 *   INV-B1: availableCopies > 0 checked in BorrowingService before create()
 *   INV-B2: fine only if returnDate > dueDate + grace period — FineCalculationService
 *   INV-B7: one unpaid fine per record — FineCalculationService
 *   INV-B8: amount = dailyRate x daysOverdue — Fine.create()
 *   INV-B9: max concurrent borrows — BorrowingService before create()
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
     * Extends the due date by {@link #LOAN_PERIOD_DAYS} from the current due date
     * (standard loan renewal: more time from the existing deadline, not a reset from today).
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
        // Only transition if coming from ACTIVE; a RENEWED record stays RENEWED
        // (RENEWED → RENEWED is not in the 6 valid transitions — status doesn't change, dates do)
        if (this.status == BorrowStatus.ACTIVE) {
            transitionTo(BorrowStatus.RENEWED);
        }
        this.dueDate = this.dueDate.plusDays(LOAN_PERIOD_DAYS);
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
