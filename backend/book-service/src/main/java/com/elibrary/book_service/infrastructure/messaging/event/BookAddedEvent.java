package com.elibrary.book_service.infrastructure.messaging.event;

import java.time.LocalDateTime;

public record BookAddedEvent(
    Long bookId,
    String title,
    String author,
    String isbn,
    Integer totalCopies,
    LocalDateTime timestamp
) {
}
