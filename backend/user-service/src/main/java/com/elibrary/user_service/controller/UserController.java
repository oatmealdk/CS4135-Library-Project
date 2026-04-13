package com.elibrary.user_service.controller;

import com.elibrary.user_service.dto.AuthResponse;
import com.elibrary.user_service.dto.UpdateUserDetailsRequest;
import com.elibrary.user_service.entity.User;
import com.elibrary.user_service.repository.UserRepository;
import com.elibrary.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Internal API used by borrowing-service ({@code UserServiceClient}) to validate a user id
 * before creating a loan, and to show desk-facing patron details on fines. No password is exposed.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
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

    /**
     * Desk / borrowing-service: name and email for fines UI (no auth — same trust model as {@code /exists}).
     */
    @GetMapping("/{userId}/desk-profile")
    public ResponseEntity<Map<String, String>> deskProfile(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(u -> ResponseEntity.ok(Map.of(
                "name", u.getName(),
                "email", u.getEmail()
            )))
            .orElseGet(() -> ResponseEntity.ok(Map.of(
                "name", "Unknown patron",
                "email", "—"
            )));
    }

    @PutMapping("/me")
    public ResponseEntity<AuthResponse> updateCurrentUser(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateUserDetailsRequest request
    ) {
        User user = authService.getAuthenticatedUser(authHeader);
        boolean updated = false;

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
            updated = true;
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(encoder.encode(request.getPassword()));
            updated = true;
        }
        if (!updated) {
            throw new RuntimeException("No valid fields were provided to update");
        }

        User saved = userRepository.save(user);
        String token = authHeader.substring(7);
        return ResponseEntity.ok(new AuthResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole(),
                token,
                "User details updated successfully"
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteCurrentUser(@RequestHeader("Authorization") String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        try {
            authService.revokeToken(authHeader);
            userRepository.deleteById(user.getId());
        } catch (Exception ex) {
            throw new RuntimeException("Unable to delete account right now. Please try again.");
        }
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
