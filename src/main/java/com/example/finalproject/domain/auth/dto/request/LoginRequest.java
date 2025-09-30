package com.example.finalproject.domain.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @Email @NotBlank private String email;
    @NotBlank private String password; }