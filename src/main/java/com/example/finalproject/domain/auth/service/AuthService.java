package com.example.finalproject.domain.auth.service;

import com.example.finalproject.config.SessionIndexService;
import com.example.finalproject.config.TokenProperties;
import com.example.finalproject.config.TokenProvider;
import com.example.finalproject.domain.auth.dto.*;
import com.example.finalproject.domain.auth.entity.SocialAccount;
import com.example.finalproject.domain.auth.repository.SocialAccountRepository;
import com.example.finalproject.domain.common.mail.SmtpMailSender;
import com.example.finalproject.domain.common.redis.CodeStore;
import com.example.finalproject.domain.common.redis.TokenStore;
import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 인증/인가 핵심 서비스
 * - 회원가입/이메일인증
 * - 로그인/토큰재발급/로그아웃
 * - 소셜로그인(카카오/네이버 액세스토큰 기반)
 * - 중복로그인 방지(sid + Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // ── Repository & Infra ───────────────────────────────────────────────
    private final UsersRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    private final PasswordEncoder passwordEncoder;
    private final SmtpMailSender mailSender;

    // ── Token / Redis Stores ─────────────────────────────────────────────
    private final TokenProvider tokenProvider;   // JWT 발급/검증 유틸
    private final TokenStore tokenStore;         // refresh 저장/검증/폐기
    private final CodeStore codeStore;           // 이메일 인증코드 저장
    private final SessionIndexService sidStore;  // userId → sid (중복 로그인 방지)

    private final TokenProperties tokenProperties; // TTL 가져올 때 사용

    // ── OAuth (카카오/네이버) 액세스토큰 검증 서비스 ───────────────────────
    private final OAuthService oAuthService;

    // ====================================================================
    // 이메일 인증
    // ====================================================================
    public void sendSignupEmail(EmailRequest req) {
        String code = sixDigitCode();
        codeStore.saveSignupCode(req.getEmail(), code);
        mailSender.send(
                req.getEmail(),
                "Your verification code",
                "CODE: " + code
        );
    }

    public Map<String, Object> verifySignupEmail(EmailVerifyRequest req) {
        boolean ok = codeStore.verifySignupCode(req.getEmail(), req.getCode());
        if (!ok) throw new IllegalArgumentException("invalid code");
        return Map.of(
                "verifiedToken", UUID.randomUUID().toString(),
                "expiresIn", 600
        );
    }

    // ====================================================================
    // 회원가입
    // ====================================================================
    @Transactional
    public Users signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("email used");
        }

        UserRole role = req.getRole() != null ? UserRole.valueOf(req.getRole()) : UserRole.USER;

        // Users 객체 생성 시, 이름과 핸드폰 번호도 저장
        Users u = Users.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .phoneNumber(req.getPhone_number())  // 핸드폰 번호 설정
                .name(req.getName())                 // 이름 설정
                .role(role)
                .build();

