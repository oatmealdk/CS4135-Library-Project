package com.elibrary.user_service.dto;

import jakarta.validation.constraints.Size;

public class UpdateUserDetailsRequest {

    @Size(min = 1, message = "Name cannot be empty")
    private String name;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
