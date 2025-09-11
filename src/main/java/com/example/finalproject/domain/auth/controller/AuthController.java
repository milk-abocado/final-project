package com.example.finalproject.domain.auth.controller;

import com.example.finalproject.domain.auth.dto.*;
import com.example.finalproject.domain.auth.service.AuthService;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearer,
                                       @RequestHeader("X-USER-ID") Long userId) {
        String access = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
        authService.logout(access, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/social-login/token")
    public ResponseEntity<Map<String, Object>> socialLoginByAccessToken(@Valid @RequestBody SocialProviderLoginRequest req) {
        return ResponseEntity.ok(authService.socialLoginByAccessToken(req));
    }

    @PostMapping("/social-login/kakao")
    public ResponseEntity<Map<String, Object>> socialLoginKakao(@Valid @RequestBody AccessTokenRequest req) {
        var sreq = SocialProviderLoginRequest.builder()
                .provider("kakao")
                .accessToken(req.getAccessToken())
                .build();
        return ResponseEntity.ok(authService.socialLoginByAccessToken(sreq));
    }

    @PostMapping("/social-login/naver")
    public ResponseEntity<Map<String, Object>> socialLoginNaver(@Valid @RequestBody AccessTokenRequest req) {
        var sreq = SocialProviderLoginRequest.builder()
                .provider("naver")
                .accessToken(req.getAccessToken())
                .build();
        return ResponseEntity.ok(authService.socialLoginByAccessToken(sreq));
    }
}