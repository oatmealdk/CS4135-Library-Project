package com.elibrary.notification_service.domain.model;

public enum NotificationType {
    BOOK_BORROWED,
    BOOK_RETURNED,
    BOOK_RENEWED,
    BOOK_OVERDUE,
    FINE_APPLIED,
    /** Due in approximately one week (scheduled reminder). */
    DUE_SOON_WEEK,
    /** Due tomorrow / next day (scheduled reminder). */
    DUE_SOON_DAY,
    REMINDER
}
