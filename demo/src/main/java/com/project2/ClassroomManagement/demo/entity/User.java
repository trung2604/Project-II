package com.project2.ClassroomManagement.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "app_user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    public enum Role {
        ROLE_TEACHER, ROLE_STUDENT
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }
}