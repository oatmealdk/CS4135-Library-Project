package com.elibrary.user_service.dto;

import com.elibrary.user_service.entity.Role;

public class AuthResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String token;
    private String message;

    public AuthResponse(Long id, String name, String email, Role role, String token, String message) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.token = token;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getToken() { return token; }
    public String getMessage() { return message; }
}