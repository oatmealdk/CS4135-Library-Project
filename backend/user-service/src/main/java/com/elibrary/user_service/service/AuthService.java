package com.elibrary.user_service.service;

import com.elibrary.user_service.config.JwtUtil;
import com.elibrary.user_service.dto.AuthResponse;
import com.elibrary.user_service.dto.RegisterRequest;
import com.elibrary.user_service.entity.Role;
import com.elibrary.user_service.entity.User;
import com.elibrary.user_service.exception.UnauthorizedException;
import com.elibrary.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.elibrary.user_service.dto.LoginRequest;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(encoder.encode(request.getPassword()));
        user.setRole(determineRole(request.getEmail()));

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return new AuthResponse(saved.getId(), saved.getName(),
                saved.getEmail(), saved.getRole(), token, "Registration successful");
    }

    private Role determineRole(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        if (domain.equals("studentmail.ul.ie")) {
            return Role.STUDENT;
        } else if (domain.equals("ul.ie") || domain.equals("lero.ie")) {
            return Role.STAFF;
        }
        throw new RuntimeException("Registration is only allowed with UL email addresses");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(user.getId(), user.getName(),
                user.getEmail(), user.getRole(), token, "Login successful");
    }

    public AuthResponse getCurrentUser(String authHeader) {
        String token = extractValidatedToken(authHeader);
        User user = getUserFromToken(token);

        return new AuthResponse(user.getId(), user.getName(),
                user.getEmail(), user.getRole(), token, "Authenticated");
    }

    public void logout(String authHeader) {
        revokeToken(authHeader);
    }

    public void validate(String authHeader) {
        extractValidatedToken(authHeader);
    }

    public User getAuthenticatedUser(String authHeader) {
        String token = extractValidatedToken(authHeader);
        return getUserFromToken(token);
    }

    public void revokeToken(String authHeader) {
        String token = extractValidatedToken(authHeader);
        tokenRevocationService.revoke(jwtUtil.extractTokenId(token), jwtUtil.extractExpiration(token));
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid token");
        }
        return authHeader.substring(7);
    }

    private void validateTokenOrThrow(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            throw new UnauthorizedException("Token is invalid or expired");
        }
        String tokenId = jwtUtil.extractTokenId(token);
        if (tokenRevocationService.isRevoked(tokenId)) {
            throw new UnauthorizedException("Token has been revoked");
        }
    }

    private String extractValidatedToken(String authHeader) {
        String token = extractBearerToken(authHeader);
        validateTokenOrThrow(token);
        return token;
    }

    private User getUserFromToken(String token) {
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}