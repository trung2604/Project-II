package com.project2.ClassroomManagement.demo.dto;

import com.project2.ClassroomManagement.demo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String password;
    private User.Role role;
}