package com.example.finalproject.domain.infra.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.time.Instant;
import java.util.*;


@Component
public class JwtProvider {
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);


    public String createAccess(Long userId, String role){
        String jti = UUID.randomUUID().toString();
        return Jwts.builder().setId(jti).setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key).compact();
    }


    public String createRefresh(Long userId){
        return Jwts.builder().setSubject("R:"+userId)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(60*60*24*7)))
                .signWith(key).compact();
    }


    public String getJti(String access){
        try { return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(access).getBody().getId(); }
        catch(Exception e){ return null; }
    }
}