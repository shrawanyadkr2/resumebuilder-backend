package com.shrawan.resumebuilder.service;

import com.shrawan.resumebuilder.document.User;
import com.shrawan.resumebuilder.dto.AuthResponse;
import com.shrawan.resumebuilder.dto.LoginRequest;
import com.shrawan.resumebuilder.dto.RegisterRequest;
import com.shrawan.resumebuilder.exception.ResourceExistsException;
import com.shrawan.resumebuilder.repository.UserRepository;
import com.shrawan.resumebuilder.util.JwtUtil;
import com.shrawan.resumebuilder.dto.SendPhoneOtpRequest;
import com.shrawan.resumebuilder.dto.VerifyPhoneOtpRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, PhoneOtpData> phoneOtpCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    public static class PhoneOtpData {
        private String otp;
        private LocalDateTime expiresAt;
    }

    public Map<String, Object> sendPhoneOtp(SendPhoneOtpRequest request) {
        log.info("Inside AuthService - sendPhoneOtp(): {}", request);
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceExistsException("User already exists with this email");
        }

        String phone = request.getPhoneNumber().trim();
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        phoneOtpCache.put(phone, new PhoneOtpData(otp, LocalDateTime.now().plusMinutes(10)));

        log.info("SMS OTP Generated for Mobile Phone {}: {}", phone, otp);

        return Map.of(
            "success", true,
            "message", "6-digit SMS OTP code sent to mobile number " + phone
        );
    }

    public AuthResponse verifyPhoneOtpAndRegister(VerifyPhoneOtpRequest request) {
        log.info("Inside AuthService - verifyPhoneOtpAndRegister(): phone={}", request.getPhoneNumber());
        String phone = request.getPhoneNumber().trim();
        PhoneOtpData otpData = phoneOtpCache.get(phone);

        if (otpData == null || !otpData.getOtp().equals(request.getOtp().trim())) {
            throw new RuntimeException("Invalid 6-digit SMS OTP code. Please check and try again.");
        }

        if (otpData.getExpiresAt().isBefore(LocalDateTime.now())) {
            phoneOtpCache.remove(phone);
            throw new RuntimeException("SMS OTP code has expired. Please request a new code.");
        }

        // OTP IS VERIFIED! NOW AND ONLY NOW DO WE INSERT THE USER INTO MONGODB!
        phoneOtpCache.remove(phone);

        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceExistsException("User already exists with this email");
        }

        LocalDateTime now = LocalDateTime.now();
        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(phone)
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(true)
                .phoneVerified(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(newUser);

        String jwtToken = jwtUtil.generateToken(newUser.getId());

        AuthResponse response = toResponse(newUser);
        response.setToken(jwtToken);

        return response;
    }

    public AuthResponse register(RegisterRequest request){
        log.info("Inside AuthService : register() {} ",request);

        if(userRepository.existsByEmail(request.getEmail())){
            throw new ResourceExistsException("User already exist with this email");
        }

        User newUser = toDocument(request);

        userRepository.save(newUser);

        String jwtToken = jwtUtil.generateToken(newUser.getId());

        AuthResponse response = toResponse(newUser);
        response.setToken(jwtToken);

        sendVarificationEmail(newUser);

        return response;
    }

   private void sendVarificationEmail(User newUser) {
        log.info("Inside AuthService - sendVarificationEmail(): {}", newUser);
        CompletableFuture.runAsync(() -> {
            try {
                String link = appClientUrl + "/verify-email?token=" + newUser.getVerificationToken();
                String html = "<div style='font-family:sans-serif'>" +
                                "<h2>Verify Your Account</h2>" +
                                "<p>Hi " + newUser.getName() + ", thank you for joining ResumeBuilder PRO.</p>" +
                                "<p><a href='" + link + "' style='display:inline-block;padding:10px 16px;background:#6366f1;color:#fff;border-radius:6px;text-decoration:none;'>Confirm Email Link</a></p>" +
                                "</div>";
                emailService.sendHtmlEmail(newUser.getEmail(), "Welcome to ResumeBuilder PRO", html);
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

        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(true)
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
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request){
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid Email and Password"));

        if(!passwordEncoder.matches(request.getPassword(), existingUser.getPassword() )){
            throw new UsernameNotFoundException("Invalid Email and Password");
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



















