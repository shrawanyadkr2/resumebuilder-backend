package com.shrawan.resumebuilder.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String name;
    private String email;
    private String password;
    private String profileImageUrl;
    private String phoneNumber;
    private String subscriptionPlan = "Basic";
    private boolean emailVerified = false;
    private boolean phoneVerified = false;
    private String verificationToken;
    private String otpCode;
    private LocalDateTime otpExpires;

    private LocalDateTime verificationExpires;
    @CreatedDate
    private LocalDateTime createdAt;
    // ✅ CORRECT
    @LastModifiedDate
    private LocalDateTime updatedAt;

}