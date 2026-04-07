package com.elibrary.borrowing_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a single renewal event for a BorrowRecord.
 *
 * Lives within the BorrowRecord aggregate.
 */
@Entity
@Table(name = "renewal_record", schema = "borrowing")
public class RenewalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long renewalId;

    @NotNull
    @Column(nullable = false)
    private Long recordId;

    @NotNull
    @Column(nullable = false)
    private LocalDate previousDueDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate newDueDate;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime renewedAt;

    protected RenewalRecord() {}

    public static RenewalRecord create(Long recordId,
                                       LocalDate previousDueDate,
                                       LocalDate newDueDate) {
        RenewalRecord renewal = new RenewalRecord();
        renewal.recordId = recordId;
        renewal.previousDueDate = previousDueDate;
        renewal.newDueDate = newDueDate;
        renewal.renewedAt = LocalDateTime.now();
        return renewal;
    }

    public RenewalRecord getRenewalDetails() {
        return this;
    }

    public Long getRenewalId()             { return renewalId; }
    public Long getRecordId()              { return recordId; }
    public LocalDate getPreviousDueDate()  { return previousDueDate; }
    public LocalDate getNewDueDate()       { return newDueDate; }
    public LocalDateTime getRenewedAt()    { return renewedAt; }
}
