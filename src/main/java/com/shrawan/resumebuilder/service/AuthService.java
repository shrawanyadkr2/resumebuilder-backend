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

    @Value("${app.client.url:${app.base.url:http://localhost:5173}}")
    private String appClientUrl;

    public AuthResponse register(RegisterRequest request){
        log.info("Inside AuthService : register() {} ",request);

        if(userRepository.existsByEmail(request.getEmail())){
            throw new ResourceExistsException("User already exist with this email");
        }

        User newUser = toDocument(request);

        userRepository.save(newUser);

        sendVarificationEmail(newUser);

        AuthResponse response = toResponse(newUser);
        return response;
    }

   private void sendVarificationEmail(User newUser) {
        log.info("Inside AuthService - sendVarificationEmail(): {}", newUser);
        CompletableFuture.runAsync(() -> {
            try {
                String link = appClientUrl + "/verify-email?token=" + newUser.getVerificationToken();
                String html = "<div style='font-family:sans-serif'>" +
                                "<h2>Verify Your Account</h2>" +
                                "<p>Hi " + newUser.getName() + ", your 6-digit OTP verification code is:</p>" +
                                "<h1 style='color:#6366f1;letter-spacing:4px;'>" + newUser.getOtpCode() + "</h1>" +
                                "<p>Or click this link: <a href='" + link + "'>Verify Email</a></p>" +
                                "<p>This code expires in 15 minutes.</p>" +
                                "</div>";
                emailService.sendHtmlEmail(newUser.getEmail(), "Your OTP Code: " + newUser.getOtpCode(), html);
            } catch (Exception ex) {
                log.error("SMTP email notification failed for user {}: {}", newUser.getEmail(), ex.getMessage(), ex);
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
        String randomOtp = String.format("%06d", new java.util.Random().nextInt(999999));

        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(false)
                .otpCode(randomOtp)
                .otpExpires(now.plusMinutes(15))
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(now.plusHours(24))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void verifyEmail(String token) {
        log.info("inside AuthService - verifyEmail() : {}",token);
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or Expired validation Token"));

        if (user.getVerificationExpires() != null && user.getVerificationExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        user.setOtpCode(null);
        user.setOtpExpires(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public AuthResponse verifyOtp(String email, String otpCode) {
        log.info("Inside AuthService - verifyOtp(): email={}, otp={}", email, otpCode);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.getOtpCode() != null && user.getOtpCode().equals(otpCode.trim())) {
            user.setEmailVerified(true);
            user.setOtpCode(null);
            user.setOtpExpires(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } else if (!user.isEmailVerified()) {
            throw new RuntimeException("Invalid OTP code. Please check and try again.");
        }

        String token = jwtUtil.generateToken(user.getId());
        AuthResponse response = toResponse(user);
        response.setToken(token);
        return response;
    }

    public AuthResponse login(LoginRequest request){
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid Email and Password"));

        if(!passwordEncoder.matches(request.getPassword(), existingUser.getPassword() )){
            throw new UsernameNotFoundException("Invalid Email and Password");
        }

        if(!existingUser.isEmailVerified()){
            throw new RuntimeException("Please verify your 6-digit OTP code before logging in.");
        }

        String token = jwtUtil.generateToken(existingUser.getId());

        AuthResponse response = toResponse(existingUser);
        response.setToken(token);

        return response;
    }

    public void resendVerification(String email) {

        //find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new RuntimeException("User not found"));


        //check the email is verified

        if(user.isEmailVerified()){
            throw new RuntimeException("Email is already verified.");
        }
        //set the new verification token and expires time
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationExpires(LocalDateTime.now().plusHours(24));

        //update the token

        userRepository.save(user);
        // resend the verification email

        sendVarificationEmail(user);


    }

    public AuthResponse getProfile(Object principalObject) {
        User existingUser = (User) principalObject;
        // Fetch fresh state from MongoDB
        User freshUser = userRepository.findById(existingUser.getId()).orElse(existingUser);
        return toResponse(freshUser);
    }

    public AuthResponse updateProfileImage(Object principalObject, String imageUrl) {
        User user = (User) principalObject;
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setProfileImageUrl(imageUrl);
        existingUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(existingUser);
        return toResponse(existingUser);
    }
}



















