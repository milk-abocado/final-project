package com.example.finalproject.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForceLogoutRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
