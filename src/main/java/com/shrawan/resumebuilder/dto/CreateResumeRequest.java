package com.shrawan.resumebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class CreateResumeRequest {

    @NotBlank(message = "title is required")
    private String title;


}
