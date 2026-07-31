package com.shrawan.resumebuilder.controller;

import com.shrawan.resumebuilder.dto.AuthResponse;
import com.shrawan.resumebuilder.dto.LoginRequest;
import com.shrawan.resumebuilder.dto.RegisterRequest;
import com.shrawan.resumebuilder.service.AuthService;
import com.shrawan.resumebuilder.service.FileUploadeService;
import com.shrawan.resumebuilder.util.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.shrawan.resumebuilder.util.AppConstants.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(AUTH_CONTROLLER)
public class AuthController {
    private final AuthService authService;
    private final FileUploadeService fileUploadeService;

    @PostMapping(REGISTER)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request){
        log.info("inside AuthController - register() : {}",request);

         AuthResponse response = authService.register(request);
         log.info("Response from Service: {}",response);
         return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }


    @GetMapping(VERIFY_EMAIL)
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        log.info("Inside AuthController - verifyEmail(): {}",token);
        authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body((Map.of("message", "Email verified successfully")));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        log.info("Inside AuthController - verifyOtp(): email={}, otp={}", email, otp);
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }
        AuthResponse response = authService.verifyOtp(email, otp);
        return ResponseEntity.ok(response);
    }

    @PostMapping(UPLOAD_PROFILE)
    public ResponseEntity<?> uploadImage(Authentication authentication, @RequestPart("image") MultipartFile file) throws IOException {
        log.info("Inside AuthController - uploadImage()");
        Map<String, String> response = fileUploadeService.uploadSingleImage(file);
        String imageUrl = response.get("imageUrl");
        if (authentication != null && authentication.getPrincipal() != null) {
            authService.updateProfileImage(authentication.getPrincipal(), imageUrl);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(LOGIN)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(RESEND_VERIFICATION)
    public ResponseEntity<?> resendVerification(@RequestBody Map<String,String> body){

        //get the email from requst
        String email = body.get("email");

        //add the validation
        if(Objects.isNull(email)){
            return ResponseEntity.badRequest().body(Map.of("message","Email is required"));
        }

        //call the service method to resend verification link
        authService.resendVerification(email);


        //return the response

        return ResponseEntity.ok(Map.of("sucess", true, "message" , "verification email sent"));

    }

    @GetMapping(PROFILE)
    public ResponseEntity<?> getProfile(Authentication authentication){
        //get the principal object
        Object principalObject = authentication.getPrincipal();
        //call the service method
        AuthResponse currentProfile = authService.getProfile(principalObject);
        //return the response
        return ResponseEntity.ok(currentProfile);
    }


}


























