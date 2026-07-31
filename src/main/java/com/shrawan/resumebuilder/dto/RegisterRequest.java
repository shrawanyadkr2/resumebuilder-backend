package com.shrawan.resumebuilder.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import javax.crypto.Mac;

@Data
public class RegisterRequest {

    @Email(message = "Email Should be Valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Name is Required")
    @Size(min = 6, max = 20, message = "name should be int the range of 6 and 20 charctores")
    private String name;

    @NotBlank(message = "Passwod is required")
    @Size(min = 6 , max = 15 , message = "password should be in the range of 6 to 15 charactor")
    private String password;
    private String profileImageUrl;
}
