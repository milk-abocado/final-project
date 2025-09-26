package com.example.finalproject.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter @Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class TokenProperties {
    private String issuer;
    private Part access;
    private Part refresh;

    @Data
    public static class Part {
        private String secret;     // yml: jwt.access.secret
        private long ttlSeconds;   // yml: jwt.access.ttl-seconds  (relaxed binding OK)
    }
}
