package com.example.finalproject.domain.auth.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyResetCodeRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "코드는 6자리 숫자여야 합니다.")
        String code
) {}