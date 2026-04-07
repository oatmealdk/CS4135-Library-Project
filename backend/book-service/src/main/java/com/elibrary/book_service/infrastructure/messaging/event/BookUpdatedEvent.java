package com.elibrary.book_service.infrastructure.messaging.event;

import java.time.LocalDateTime;
import java.util.Map;

public record BookUpdatedEvent(
    Long bookId,
    Map<String, Object> changedFields,
    LocalDateTime timestamp
) {
}
