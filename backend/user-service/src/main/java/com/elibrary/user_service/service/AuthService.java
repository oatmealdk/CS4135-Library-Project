package com.elibrary.user_service.service;

import com.elibrary.user_service.config.JwtUtil;
import com.elibrary.user_service.dto.AuthResponse;
import com.elibrary.user_service.dto.RegisterRequest;
import com.elibrary.user_service.entity.Role;
import com.elibrary.user_service.entity.User;
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
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid token");
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(user.getId(), user.getName(),
                user.getEmail(), user.getRole(), token, "Authenticated");
    }

}