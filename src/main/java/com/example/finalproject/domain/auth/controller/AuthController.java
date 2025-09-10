package com.example.finalproject.domain.auth.controller;

import com.example.finalproject.domain.auth.AuthService;
import com.example.finalproject.domain.auth.dto.*;
import com.example.finalproject.domain.users.*;
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
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        Users u = authService.signup(req);
        return ResponseEntity.status(201).body(Map.of(
                "id", u.getId(), "email", u.getEmail(), "nickname", u.getNickname()
        ));
    }


    @PostMapping("/signup/email/request")
    public ResponseEntity<?> signupEmail(@Valid @RequestBody EmailRequest req) {
        authService.sendSignupEmail(req);
        return ResponseEntity.status(201).build();
    }


    @PostMapping("/signup/email/verify")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerifyRequest req) {
        return ResponseEntity.ok(authService.verifySignupEmail(req));
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }


    @PostMapping("/token/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("X-USER-ID") Long userId,
                                     @Valid @RequestBody TokenRefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(userId, req));
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String bearer,
                                    @RequestHeader("X-USER-ID") Long userId) {
        String access = bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : null;
        authService.logout(access, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/social-login/token")
    public ResponseEntity<?> socialLoginByAccessToken(
            @Valid @RequestBody SocialProviderLoginRequest req) {
        return ResponseEntity.ok(authService.socialLoginByAccessToken(req));
    }

    @PostMapping("/social-login/kakao")
    public ResponseEntity<?> socialLoginKakao(
            @jakarta.validation.Valid @RequestBody AccessTokenRequest req) {
        var sreq = SocialProviderLoginRequest.builder()
                .provider("kakao")
                .accessToken(req.getAccessToken())
                .build();
        return ResponseEntity.ok(authService.socialLoginByAccessToken(sreq));
    }

    @PostMapping("/social-login/naver")
    public ResponseEntity<?> socialLoginNaver(
            @jakarta.validation.Valid @RequestBody AccessTokenRequest req) {
        var sreq = SocialProviderLoginRequest.builder()
                .provider("naver")
                .accessToken(req.getAccessToken())
                .build();
        return ResponseEntity.ok(authService.socialLoginByAccessToken(sreq));
    }
}