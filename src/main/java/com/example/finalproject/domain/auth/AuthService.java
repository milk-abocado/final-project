// src/main/java/com/example/finalproject/domain/auth/AuthService.java
package com.example.finalproject.domain.auth;

import com.example.finalproject.domain.auth.dto.EmailRequest;
import com.example.finalproject.domain.auth.dto.EmailVerifyRequest;
import com.example.finalproject.domain.auth.dto.LoginRequest;
import com.example.finalproject.domain.auth.dto.SignupRequest;
import com.example.finalproject.domain.auth.dto.SocialProviderLoginRequest;
import com.example.finalproject.domain.auth.dto.TokenRefreshRequest;

import com.example.finalproject.domain.users.UserRepository;
import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.Users;
import com.example.finalproject.domain.common.jwt.JwtProvider;
import com.example.finalproject.domain.common.redis.CodeStore;
import com.example.finalproject.domain.common.redis.TokenStore;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    private final JwtProvider jwtProvider;
    private final CodeStore codeStore;
    private final TokenStore tokenStore;
    private final SmtpMailSender mailSender;     // SMTP 메일 발송기
    private final OAuthService oAuthService;     // 카카오/네이버 액세스 토큰 검증

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ─────────────────────────────────────────────────────────────────────
    // 이메일 인증 (가입)
    // ─────────────────────────────────────────────────────────────────────
    public void sendSignupEmail(EmailRequest req) {
        String code = sixDigitCode();
        codeStore.saveSignupCode(req.getEmail(), code);
        mailSender.send(req.getEmail(), "Your verification code", "CODE: " + code);
    }

    public Map<String, Object> verifySignupEmail(EmailVerifyRequest req) {
        boolean ok = codeStore.verifySignupCode(req.getEmail(), req.getCode());
        if (!ok) throw new IllegalArgumentException("invalid code");
        return Map.of("verifiedToken", UUID.randomUUID().toString(), "expiresIn", 600);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 회원 가입 / 로그인 / 토큰 재발급 / 로그아웃
    // ─────────────────────────────────────────────────────────────────────
    @Transactional
    public Users signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("email used");

        Users u = Users.builder()
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .role(UserRole.USER)
                .build();

        return userRepository.save(u);
    }

    public Map<String, Object> login(LoginRequest req) {
        Users u = userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("invalid login"));
        if (!encoder.matches(req.getPassword(), u.getPassword()))
            throw new IllegalArgumentException("invalid login");

        String access = jwtProvider.createAccess(u.getId(), u.getRole().name());
        String refresh = jwtProvider.createRefresh(u.getId());
        tokenStore.saveRefresh(u.getId(), refresh, 60L * 60 * 24 * 7);

        return Map.of(
                "access_token", access,
                "refresh_token", refresh,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", Map.of("user_id", u.getId(), "role", u.getRole().name()),
                "social_login", false
        );
    }

    public Map<String, Object> refresh(Long userId, TokenRefreshRequest req) {
        if (!tokenStore.isRefreshValid(userId, req.getRefreshToken()))
            throw new IllegalStateException("INVALID_REFRESH");

        // rotate (이전 refresh 무효화: 같은 키에 새 토큰 저장)
        String newAccess = jwtProvider.createAccess(userId, "USER");
        String newRefresh = jwtProvider.createRefresh(userId);
        tokenStore.saveRefresh(userId, newRefresh, 60L * 60 * 24 * 7);

        return Map.of(
                "access_token", newAccess,
                "refresh_token", newRefresh,
                "token_type", "Bearer",
                "expires_in", 3600
        );
    }

    public void logout(String access, Long userId) {
        String jti = jwtProvider.getJti(access);
        tokenStore.blacklistAccess(jti, 3600); // 액세스 만료까지 블랙리스트
        tokenStore.revokeRefresh(userId);      // 사용자 리프레시 제거
    }

    // ─────────────────────────────────────────────────────────────────────
    // 소셜 로그인 (카카오/네이버) — 클라이언트의 AccessToken 검증 방식
    // ─────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> socialLoginByAccessToken(SocialProviderLoginRequest req) {
        var info = oAuthService.fetchUser(req.getProvider(), req.getAccessToken());

        // 1) 소셜 계정이 이미 존재?
        Optional<SocialAccount> existing = socialAccountRepository
                .findByProviderAndProviderId(info.getProvider(), info.getProviderId());

        Users u;
        if (existing.isPresent()) {
            u = existing.get().getUser();
        } else {
            // 2) 최초 로그인: 사용자 연결/생성 후 소셜 계정 저장
            String email = (info.getEmail() != null && !info.getEmail().isBlank())
                    ? info.getEmail()
                    : info.getProvider() + "_" + info.getProviderId() + "@social.local";

            u = userRepository.findByEmailAndDeletedFalse(email).orElseGet(() ->
                    userRepository.save(Users.builder()
                            .email(email)
                            .password(encoder.encode(UUID.randomUUID().toString()))
                            .nickname(info.getNickname() != null ? info.getNickname() : "소셜유저")
                            .role(UserRole.USER)
                            .build())
            );

            socialAccountRepository.save(SocialAccount.builder()
                    .user(u)
                    .provider(info.getProvider())
                    .providerId(info.getProviderId())
                    .build());
        }

        String access = jwtProvider.createAccess(u.getId(), u.getRole().name());
        String refresh = jwtProvider.createRefresh(u.getId());
        tokenStore.saveRefresh(u.getId(), refresh, 60L * 60 * 24 * 7);

        return Map.of(
                "access_token", access,
                "refresh_token", refresh,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", Map.of("user_id", u.getId(), "role", u.getRole().name()),
                "social_login", true,
                "provider", info.getProvider()
        );
    }

    private String sixDigitCode() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }
}
