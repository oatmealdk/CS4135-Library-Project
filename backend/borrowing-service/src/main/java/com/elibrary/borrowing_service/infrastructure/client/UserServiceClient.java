package com.elibrary.borrowing_service.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Anti-Corruption Layer between the Borrowing context and the User & Authentication context.
 *
 * Only userId, exists, and isActive are extracted from the UserDTO response.
 *
 * calls here are wrapped with a circuit breaker and retry
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private static final String BASE_URL = "http://user-service/api/users";

    private final RestTemplate restTemplate;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * ACL — only what the Borrowing context needs from user-service rather than the entire BookDTO
     */
    public record UserValidation(Long userId, boolean exists, boolean isActive) {}

    @CircuitBreaker(name = "userService", fallbackMethod = "userValidationFallback")
    @Retry(name = "userService")
    public UserValidation validateUser(Long userId) {
        String url = BASE_URL + "/{userId}/exists";
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class, userId);
        if (response == null) {
            return new UserValidation(userId, false, false);
        }
        boolean exists   = Boolean.TRUE.equals(response.get("exists"));
        boolean isActive = Boolean.TRUE.equals(response.get("isActive"));
        return new UserValidation(userId, exists, isActive);
    }

    public UserValidation userValidationFallback(Long userId, Throwable t) {
        log.warn("user-service circuit open for userId={}: {}", userId, t.getMessage());
        // fail closed - deny borrow if user service is unreachable
        return new UserValidation(userId, false, false);
    }

    /** Name and email for admin fines UI (desk). */
    public record DeskProfile(String name, String email) {}

    /**
     * Uses a separate circuit breaker from {@link #validateUser} so borrow-validation failures
     * do not open the circuit for desk-profile reads (otherwise patron shows "Unavailable").
     */
    @CircuitBreaker(name = "userServiceDesk", fallbackMethod = "deskProfileFallback")
    @Retry(name = "userServiceDesk")
    public DeskProfile getDeskProfile(Long userId) {
        String url = BASE_URL + "/{userId}/desk-profile";
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class, userId);
        if (response == null) {
            return new DeskProfile("Unavailable", "—");
        }
        String name = response.get("name") != null ? String.valueOf(response.get("name")) : "—";
        String email = response.get("email") != null ? String.valueOf(response.get("email")) : "—";
        return new DeskProfile(name, email);
    }

    public DeskProfile deskProfileFallback(Long userId, Throwable t) {
        log.warn("user-service desk-profile failed for userId={}: {}", userId, t.getMessage());
        return new DeskProfile("Unavailable", "—");
    }
}
