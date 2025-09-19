//package com.example.finalproject.domain.common.jwt;
//
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.nio.charset.StandardCharsets;
//import java.security.Key;
//import java.util.Date;
//import java.util.UUID;
//
//@Component
//public class JwtProvider {
//
//    private final String issuer;
//    private final String accessSecret;
//    private final String refreshSecret;
//    private final long accessTtlSeconds;
//    private final long refreshTtlSeconds;
//
//    private final Key accessKey;
//    private final Key refreshKey;
//
//    public JwtProvider(
//            @Value("${jwt.issuer:finalproject}") String issuer,
//            @Value("${jwt.access.secret}") String accessSecret,
//            @Value("${jwt.refresh.secret}") String refreshSecret,
//            @Value("${jwt.access.ttl-seconds:3600}") long accessTtlSeconds,
//            @Value("${jwt.refresh.ttl-seconds:604800}") long refreshTtlSeconds
//    ) {
//        this.issuer = issuer;
//        this.accessSecret = accessSecret;
//        this.refreshSecret = refreshSecret;
//        this.accessTtlSeconds = accessTtlSeconds;
//        this.refreshTtlSeconds = refreshTtlSeconds;
//
//        // 🔧 여기서 평문/BASE64/BASE64URL 모두 안전하게 처리
//        this.accessKey = toHmacKey(accessSecret);
//        this.refreshKey = toHmacKey(refreshSecret);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────
//    // Create
//    // ─────────────────────────────────────────────────────────────────────
//    public String createAccess(Long userId, String role) {
//        Date now = new Date();
//        Date exp = new Date(now.getTime() + accessTtlSeconds * 1000L);
//        return Jwts.builder()
//                .setIssuer(issuer)
//                .setSubject(String.valueOf(userId))
//                .setId(UUID.randomUUID().toString())
//                .claim("role", role)
//                .claim("typ", "access")
//                .setIssuedAt(now)
//                .setExpiration(exp)
//                .signWith(accessKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public String createRefresh(Long userId) {
//        Date now = new Date();
//        Date exp = new Date(now.getTime() + refreshTtlSeconds * 1000L);
//        return Jwts.builder()
//                .setIssuer(issuer)
//                .setSubject(String.valueOf(userId))
//                .setId(UUID.randomUUID().toString())
//                .claim("typ", "refresh")
//                .setIssuedAt(now)
//                .setExpiration(exp)
//                .signWith(refreshKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    // ─────────────────────────────────────────────────────────────────────
//    // Parse / Validate
//    // ─────────────────────────────────────────────────────────────────────
//    /** access/refresh 어느 키로 서명됐든지 간에 파싱 시도 */
//    public Claims parseClaimsAny(String token) {
//        try {
//            return Jwts.parserBuilder().setSigningKey(accessKey).build()
//                    .parseClaimsJws(token).getBody();
//        } catch (JwtException | IllegalArgumentException e1) {
//            // access 키로 안되면 refresh 키로 재시도
//            return Jwts.parserBuilder().setSigningKey(refreshKey).build()
//                    .parseClaimsJws(token).getBody();
//        }
//    }
//
//    /** refresh 토큰 전용 검증 */
//    public Claims parseRefresh(String refreshToken) {
//        return Jwts.parserBuilder().setSigningKey(refreshKey).build()
//                .parseClaimsJws(refreshToken).getBody();
//    }
//
//    public boolean validateRefresh(String refreshToken) {
//        try {
//            parseRefresh(refreshToken);
//            return true;
//        } catch (JwtException | IllegalArgumentException e) {
//            return false;
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────────
//    // Extractors
//    // ─────────────────────────────────────────────────────────────────────
//    public Long getUserId(String token) {
//        Claims c = parseClaimsAny(token);
//        try {
//            return Long.parseLong(c.getSubject());
//        } catch (NumberFormatException e) {
//            return null;
//        }
//    }
//
//    public String getJti(String token) {
//        return parseClaimsAny(token).getId();
//    }
//
//    public String getRole(String accessToken) {
//        Object v = parseClaimsAny(accessToken).get("role");
//        return v != null ? v.toString() : null;
//    }
//
//    // ─────────────────────────────────────────────────────────────────────
//    // Key helpers (★ 평문/BASE64/BASE64URL 모두 허용)
//    // ─────────────────────────────────────────────────────────────────────
//    private Key toHmacKey(String secret) {
//        byte[] keyBytes = decodeBestEffort(secret);
//
//        // HS256은 최소 256비트(32바이트) 키 권장 — 짧으면 0으로 padding
//        if (keyBytes.length < 32) {
//            byte[] padded = new byte[32];
//            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
//            for (int i = keyBytes.length; i < 32; i++) padded[i] = '0';
//            keyBytes = padded;
//        }
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
//
//    /**
//     * secret이
//     *  - Base64URL( - _ 포함 ) 이면 BASE64URL로
//     *  - 전통 Base64 정규식에 맞으면 BASE64로
//     *  - 둘 다 아니면 평문(UTF-8)으로 사용
//     */
//    private byte[] decodeBestEffort(String s) {
//        if (s == null) return new byte[0];
//        String v = s.trim();
//
//        // Base64URL 후보: '-' 또는 '_' 포함
//        if (v.contains("-") || v.contains("_")) {
//            try {
//                return Decoders.BASE64URL.decode(v);
//            } catch (RuntimeException ignore) {
//                // fall through
//            }
//        }
//
//        // 전통 Base64 후보
//        if (v.matches("^[A-Za-z0-9+/]+={0,2}$")) {
//            try {
//                return Decoders.BASE64.decode(v);
//            } catch (RuntimeException ignore) {
//                // fall through
//            }
//        }
//
//        // 평문
//        return v.getBytes(StandardCharsets.UTF_8);
//    }
//}
