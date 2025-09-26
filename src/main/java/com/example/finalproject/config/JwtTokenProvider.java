package com.example.finalproject.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtTokenProvider {

    private final Key accessKey;
    private final String issuer;


    public JwtTokenProvider(
            @Value("${jwt.access.secret}") String accessSecretBase64,
            @Value("${jwt.issuer:finalproject}") String issuer
    ) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecretBase64));
        this.issuer = issuer;
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .requireIssuer(issuer)
                    .setSigningKey(accessKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(issuer)
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject(); // subject에 username/email을 넣어 발급했다고 가정
    }
}