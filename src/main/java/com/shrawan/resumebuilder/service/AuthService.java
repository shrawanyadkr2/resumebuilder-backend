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

        return toResponse(newUser);
    }

   private void sendVarificationEmail(User newUser) {
        log.info("Inside AuthService - sendVarificationEmail(): {}", newUser);
        CompletableFuture.runAsync(() -> {
            try {
                String link = appClientUrl + "/verify-email?token=" + newUser.getVerificationToken();
                String html = "<div style='font-family:sans-serif'>" +
                                "<h2>Verify your email</h2>" +
                                "<p>Hi " + newUser.getName() + ", please confirm your email to activate your account.</p>" +
                                "<p><a href='" + link + "' style='display:inline-block;padding:10px 16px;background:#6366f1;color:#fff;border-radius:6px;text-decoration:none;'>Verify Email</a></p>" +
                                "<p>Or copy this link: " + link + "</p>" +
                                "<p>This link expires in 24 hours.</p>" +
                                "</div>";
                emailService.sendHtmlEmail(newUser.getEmail(), "verify your email", html);
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
                .token(newUser.getVerificationToken()) // Added this line!
                .createdAt(newUser.getCreatedAt())
                .updatedAt(newUser.getUpdatedAt())
                .build();
    }

    private User toDocument(RegisterRequest request) {
        LocalDateTime now = LocalDateTime.now(); // Store current time in a single variable

        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(now.plusHours(24))
                .createdAt(now) // Set to registration time
                .updatedAt(now) // Set to registration time (same as createdAt)
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
        user.setUpdatedAt(LocalDateTime.now()); // Changes updatedAt to the new verification time!

        userRepository.save(user); // createdAt stays untouched in MongoDB
    }

    public AuthResponse login(LoginRequest request){
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid Email and Password"));

        if(!passwordEncoder.matches(request.getPassword(), existingUser.getPassword() )){
            throw new UsernameNotFoundException("Invalid Email and Password");
        }

        if(!existingUser.isEmailVerified()){
            throw new RuntimeException("Please verify your email address before logging in.");
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



















