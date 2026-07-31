package com.shrawan.resumebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendPhoneOtpRequest {
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    private String email;
}
