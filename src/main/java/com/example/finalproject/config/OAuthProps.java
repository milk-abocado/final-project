package com.example.finalproject.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProps {
    private Provider kakao = new Provider();
    private Provider naver = new Provider();

    @Data
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope; // optional
    }
}