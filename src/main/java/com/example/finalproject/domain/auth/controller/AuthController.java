package com.example.finalproject.domain.auth.controller;

import com.example.finalproject.domain.auth.dto.request.*;
import com.example.finalproject.domain.auth.exception.AuthApiException;
import com.example.finalproject.domain.auth.exception.AuthErrorCode;
import com.example.finalproject.domain.auth.service.AuthService;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
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
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        String access = authentication.getCredentials().toString();
        Long userId   = extractUid(authentication.getDetails());

        authService.logout(access, userId);

        return ResponseEntity.ok(Map.of("message", "로그아웃이 완료되었습니다."));
    }

    @SuppressWarnings("unchecked")
    private Long extractUid(Object details) {
        if (details instanceof Map<?, ?> map) {
            Object v = map.get("uid");
            if (v instanceof Number n) return n.longValue();
            if (v != null) return Long.valueOf(v.toString());
        }
        // details가 Map이 아니거나 uid가 없으면 토큰이 비정상인 것으로 처리
        throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
    }

        @PostMapping("/force-logout")
        public ResponseEntity<Map<String, Object>> forceLogout (@Valid @RequestBody ForceLogoutRequest req){
            authService.forceLogoutWithoutToken(req);
            return ResponseEntity.ok(Map.of(
                    "terminated", true,
                    "message", "이전 세션이 종료되었습니다. 다시 로그인하세요."
            ));
        }
    }
