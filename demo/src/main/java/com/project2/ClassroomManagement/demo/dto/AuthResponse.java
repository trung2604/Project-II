package com.project2.ClassroomManagement.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private boolean emailVerified;
    private String message;

    public AuthResponse(String token, String email, String role, boolean emailVerified) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.emailVerified = emailVerified;
        this.message = null;
    }

    public AuthResponse(String token, String email, String role, boolean emailVerified, String message) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.emailVerified = emailVerified;
        this.message = message;
    }
}
