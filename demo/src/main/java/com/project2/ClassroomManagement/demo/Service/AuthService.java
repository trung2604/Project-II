package com.project2.ClassroomManagement.demo.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.project2.ClassroomManagement.demo.dto.AuthResponse;
import com.project2.ClassroomManagement.demo.dto.GoogleLoginRequest;
import com.project2.ClassroomManagement.demo.dto.LoginRequest;
import com.project2.ClassroomManagement.demo.dto.RegisterRequest;
import com.project2.ClassroomManagement.demo.entity.User;
import com.project2.ClassroomManagement.demo.entity.User.AuthProvider;
import com.project2.ClassroomManagement.demo.repository.UserRepository;
import com.project2.ClassroomManagement.demo.security.jwt.JwtTokenProvider;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    // Store verification codes with their expiration times
    private final Map<String, VerificationData> verificationCodes = new HashMap<>();

    // Class to store verification data
    private static class VerificationData {
        private final String code;
        private final long expirationTime;
        private final String email;

        public VerificationData(String code, String email) {
            this.code = code;
            this.email = email;
            // Code expires after 10 minutes
            this.expirationTime = System.currentTimeMillis() + (10 * 60 * 1000);
        }

        public boolean isValid() {
            return System.currentTimeMillis() < expirationTime;
        }
    }

    // Generate a random 6-digit verification code
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit number between 100000 and 999999
        return String.valueOf(code);
    }

    @Transactional(rollbackOn = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setRole(request.getRole());
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        String verificationCode = generateVerificationCode();
        verificationCodes.put(user.getEmail(), new VerificationData(verificationCode, user.getEmail()));

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationCode);
        } catch (MessagingException e) {
            logger.error("Không thể gửi email xác minh cho người dùng {}: {}", savedUser.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email xác minh. Vui lòng thử lại sau.", e);
        }

        String jwt = tokenProvider.generateToken(savedUser);
        return new AuthResponse(jwt, savedUser.getEmail(), savedUser.getRole().toString(), false,
                "Vui lòng xác minh email của bạn để hoàn tất đăng ký");
    }

    public boolean verifyEmailWithCode(String email, String code) {
        VerificationData data = verificationCodes.get(email);

        if (data != null && data.isValid() && data.code.equals(code)) {
            User user = userRepository.findByEmail(email);
            if (user != null && !user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
                verificationCodes.remove(email); // Remove the used code
                return true;
            }
        }
        return false;
    }
    
    public boolean resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || user.isEmailVerified()) {
            return false;
        }
        
        String verificationCode = generateVerificationCode();
        verificationCodes.put(email, new VerificationData(verificationCode, email));
        
        try {
            emailService.sendVerificationEmail(email, verificationCode);
            return true;
        } catch (MessagingException e) {
            logger.error("Không thể gửi lại mã xác minh cho người dùng {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String jwt = tokenProvider.generateToken(user);

        return new AuthResponse(
                jwt,
                user.getEmail(),
                user.getRole().toString(),
                user.isEmailVerified()
        );
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getGoogleToken());
            if (idToken == null) {
                throw new RuntimeException("Invalid Google token");
            }

            Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            User user = userRepository.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setEmailVerified(true);
                user.setProvider(AuthProvider.GOOGLE);
                user.setRole(User.Role.ROLE_STUDENT);
                user = userRepository.save(user);
            }

            String jwt = tokenProvider.generateToken(user);

            return new AuthResponse(
                    jwt,
                    user.getEmail(),
                    user.getRole().toString(),
                    user.isEmailVerified()
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Error verifying Google token", e);
        }
    }
}
