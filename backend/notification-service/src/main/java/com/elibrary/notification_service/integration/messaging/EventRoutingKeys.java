package com.elibrary.notification_service.integration.messaging;

/**
 * Routing keys on the external {@code borrowing.events} exchange (published by borrowing-service).
 * Values must stay aligned with {@code borrowing-service} {@code RabbitMQConfig}.
 */
public final class EventRoutingKeys {
    private EventRoutingKeys() {}

    public static final String BOOK_BORROWED = "borrowing.book.borrowed";
    public static final String BOOK_RETURNED = "borrowing.book.returned";
    public static final String BOOK_RENEWED = "borrowing.book.renewed";
    public static final String BOOK_OVERDUE = "borrowing.book.overdue";
    public static final String FINE_APPLIED = "borrowing.fine.applied";
}
