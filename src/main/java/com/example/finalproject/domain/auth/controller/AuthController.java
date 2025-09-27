package com.example.finalproject.domain.auth.controller;

import com.example.finalproject.domain.auth.dto.request.*;
import com.example.finalproject.domain.auth.exception.AuthApiException;
import com.example.finalproject.domain.auth.exception.AuthErrorCode;
import com.example.finalproject.domain.auth.service.AuthService;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest req) {
        Users u = authService.signup(req);
        return ResponseEntity.status(201).body(Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "nickname", u.getNickname()
        ));
    }

    @PostMapping("/signup/email/request")
    public ResponseEntity<Void> signupEmail(@Valid @RequestBody EmailRequest req) {
        authService.sendSignupEmail(req);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/signup/email/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody EmailVerifyRequest req) {
        return ResponseEntity.ok(authService.verifySignupEmail(req));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @CookieValue(value = "refresh_token", required = false) String refreshCookie,
            @RequestBody(required = false) TokenRefreshRequest body
    ) {
        String refreshToken = resolveRefreshToken(authorization, refreshCookie, body);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "refresh_token_missing",
                    "hint", "Send refresh token via Authorization: Bearer <token>, cookie 'refresh_token', or JSON body {\"refreshToken\":\"...\"}"
            ));
        }
        return ResponseEntity.ok(authService.refresh(refreshToken)); // 서비스는 Map 반환
    }

    private String resolveRefreshToken(String authorization, String refreshCookie, TokenRefreshRequest body) {
        // 1) Authorization: Bearer <token>
        if (authorization != null && authorization.toLowerCase().startsWith("bearer ")) {
            return authorization.substring(7).trim();
        }
        // 2) Cookie: refresh_token=<token>
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            return refreshCookie.trim();
        }
        // 3) JSON Body: { "refreshToken": "<token>" }
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            return body.getRefreshToken().trim();
        }
        return null;
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(org.springframework.security.core.Authentication authentication) {

        // 0) 토큰 자체가 만료/위조라면 필터에서 인증이 안 세워지므로 여기서 401
        if (authentication == null) {
            throw AuthApiException.of(AuthErrorCode.UNAUTHORIZED);
        }

        // 1) Bearer 자격증명 존재/형식 검사
        Object credObj = authentication.getCredentials();
        if (credObj == null) {
            throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        String access = credObj.toString();
        if (access.isBlank()) {
            throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        // 2) details에 담긴 uid 추출(숫자/문자열 모두 수용), 실패시 401
        Long userId = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
            Object uidObj = (details != null) ? details.get("uid") : null;
            if (uidObj instanceof Number n) {
                userId = n.longValue();
            } else if (uidObj != null) {
                userId = Long.valueOf(uidObj.toString());
            }
        } catch (ClassCastException | NumberFormatException e) {
            throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        if (userId == null) {
            throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        // 3) 정상 처리
        authService.logout(access, userId);
        return ResponseEntity.ok(java.util.Map.of("message", "로그아웃되었습니다."));
    }
    @PostMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        // (1) 액세스 토큰 (Bearer 본문)
        String access = auth.getCredentials() != null ? auth.getCredentials().toString() : null;

        // (2) username (우린 email 있으면 email, 없으면 uid를 username으로 세팅해둠)
        String username = auth.getName();

        // (3) details 맵에서 uid/email/sid/roles 안전하게 추출
        Long   uid   = null;
        String email = null;
        String sid   = null;
        String roles = null;

        Object details = auth.getDetails();
        if (details instanceof java.util.Map<?,?> m) {
            Object u = m.get("uid");
            if (u instanceof Number n) uid = n.longValue();
            else if (u != null)        uid = Long.valueOf(u.toString());

            Object e = m.get("email"); if (e != null) email = e.toString();
            Object s = m.get("sid");   if (s != null) sid   = s.toString();
            Object r = m.get("roles"); if (r != null) roles = r.toString(); // "ROLE_USER,ROLE_ADMIN"
        }

        // (4) 권한은 authorities에도 들어있음
        var authorities = auth.getAuthorities(); // Collection<GrantedAuthority>

        return ResponseEntity.ok(Map.of(
                "access", access, "username", username,
                "uid", uid, "email", email, "sid", sid, "roles", roles,
                "authorities", authorities
        ));
    }

    @PostMapping("/force-logout")
    public ResponseEntity<Map<String, Object>> forceLogout(@Valid @RequestBody ForceLogoutRequest req) {
        authService.forceLogoutWithoutToken(req);
        return ResponseEntity.ok(Map.of(
                "terminated", true,
                "message", "이전 세션이 종료되었습니다. 다시 로그인하세요."
        ));
    }
}