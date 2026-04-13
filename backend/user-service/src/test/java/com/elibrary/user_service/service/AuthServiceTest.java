package com.elibrary.user_service.service;

import com.elibrary.user_service.config.JwtUtil;
import com.elibrary.user_service.dto.LoginRequest;
import com.elibrary.user_service.dto.RegisterRequest;
import com.elibrary.user_service.entity.Role;
import com.elibrary.user_service.entity.User;
import com.elibrary.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_assignsStudentRoleAndHashesPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test Student");
        request.setEmail("student@studentmail.ul.ie");
        request.setPassword("Password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtUtil.generateToken(eq(10L), eq(request.getEmail()), eq(Role.STUDENT.name())))
            .thenReturn("jwt-token");

        var response = authService.register(request);

        assertEquals(Role.STUDENT, response.getRole());
        assertEquals("jwt-token", response.getToken());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotEquals("Password123", userCaptor.getValue().getPasswordHash());
    }

    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("staff@ul.ie");
        request.setPassword("wrong-password");

        User persisted = new User();
        persisted.setId(1L);
        persisted.setName("Staff User");
        persisted.setEmail("staff@ul.ie");
        persisted.setPasswordHash("$2a$10$k2L3h1pkR7QmJJ2A8iTaouNjjDUfE4vB6aGZX4caR8Jn6qxGNzc4a");
        persisted.setRole(Role.STAFF);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(persisted));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void getCurrentUser_throwsForMissingBearerHeader() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.getCurrentUser("bad-token"));
        assertEquals("Missing or invalid token", ex.getMessage());
    }

    @Test
    void validate_throwsWhenTokenIsRevoked() {
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractTokenId("valid-token")).thenReturn("token-id");
        when(tokenRevocationService.isRevoked("token-id")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.validate("Bearer valid-token"));
        assertEquals("Token has been revoked", ex.getMessage());
    }
}
