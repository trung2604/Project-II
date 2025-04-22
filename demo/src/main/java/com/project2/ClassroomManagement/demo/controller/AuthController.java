package com.project2.ClassroomManagement.demo.controller;

import com.project2.ClassroomManagement.demo.dto.*;
import com.project2.ClassroomManagement.demo.Service.AuthService;
import com.project2.ClassroomManagement.demo.entity.User;
import com.project2.ClassroomManagement.demo.repository.UserRepository;
import com.project2.ClassroomManagement.demo.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Email đã được sử dụng")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Email đã được sử dụng"));
            } else if (e.getMessage().equals("Không thể gửi email xác minh. Vui lòng thử lại sau.")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("Không thể gửi email xác minh. Vui lòng thử lại sau."));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Đã xảy ra lỗi trong quá trình đăng ký"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerificationRequest request) {
        try {
            boolean verified = authService.verifyEmailWithCode(request.getEmail(), request.getCode());
            
            if (verified) {
                // Get updated user information to return
                User user = userRepository.findByEmail(request.getEmail());
                String jwt = tokenProvider.generateToken(user);
                
                return ResponseEntity.ok(new AuthResponse(
                    jwt,
                    user.getEmail(),
                    user.getRole().toString(),
                    true,
                    "Xác minh email thành công! Đăng ký của bạn đã hoàn tất."
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new EmailVerificationResponse("Mã xác minh không hợp lệ hoặc đã hết hạn", false));
            }
        } catch (Exception e) {
            logger.error("Error verifying code: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new EmailVerificationResponse("Lỗi xác minh email: " + e.getMessage(), false));
        }
    }
    
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendVerificationCode(@RequestBody EmailRequest request) {
        boolean sent = authService.resendVerificationCode(request.getEmail());
        if (sent) {
            return ResponseEntity.ok(new EmailVerificationResponse("Mã xác nhận mới đã được gửi đến email của bạn", true));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new EmailVerificationResponse("Không thể gửi lại mã xác nhận", false));
        }
    }
}
