package com.example.finalproject.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TokenRefreshRequest {
    @NotBlank
    private String refreshToken;
}