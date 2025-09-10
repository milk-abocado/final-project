package com.example.finalproject.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerifyRequest {
    @Email @NotBlank private String email;
    @NotBlank private String code; }