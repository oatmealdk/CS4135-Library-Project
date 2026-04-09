package com.elibrary.book_service.infrastructure.messaging.event;

import java.time.LocalDateTime;
import java.io.Serializable;

public record BookAddedEvent(
    Long bookId,
    String title,
    String author,
    String isbn,
    Integer totalCopies,
    LocalDateTime timestamp
) implements Serializable {
}
