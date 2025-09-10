package com.example.finalproject.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialProviderLoginRequest {
    @NotBlank private String provider;     // "kakao" | "naver"
    @NotBlank private String accessToken;  // 클라이언트에서 받은 IdP access token
}