package com.elibrary.book_service.infrastructure.messaging.event;

import java.time.LocalDateTime;

public record BookRemovedEvent(
    Long bookId,
    String title,
    String removalType,
    LocalDateTime timestamp
) {
}
