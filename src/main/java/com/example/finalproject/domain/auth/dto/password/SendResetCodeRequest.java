package com.example.finalproject.domain.auth.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendResetCodeRequest(
        @NotBlank @Email
        String email
) {}
