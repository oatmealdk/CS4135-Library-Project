package com.elibrary.borrowing_service.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BorrowStatus - valid transitions (INV-B5)")
class BorrowStatusTest {

    @Test void active_canTransitionTo_returned()  { assertTrue(BorrowStatus.ACTIVE.canTransitionTo(BorrowStatus.RETURNED)); }
    @Test void active_canTransitionTo_overdue()   { assertTrue(BorrowStatus.ACTIVE.canTransitionTo(BorrowStatus.OVERDUE)); }
    @Test void active_canTransitionTo_renewed()   { assertTrue(BorrowStatus.ACTIVE.canTransitionTo(BorrowStatus.RENEWED)); }

    @Test void overdue_canTransitionTo_returned() { assertTrue(BorrowStatus.OVERDUE.canTransitionTo(BorrowStatus.RETURNED)); }
    @Test void overdue_cannotTransitionTo_renewed(){ assertFalse(BorrowStatus.OVERDUE.canTransitionTo(BorrowStatus.RENEWED)); }

    @Test void renewed_canTransitionTo_returned() { assertTrue(BorrowStatus.RENEWED.canTransitionTo(BorrowStatus.RETURNED)); }
    @Test void renewed_canTransitionTo_overdue()  { assertTrue(BorrowStatus.RENEWED.canTransitionTo(BorrowStatus.OVERDUE)); }

    @Test void returned_cannotTransitionToAnything() {
        for (BorrowStatus target : BorrowStatus.values()) {
            assertFalse(BorrowStatus.RETURNED.canTransitionTo(target),
                "RETURNED should not transition to " + target);
        }
    }

    @Test void active_cannotTransitionTo_active() { assertFalse(BorrowStatus.ACTIVE.canTransitionTo(BorrowStatus.ACTIVE)); }
}
