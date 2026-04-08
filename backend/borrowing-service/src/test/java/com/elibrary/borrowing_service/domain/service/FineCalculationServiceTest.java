package com.elibrary.borrowing_service.domain.service;

import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.Fine;
import com.elibrary.borrowing_service.domain.repository.FineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FineCalculationService – fine logic and grace period")
class FineCalculationServiceTest {

    @Mock
    private FineRepository fineRepository;

    @InjectMocks
    private FineCalculationService fineCalculationService;

    @BeforeEach
    void setUp() {
        // mirror the value in config-server borrowing-service.properties
        ReflectionTestUtils.setField(fineCalculationService, "gracePeriodDays", 1);
    }

    @Test
    @DisplayName("No fine when book returned on the due date")
    void noFine_returnedOnDueDate() {
        BorrowRecord record = BorrowRecord.create(1L, 10L);
        record.returnBook(); // returnDate = today = borrowDate, which is <= dueDate

        Optional<Fine> result = fineCalculationService.calculateFine(record);

        assertTrue(result.isEmpty());
        verify(fineRepository, never()).save(any());
    }

    @Test
    @DisplayName("No fine when book returned on due date + 1 (within grace period, INV-B2)")
    void noFine_returnedWithinGracePeriod() {
        BorrowRecord record = BorrowRecord.create(1L, 10L);
        record.returnBook();
        // Simulate returnDate = dueDate + 1 (exactly at the grace boundary)
        ReflectionTestUtils.setField(record, "returnDate", record.getDueDate().plusDays(1));

        Optional<Fine> result = fineCalculationService.calculateFine(record);

        assertTrue(result.isEmpty());
        verify(fineRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fine applied when returned 3 days late - 2 chargeable days = €1.00 (Story 2)")
    void fine_threeDaysLate_twoChargeableDays() {
        BorrowRecord record = BorrowRecord.create(1L, 10L);
        record.returnBook();
        // 3 days past due date: chargeable = 3 - 1 (grace) = 2 days -> €1.00
        ReflectionTestUtils.setField(record, "returnDate", record.getDueDate().plusDays(3));

        Fine savedFine = Fine.create(record.getRecordId(), record.getUserId(), 2);
        when(fineRepository.findByRecordIdAndIsPaidFalse(any())).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenReturn(savedFine);

        Optional<Fine> result = fineCalculationService.calculateFine(record);

        assertTrue(result.isPresent());
        assertEquals(1.00, result.get().getAmount(), 0.001, "3 days late - 1 grace = 2 chargeable × €0.50 = €1.00");
        assertEquals(2, result.get().getDaysOverdue());
        verify(fineRepository).save(any(Fine.class));
    }

    @Test
    @DisplayName("Fine amount is always dailyRate × chargeableDays (INV-B8)")
    void fineAmount_matchesDailyRateTimesDays() {
        BorrowRecord record = BorrowRecord.create(1L, 10L);
        record.returnBook();
        // 6 days late: chargeable = 6 - 1 = 5 days → €2.50
        ReflectionTestUtils.setField(record, "returnDate", record.getDueDate().plusDays(6));

        Fine savedFine = Fine.create(record.getRecordId(), record.getUserId(), 5);
        when(fineRepository.findByRecordIdAndIsPaidFalse(any())).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenReturn(savedFine);

        Optional<Fine> result = fineCalculationService.calculateFine(record);

        assertTrue(result.isPresent());
        assertEquals(Fine.DAILY_RATE * 5, result.get().getAmount(), 0.001);
    }

    @Test
    @DisplayName("Throws if returnDate is null (book not yet returned)")
    void throwsWhenReturnDateIsNull() {
        BorrowRecord record = BorrowRecord.create(1L, 10L);
        // do NOT call returnBook() - returnDate stays null
        assertThrows(IllegalStateException.class,
            () -> fineCalculationService.calculateFine(record));
    }

    @Test
    @DisplayName("Throws when an unpaid fine already exists for the record (INV-B7)")
    void throwsWhenUnpaidFineAlreadyExists() {
        BorrowRecord record = BorrowRecord.create(1L, 10L);
        record.returnBook();
        ReflectionTestUtils.setField(record, "returnDate", record.getDueDate().plusDays(5));

        Fine existingFine = Fine.create(record.getRecordId(), record.getUserId(), 3);
        when(fineRepository.findByRecordIdAndIsPaidFalse(any())).thenReturn(Optional.of(existingFine));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> fineCalculationService.calculateFine(record));
        assertTrue(ex.getMessage().contains("INV-B7"));
    }
}
