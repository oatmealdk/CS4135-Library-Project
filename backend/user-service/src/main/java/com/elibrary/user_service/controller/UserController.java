package com.elibrary.user_service.controller;

import com.elibrary.user_service.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal API used by borrowing-service ({@code UserServiceClient}) to validate a user id
 * before creating a loan. No {@code active} flag exists on {@link com.elibrary.user_service.entity.User}
 * yet; any persisted user is treated as active.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<Map<String, Object>> exists(@PathVariable Long userId) {
        if (userRepository.existsById(userId)) {
            return ResponseEntity.ok(Map.of(
                "exists", true,
                "isActive", true
            ));
        }
        return ResponseEntity.ok(Map.of(
            "exists", false,
            "isActive", false
        ));
    }
}
