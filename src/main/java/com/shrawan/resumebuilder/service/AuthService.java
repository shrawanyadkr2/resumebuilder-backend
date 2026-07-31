package com.shrawan.resumebuilder.service;

import com.shrawan.resumebuilder.document.User;
import com.shrawan.resumebuilder.dto.AuthResponse;
import com.shrawan.resumebuilder.dto.LoginRequest;
import com.shrawan.resumebuilder.dto.RegisterRequest;
import com.shrawan.resumebuilder.exception.ResourceExistsException;
import com.shrawan.resumebuilder.repository.UserRepository;
import com.shrawan.resumebuilder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.client.url:${app.base.url:https://skycodex.vercel.app}}")
    private String appClientUrl;

    public AuthResponse register(RegisterRequest request){
        log.info("Inside AuthService : register() {} ", request);

        if (userRepository.existsByEmail(request.getEmail())){
            throw new ResourceExistsException("User already exists with this email");
        }

        User newUser = toDocument(request);
        userRepository.save(newUser);

        sendVarificationEmail(newUser);

        return toResponse(newUser);
    }

    private void sendVarificationEmail(User newUser) {
        log.info("Inside AuthService - sendVarificationEmail(): {}", newUser);
        CompletableFuture.runAsync(() -> {
            try {
                String baseUrl = "https://skycodex.vercel.app";
                if (appClientUrl != null && !appClientUrl.isBlank() && !appClientUrl.contains("localhost") && !appClientUrl.contains("127.0.0.1")) {
                    baseUrl = appClientUrl.replaceAll("/+$", "");
                }
                String link = baseUrl + "/verify-email?token=" + newUser.getVerificationToken();
                log.info("Generated verification email link for {}: {}", newUser.getEmail(), link);
                String html = "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;'>" +
                                "<h2 style='color: #6366f1;'>Verify Your ResumeBuilder PRO Account</h2>" +
                                "<p>Hi <strong>" + newUser.getName() + "</strong>,</p>" +
                                "<p>Thank you for signing up for ResumeBuilder PRO! Please click the button below to confirm your email address and activate your account:</p>" +
                                "<p style='margin: 25px 0;'><a href='" + link + "' style='display: inline-block; padding: 12px 24px; background-color: #6366f1; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold;'>Verify Email Address</a></p>" +
                                "<p style='color: #666; font-size: 13px;'>Or copy and paste this link in your browser:<br/><a href='" + link + "'>" + link + "</a></p>" +
                                "<p style='color: #999; font-size: 12px; margin-top: 30px;'>This verification link expires in 24 hours.</p>" +
                                "</div>";
                emailService.sendHtmlEmail(newUser.getEmail(), "Verify your ResumeBuilder PRO email", html);
            } catch (Exception ex) {
                log.error("Email verification notification failed for user {}: {}", newUser.getEmail(), ex.getMessage(), ex);
            }
        });
    }

    private AuthResponse toResponse(User newUser){
        return AuthResponse.builder()
                .id(newUser.getId())
                .name(newUser.getName())
                .email(newUser.getEmail())
                .profileImageUrl(newUser.getProfileImageUrl())
                .emailVarified(newUser.isEmailVerified())
                .subscriptionPlan(newUser.getSubscriptionPlan())
                .token(newUser.getVerificationToken())
                .createdAt(newUser.getCreatedAt())
                .updatedAt(newUser.getUpdatedAt())
                .build();
    }

    private User toDocument(RegisterRequest request) {
        LocalDateTime now = LocalDateTime.now();

        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(now.plusHours(24))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void verifyEmail(String token) {
        log.info("inside AuthService - verifyEmail() : {}", token);
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Invalid verification token");
        }

        User user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null) {
            log.info("Verification token not found or already verified: {}", token);
            return;
        }

        if (user.getVerificationExpires() != null && user.getVerificationExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request){
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid Email and Password"));

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())){
            throw new UsernameNotFoundException("Invalid Email and Password");
        }

        if (!existingUser.isEmailVerified()){
            throw new RuntimeException("Please verify your email address before logging in.");
        }

        String token = jwtUtil.generateToken(existingUser.getId());

        AuthResponse response = toResponse(existingUser);
        response.setToken(token);

        return response;
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationExpires(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        sendVarificationEmail(user);
    }

    public AuthResponse getProfile(Object principal) {
        if (principal instanceof User) {
            return toResponse((User) principal);
        }
        throw new RuntimeException("User not authenticated");
    }

    public void updateProfileImage(Object principal, String imageUrl) {
        if (principal instanceof User) {
            User user = (User) principal;
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);
        }
    }
}
