package com.example.finalproject.config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtService {

    private final String accessSecretValue;
    private final String refreshSecretValue;
    private final long accessTtlSec;
    private final long refreshTtlSec;

    private Key accessKey;
    private Key refreshKey;

    public JwtService(
            @Value("${jwt.access.secret}") String accessSecretValue,
            @Value("${jwt.refresh.secret}") String refreshSecretValue,
            @Value("${jwt.access.ttl-seconds:3600}") long accessTtlSec,
            @Value("${jwt.refresh.ttl-seconds:604800}") long refreshTtlSec
    ) {
        this.accessSecretValue = accessSecretValue;
        this.refreshSecretValue = refreshSecretValue;
        this.accessTtlSec = accessTtlSec;
        this.refreshTtlSec = refreshTtlSec;
    }

    @PostConstruct
    void init() {
        this.accessKey = buildKey(this.accessSecretValue);
        this.refreshKey = buildKey(this.refreshSecretValue);
    }

    /**
     * 시크릿 값이 표준 Base64 / Base64URL / 평문(UTF-8) 무엇이든 받아서
     * 디코딩/변환 후 32바이트(256비트) 이상인지 검증.
     */
    private Key buildKey(String value) {
        String v = value == null ? "" : value.trim();
        byte[] keyBytes;

        // 1) 표준 Base64 시도
        try {
            keyBytes = Decoders.BASE64.decode(v);
        } catch (DecodingException e1) {
            // 2) Base64URL 시도 (-, _ 허용)
            try {
                keyBytes = Decoders.BASE64URL.decode(v);
            } catch (DecodingException e2) {
                // 3) 평문으로 처리
                keyBytes = v.getBytes(StandardCharsets.UTF_8);
            }
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be >= 256 bits (32 bytes) after decoding. current="
                            + (keyBytes.length * 8) + " bits");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, accessKey, accessTtlSec);
    }

    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, refreshKey, refreshTtlSec);
    }

    private String buildToken(String subject, Map<String, Object> claims, Key key, long ttlSec) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSec)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token, boolean refresh) {
        return extractClaim(token, Claims::getSubject, refresh);
    }

    public boolean isExpired(String token, boolean refresh) {
        Date exp = extractClaim(token, Claims::getExpiration, refresh);
        return exp.before(new Date());
    }

    public boolean isTokenValid(String token, String username, boolean refresh) {
        return username.equals(extractUsername(token, refresh)) && !isExpired(token, refresh);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver, boolean refresh) {
        return resolver.apply(parseAllClaims(token, refresh));
    }

    private Claims parseAllClaims(String token, boolean refresh) {
        Key key = refresh ? refreshKey : accessKey;
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}