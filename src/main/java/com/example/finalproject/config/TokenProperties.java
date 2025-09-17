package com.example.finalproject.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter @Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class TokenProperties {

    /** 토큰 발급자(issuer) */
    @NotBlank
    private String issuer = "finalproject";

    /** 액세스 토큰 설정 */
    @Valid
    private Access access = new Access();

    /** 리프레시 토큰 설정 */
    @Valid
    private Refresh refresh = new Refresh();

    @Getter @Setter
    public static class Access {
        /** Base64 인코딩된 HS256 시크릿 */
        @NotBlank
        private String secret;
        /** 유효기간(초) */
        @Positive
        private long ttlSeconds = 3600;
    }

    @Getter @Setter
    public static class Refresh {
        /** Base64 인코딩된 HS256 시크릿 */
        @NotBlank
        private String secret;
        /** 유효기간(초) */
        @Positive
        private long ttlSeconds = 604800;
    }
}