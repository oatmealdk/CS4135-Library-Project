package com.elibrary.borrowing_service.domain.model;

/**
 * Value object representing the lifecycle state of a BorrowRecord.
 *
 * Valid transitions (INV-B5):
 *   ACTIVE -> RETURNED
 *   ACTIVE -> OVERDUE
 *   ACTIVE -> RENEWED
 *   OVERDUE -> RETURNED
 *   RENEWED -> RETURNED
 *   RENEWED -> OVERDUE
 */
public enum BorrowStatus {
    ACTIVE,
    RETURNED,
    OVERDUE,
    RENEWED;

    /**
     * Returns true when the given target status is a legal state transition
     * from the state transitions we list in (INV-B5).
     */
    public boolean canTransitionTo(BorrowStatus target) {
        return switch (this) {
            case ACTIVE  -> target == RETURNED || target == OVERDUE || target == RENEWED;
            case OVERDUE -> target == RETURNED;
            case RENEWED -> target == RETURNED || target == OVERDUE;
            case RETURNED -> false;
        };
    }
}
