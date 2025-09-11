package com.example.finalproject.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenRequest {
    @NotBlank
    private String accessToken;
}