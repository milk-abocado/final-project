package com.example.finalproject.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

    private final TokenProperties props;

    // ─────────────────────────────────────────────────────────────────────
    // Keys / Parsers
    // ─────────────────────────────────────────────────────────────────────
    private Key accessKey;
    private Key refreshKey;

    private JwtParser accessParser;
    private JwtParser refreshParser;

    @PostConstruct
    void init() {
        this.accessKey  = hmacKey(props.getAccess().getSecret());
        this.refreshKey = hmacKey(props.getRefresh().getSecret());

        // JJWT 0.11.x
        this.accessParser  = Jwts.parserBuilder().setSigningKey(accessKey).build();
        this.refreshParser = Jwts.parserBuilder().setSigningKey(refreshKey).build();

        if (log.isInfoEnabled()) {
            log.info("TokenProvider initialized. accessTTL={}s, refreshTTL={}s",
                    props.getAccess().getTtlSeconds(), props.getRefresh().getTtlSeconds());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Authorization 헤더에서 "Bearer " 접두어 제거 (대소문자 무시). 불일치 시 null */
    public static String stripBearer(String header) {
        if (header == null) return null;
        String h = header.trim();
        if (h.length() >= 7 && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return h.substring(7).trim();
        }
        return null;
    }

    /** 액세스 토큰 검증 (유효/만료/서명) */
    public boolean validateAccessToken(String token) {
        return validate(token, accessParser, "access");
    }

    /** 리프레시 토큰 검증 (유효/만료/서명) */
    public boolean validateRefreshToken(String token) {
        return validate(token, refreshParser, "refresh");
    }

    /** (추가) 리프레시 토큰 파싱 (Claims 반환) */
    public Claims parseRefresh(String token) {
        return refreshParser.parseClaimsJws(token).getBody();
    }

    /** 액세스 토큰 → Spring Security Authentication */
    public Authentication getAuthenticationFromAccess(String token) {
        Claims c = accessParser.parseClaimsJws(token).getBody();

        String uid   = optStr(c.get("uid"));
        String email = optStr(c.get("email"));        // 없을 수 있음
        String roles = optStr(c.get("roles"));        // "ROLE_USER,ROLE_ADMIN" 형태 또는 null
        String sid   = optStr(c.get("sid"));          // 선택 클레임

        // 필수는 uid만으로 축소 (email 없이도 동작하도록)
        if (!StringUtils.hasText(uid)) {
            log.warn("[JWT] required claim 'uid' missing. claims={}", c);
            throw new BadCredentialsException("JWT missing required claims");
        }

        List<GrantedAuthority> authorities =
                StringUtils.hasText(roles)
                        ? Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
                        : Collections.emptyList();

        // username: email 있으면 email, 없으면 uid
        String username = StringUtils.hasText(email) ? email : uid;

        UserDetails principal = User.withUsername(username)
                .password("")                 // 비밀번호는 사용 안 함
                .authorities(authorities)     // List<GrantedAuthority>
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, token, authorities);

        Map<String, Object> details = new HashMap<>();
        details.put("uid", uid);
        if (StringUtils.hasText(email)) details.put("email", email);
        if (StringUtils.hasText(sid))   details.put("sid", sid);
        if (StringUtils.hasText(roles)) details.put("roles", roles);
        auth.setDetails(details);

        return auth;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Token creation
    // ─────────────────────────────────────────────────────────────────────

    /** 액세스/리프레시 동시 생성 (roles null 가능, email null 가능) */
    public Map<String, Object> createTokens(Long userId,
                                            String email,
                                            Collection<String> roles,
                                            String sid) {
        String access  = createAccess(userId, email, joinRoles(roles), sid);
        String refresh = createRefresh(userId, sid);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("access_token", access);
        out.put("refresh_token", refresh);
        out.put("token_type", "Bearer");
        out.put("expires_in", props.getAccess().getTtlSeconds());
        return out;
    }

    /** 액세스 토큰 단독 생성 (roleName 단일 문자열, email 생략 가능) */
    public String createAccess(Long userId, String roleName) {
        return createAccess(userId, null, roleName, null);
    }

    /** 액세스 토큰 생성 (email/roles/sid 모두 선택) */
    public String createAccess(Long userId, String email, String roles, String sid) {
        long nowMs = System.currentTimeMillis();
        long expMs = nowMs + props.getAccess().getTtlSeconds() * 1000L;

        JwtBuilder b = Jwts.builder()
                .setSubject("access")
                .claim("uid", userId)
                .setIssuedAt(new Date(nowMs))
                .setExpiration(new Date(expMs));

        if (StringUtils.hasText(email)) b.claim("email", email);
        if (StringUtils.hasText(roles)) b.claim("roles", roles);
        if (StringUtils.hasText(sid))   b.claim("sid", sid);

        return b.signWith(accessKey, SignatureAlgorithm.HS256).compact();
    }

    /** 리프레시 토큰 생성 (sid 선택) */
    public String createRefresh(Long userId, String sid) {
        long nowMs = System.currentTimeMillis();
        long expMs = nowMs + props.getRefresh().getTtlSeconds() * 1000L;

        JwtBuilder b = Jwts.builder()
                .setSubject("refresh")
                .claim("uid", userId)
                .setIssuedAt(new Date(nowMs))
                .setExpiration(new Date(expMs));

        if (StringUtils.hasText(sid)) b.claim("sid", sid);

        return b.signWith(refreshKey, SignatureAlgorithm.HS256).compact();
    }

    /** 리프레시 토큰 단독 (overload) */
    public String createRefresh(Long userId) {
        return createRefresh(userId, null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private boolean validate(String token, JwtParser parser, String typ) {
        try {
            parser.parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("{} token expired: {}", typ, e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid {} token: {}", typ, e.getMessage());
        }
        return false;
    }

    private static String joinRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return null;
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    private static String optStr(Object v) {
        return v == null ? null : v.toString();
    }

    /** Base64 우선, 실패 시 UTF-8 바이트 사용. HS256 권장 최소 32바이트 미만이면 패딩. */
    private static Key hmacKey(String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) { // not base64
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            bytes = Arrays.copyOf(bytes, 32); // 간단 패딩 (운영에선 강한 키 사용 권장)
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
