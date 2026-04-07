package com.elibrary.borrowing_service.domain.service;

import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.Fine;
import com.elibrary.borrowing_service.domain.repository.FineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * This is the domain service that stores the fine calculation logic.
 *
 * I decided to do this here since fine calculation logic is important and I also couldn't decide which other class to put it in.
 *
 * Invariants enforced:
 *   I'll put the invariants that are enforced here later, since some of them will be enforced upstream in later classes.
 */
@Service
public class FineCalculationService {

    /** this value is taken from the config-server: borrowing.fine-grace-period-days, but will default to 1 if not set */
    @Value("${borrowing.fine-grace-period-days:1}")
    private int gracePeriodDays;

    private final FineRepository fineRepository;

    public FineCalculationService(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    /**
     * Evaluates whether the returned borrow record would incur a fine based on the grace period and expected return date.
     *
     * @param record a BorrowRecord whose returnBook() has already been called
     * @return the persisted Fine if one was warranted, or empty if returned within grace period
     */
    public Optional<Fine> calculateFine(BorrowRecord record) {
        if (record.getReturnDate() == null) {
            throw new IllegalStateException(
                "Cannot calculate fine: returnDate is null on record " + record.getRecordId());
        }

        LocalDate graceDeadline = record.getDueDate().plusDays(gracePeriodDays);

        if (!record.getReturnDate().isAfter(graceDeadline)) {
            return Optional.empty();
        }

        int chargeableDays = (int) (record.getReturnDate().toEpochDay() - graceDeadline.toEpochDay());

        // reject if an unpaid fine already exists for this record, this is to enforce INV-B7
        fineRepository.findByRecordIdAndIsPaidFalse(record.getRecordId()).ifPresent(existing -> {
            throw new IllegalStateException(
                "INV-B7: An unpaid fine already exists for record " + record.getRecordId());
        });

        Fine fine = Fine.create(record.getRecordId(), record.getUserId(), chargeableDays);
        return Optional.of(fineRepository.save(fine));
    }

    /**
     * Alias kept in line with the domain model spec.
     * Delegates directly to calculateFine.
     */
    public Optional<Fine> applyFine(BorrowRecord record) {
        return calculateFine(record);
    }
}
