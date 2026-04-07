package com.elibrary.book_service.infrastructure.messaging.event;

import java.time.LocalDateTime;

public record BookAvailabilityChangedEvent(
    Long bookId,
    Integer previousAvailableCopies,
    Integer newAvailableCopies,
    LocalDateTime timestamp
) {
}
