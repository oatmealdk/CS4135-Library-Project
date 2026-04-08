package com.elibrary.borrowing_service.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BorrowRecord – domain invariants")
class BorrowRecordTest {

    private BorrowRecord record;

    @BeforeEach
    void setUp() {
        record = BorrowRecord.create(1L, 10L);
    }

    @Test
    @DisplayName("create() produces an ACTIVE record with correct dates")
    void create_producesActiveRecord() {
        assertEquals(BorrowStatus.ACTIVE, record.getStatus());
        assertEquals(LocalDate.now(), record.getBorrowDate());
        assertEquals(LocalDate.now().plusDays(BorrowRecord.LOAN_PERIOD_DAYS), record.getDueDate());
        assertNull(record.getReturnDate(), "returnDate must be null on creation (INV-B6)");
        assertEquals(0, record.getRenewCount());
    }

    @Test
    @DisplayName("returnBook() transitions status to RETURNED and sets returnDate (INV-B6)")
    void returnBook_setsStatusAndReturnDate() {
        record.returnBook();
        assertEquals(BorrowStatus.RETURNED, record.getStatus());
        assertEquals(LocalDate.now(), record.getReturnDate());
    }

    @Test
    @DisplayName("returnBook() on an already-RETURNED record throws (INV-B5)")
    void returnBook_throwsWhenAlreadyReturned() {
        record.returnBook();
        assertThrows(IllegalStateException.class, record::returnBook);
    }

    @Test
    @DisplayName("renewBook() increments renewCount, sets RENEWED status, and sets dueDate to today + loan period")
    void renewBook_incrementsCountAndExtendsDueDate() {
        record.renewBook();
        assertEquals(1, record.getRenewCount());
        assertEquals(BorrowStatus.RENEWED, record.getStatus());
        // New due date is always today + LOAN_PERIOD_DAYS — equal to original on same day, which is correct
        assertEquals(LocalDate.now().plusDays(BorrowRecord.LOAN_PERIOD_DAYS), record.getDueDate());
    }

    @Test
    @DisplayName("renewBook() throws after MAX_RENEWALS (INV-B3)")
    void renewBook_throwsAtMaxRenewals() {
        for (int i = 0; i < BorrowRecord.MAX_RENEWALS; i++) {
            record.renewBook();
        }
        IllegalStateException ex = assertThrows(IllegalStateException.class, record::renewBook);
        assertTrue(ex.getMessage().contains("Maximum renewal limit"),
            "Expected message to mention renewal limit, was: " + ex.getMessage());
    }

    @Test
    @DisplayName("renewBook() throws when status is OVERDUE (INV-B4)")
    void renewBook_throwsWhenOverdue() {
        record.markOverdue();
        IllegalStateException ex = assertThrows(IllegalStateException.class, record::renewBook);
        assertTrue(ex.getMessage().contains("OVERDUE"),
            "Expected message to mention OVERDUE, was: " + ex.getMessage());
    }

    @Test
    @DisplayName("markOverdue() transitions ACTIVE to OVERDUE (INV-B5)")
    void markOverdue_fromActive() {
        record.markOverdue();
        assertEquals(BorrowStatus.OVERDUE, record.getStatus());
    }

    @Test
    @DisplayName("markOverdue() transitions RENEWED to OVERDUE (INV-B5)")
    void markOverdue_fromRenewed() {
        record.renewBook();
        record.markOverdue();
        assertEquals(BorrowStatus.OVERDUE, record.getStatus());
    }

    @Test
    @DisplayName("markOverdue() on RETURNED record throws (INV-B5)")
    void markOverdue_throwsWhenReturned() {
        record.returnBook();
        assertThrows(IllegalStateException.class, record::markOverdue);
    }

    @Test
    @DisplayName("isOverdue() returns false for a freshly created record")
    void isOverdue_falseForNewRecord() {
        assertFalse(record.isOverdue());
    }

    @Test
    @DisplayName("getDaysOverdue() returns 0 when not overdue")
    void getDaysOverdue_zeroWhenNotOverdue() {
        assertEquals(0, record.getDaysOverdue());
    }
}
