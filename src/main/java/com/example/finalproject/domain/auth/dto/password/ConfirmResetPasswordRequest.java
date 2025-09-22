package com.example.finalproject.domain.auth.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmResetPasswordRequest(
        @NotBlank @Email
        String email,

        // 6자리 숫자 코드
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "코드는 6자리 숫자여야 합니다.")
        String code,

        // 새 비밀번호 (정책에 맞게 길이만 제한; 필요하면 추가 패턴 적용)
        @NotBlank
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
        String newPassword
) {}