package com.elibrary.borrowing_service.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Anti-Corruption Layer between the Borrowing context and the Book Catalogue context.
 *
 * Only the fields the Borrowing context actually needs are extracted from the
 * BookDTO response, so bookId, available, availableCopies. This ensures that changes to the internal
 * Book model in book-service will not affect this context.
 *
 * The calls here are wrapped with a circuit breaker and retry.
 * Circuit breaker config lives in the config server to preserve the separation of concerns
 */
@Component
public class BookServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BookServiceClient.class);
    private static final String BASE_URL = "http://book-service/api/books";

    private final RestTemplate restTemplate;

    public BookServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * ACL - only what the Borrowing context needs from book-service rather than the entire BookDTO
     */
    public record BookAvailability(Long bookId, boolean available, int availableCopies) {}

    @CircuitBreaker(name = "bookService", fallbackMethod = "availabilityFallback")
    @Retry(name = "bookService")
    public BookAvailability checkAvailability(Long bookId) {
        String url = BASE_URL + "/{bookId}/availability";
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class, bookId);
        if (response == null) {
            return new BookAvailability(bookId, false, 0);
        }
        boolean available = Boolean.TRUE.equals(response.get("available"));
        int copies = response.get("availableCopies") instanceof Number n ? n.intValue() : 0;
        return new BookAvailability(bookId, available, copies);
    }

    public BookAvailability availabilityFallback(Long bookId, Throwable t) {
        log.warn("book-service circuit open for availability check on bookId={}: {}", bookId, t.getMessage());
        return new BookAvailability(bookId, false, 0);
    }

    @CircuitBreaker(name = "bookService", fallbackMethod = "decrementFallback")
    @Retry(name = "bookService")
    public void decrementCopies(Long bookId) {
        restTemplate.put(BASE_URL + "/{bookId}/decrement-copies", null, bookId);
    }

    public void decrementFallback(Long bookId, Throwable t) {
        log.error("CRITICAL: could not decrement copies for bookId={} — book-service unavailable: {}", bookId, t.getMessage());
        throw new IllegalStateException("Book service unavailable. Borrow operation cannot complete.");
    }

    @CircuitBreaker(name = "bookService", fallbackMethod = "incrementFallback")
    @Retry(name = "bookService")
    public void incrementCopies(Long bookId) {
        restTemplate.put(BASE_URL + "/{bookId}/increment-copies", null, bookId);
    }

    public void incrementFallback(Long bookId, Throwable t) {
        //  book has been returned, copy count update can be retried.
        log.error("Could not increment copies for bookId={}, this will require manual correction: {}", bookId, t.getMessage());
    }
}