//        // 수정 전 코드
//        Users u = Users.builder()
//                .email(req.getEmail())
//                .password(passwordEncoder.encode(req.getPassword()))
//                .nickname(req.getNickname())
//                .role(UserRole.USER)  // 여기에 USER로 하드코딩되어 있음
//                .build();
        return userRepository.save(u);
    }

     // 로그인
     public Map<String, Object> login(LoginRequest req) {
        Users u = userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("invalid login"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new IllegalArgumentException("invalid login");
        }

        // 새 sid 발급 & 보관(중복 로그인 방지)
        String sid = UUID.randomUUID().toString();
        sidStore.set(u.getId(), sid, tokenProperties.getRefresh().getTtlSeconds());

        String rolesStr = u.getRole().name();
        String access   = tokenProvider.createAccess(u.getId(), u.getEmail(), rolesStr, sid);
        String refresh  = tokenProvider.createRefresh(u.getId(), sid);

        tokenStore.saveRefresh(u.getId(), refresh, tokenProperties.getRefresh().getTtlSeconds());

        return tokenResponse(access, refresh, u, /*social*/ false, null);
    }

    // ====================================================================
    // 토큰 재발급 (Authorization 헤더/쿠키/바디 통해 전달된 refresh)
    // ====================================================================
    public Map<String, Object> refresh(TokenRefreshRequest req) {
        return refresh(req.getRefreshToken());
    }

    public Map<String, Object> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("INVALID_REFRESH");
        }

        // 1) refresh 파싱 → uid, sid
        Claims claims = tokenProvider.parseRefresh(refreshToken);   // TokenProvider에 구현
        Long userId = claims.get("uid", Long.class);
        if (userId == null) {
            userId = Long.parseLong(claims.getSubject()); // 혹시 subject에 보관했을 때 대비
        }
        String sid = Optional.ofNullable(claims.get("sid"))
                .map(Object::toString).orElse(null);

        // 2) 저장소에 실제로 보관된 refresh 인지 확인
        if (!tokenStore.isRefreshValid(userId, refreshToken)) {
            throw new IllegalStateException("INVALID_REFRESH");
        }

        // 3) (옵션) 세션 일치성 확인 — 현재 보관된 sid 와 토큰의 sid 가 다르면 중복 로그인으로 간주
        if (sid != null && !sidStore.isRefreshValid(userId, refreshToken)) {
            throw new IllegalStateException("SESSION_CONFLICT");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        // 4) 토큰 회전
        String rolesStr   = user.getRole().name();
        String newAccess  = tokenProvider.createAccess(user.getId(), user.getEmail(), rolesStr, sid);
        String newRefresh = tokenProvider.createRefresh(user.getId(), sid);

        tokenStore.saveRefresh(user.getId(), newRefresh, tokenProperties.getRefresh().getTtlSeconds());
        sidStore.bump(user.getId(), tokenProperties.getRefresh().getTtlSeconds()); // TTL 연장(선택)

        return Map.of(
                "access_token",  newAccess,
                "refresh_token", newRefresh,
                "token_type",    "Bearer",
                "expires_in",    tokenProperties.getAccess().getTtlSeconds()
        );
    }

    // ====================================================================
    // 로그아웃
    // ====================================================================
    public void logout(String accessToken, Long userId) {
        // 현재 세션 무효화: 사용자 refresh 제거 + sid 제거
        tokenStore.revokeRefresh(userId);
        sidStore.evict(userId);
        // (선택) access jti 블랙리스트 처리하려면 TokenProvider에서 jti 추출 로직을 추가해 사용하세요.
    }

    // ====================================================================
    // 소셜 로그인 (카카오/네이버) — "액세스토큰" 검증 기반
    // ====================================================================
    @Transactional
    public Map<String, Object> socialLoginByAccessToken(SocialProviderLoginRequest req) {
        // 1) 소셜 액세스토큰 검증 및 유저 정보 조회
        var info = oAuthService.fetchUser(req.getProvider(), req.getAccessToken());
        // info: provider, providerId, email(있을수도 없음), nickname(있을수도 없음)

        // 2) 계정 매핑
        Optional<SocialAccount> existing = socialAccountRepository
                .findByProviderAndProviderId(info.getProvider(), info.getProviderId());

        Users u;
        if (existing.isPresent()) {
            u = existing.get().getUser();
        } else {
            // 최초 로그인: 이메일이 없으면 가짜 이메일 부여
            String email = (info.getEmail() != null && !info.getEmail().isBlank())
                    ? info.getEmail()
                    : info.getProvider() + "_" + info.getProviderId() + "@social.local";

            u = userRepository.findByEmailAndDeletedFalse(email).orElseGet(() ->
                    userRepository.save(Users.builder()
                            .email(email)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
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

        // 3) sid 저장 후 토큰 발급
        String sid = UUID.randomUUID().toString();
        sidStore.set(u.getId(), sid, tokenProperties.getRefresh().getTtlSeconds());

        String rolesStr = u.getRole().name();
        String access   = tokenProvider.createAccess(u.getId(), u.getEmail(), rolesStr, sid);
        String refresh  = tokenProvider.createRefresh(u.getId(), sid);

        tokenStore.saveRefresh(u.getId(), refresh, tokenProperties.getRefresh().getTtlSeconds());

        return tokenResponse(access, refresh, u, /*social*/ true, info.getProvider());
    }

    // ====================================================================
    // 내부 유틸
    // ====================================================================
    private Map<String, Object> tokenResponse(String access,
                                              String refresh,
                                              Users u,
                                              boolean social,
                                              String provider) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("access_token", access);
        res.put("refresh_token", refresh);
        res.put("token_type", "Bearer");
        res.put("expires_in", tokenProperties.getAccess().getTtlSeconds());
        res.put("user", Map.of("user_id", u.getId(), "role", u.getRole().name()));
        res.put("social_login", social);
        if (social && provider != null) {
            res.put("provider", provider);
        }
        return res;
    }

    private String sixDigitCode() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }
}