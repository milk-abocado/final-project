package com.example.finalproject.domain.auth.service;

import com.example.finalproject.config.SessionIndexService;
import com.example.finalproject.config.TokenProperties;
import com.example.finalproject.config.TokenProvider;
import com.example.finalproject.domain.auth.dto.request.*;
import com.example.finalproject.domain.auth.entity.SocialAccount;
import com.example.finalproject.domain.auth.exception.AuthApiException;
import com.example.finalproject.domain.auth.exception.AuthErrorCode;
import com.example.finalproject.domain.auth.repository.SocialAccountRepository;
import com.example.finalproject.domain.common.mail.SmtpMailSender;
import com.example.finalproject.domain.common.redis.CodeStore;
import com.example.finalproject.domain.common.redis.TokenStore;
import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

/**
 * 인증/인가 핵심 서비스
 * - 회원가입/이메일인증
 * - 로그인/토큰재발급/로그아웃
 * - 소셜로그인(카카오/네이버 액세스토큰 기반)
 * - 단일 세션 + 좀비 세션 자동 정리(sid + Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redis;

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
        mailSender.send(req.getEmail(), "Your verification code", "CODE: " + code);
    }

    public Map<String, Object> verifySignupEmail(EmailVerifyRequest req) {
        boolean ok = codeStore.verifySignupCode(req.getEmail(), req.getCode());
        if (!ok) throw AuthApiException.of(AuthErrorCode.EMAIL_VERIFICATION_INVALID_CODE);
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
            // 409 CONFLICT
            throw AuthApiException.of(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        UserRole role = req.getRole() != null ? UserRole.valueOf(req.getRole()) : UserRole.USER;

        Users u = Users.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .phoneNumber(req.getPhone_number())
                .name(req.getName())
                .role(role)
                .build();

        return userRepository.save(u);
    }

    // ====================================================================
    // 로그인 (분산 락 + 단일 세션 + 좀비 세션 자동 정리)
    // ====================================================================
    public Map<String, Object> login(LoginRequest req) {
        String emailLower = req.getEmail().toLowerCase();
        String lockKey = "lock:login:" + emailLower;
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(3)))) {
            throw AuthApiException.of(AuthErrorCode.LOGIN_IN_PROGRESS);
        }

        try {
            Users u = userRepository.findByEmailIgnoreCaseAndDeletedFalse(emailLower)
                    .orElseThrow(() -> AuthApiException.of(AuthErrorCode.INVALID_CREDENTIALS));
            if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
                throw AuthApiException.of(AuthErrorCode.UNAUTHORIZED);
            }

            // ★ 단일 세션: 이미 sid가 있으면 거절
            String existingSid = sidStore.get(u.getId());
            if (existingSid != null && !existingSid.isBlank()) {
                throw AuthApiException.of(AuthErrorCode.SESSION_EXISTS);
            }

            String sid = UUID.randomUUID().toString();
            sidStore.set(u.getId(), sid, tokenProperties.getRefresh().getTtlSeconds());

            String rolesStr = u.getRole().name();
            String access   = tokenProvider.createAccess(u.getId(), u.getEmail(), rolesStr, sid);
            String refresh  = tokenProvider.createRefresh(u.getId(), sid);
            tokenStore.saveRefresh(u.getId(), refresh, tokenProperties.getRefresh().getTtlSeconds());

            return tokenResponse(access, refresh, u, false, null);
        } finally {
            redis.delete(lockKey);
        }
    }


    // ====================================================================
    // 토큰 재발급 (Authorization 헤더/쿠키/바디 통해 전달된 refresh)
    // ====================================================================
    public Map<String, Object> refresh(TokenRefreshRequest req) {
        return refresh(req.getRefreshToken());
    }

    public Map<String, Object> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AuthApiException.of(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Claims claims;
        try {
            // 1) refresh 파싱 → uid, sid
            claims = tokenProvider.parseRefresh(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            // 서명/만료/형식 오류 등은 전부 INVALID_REFRESH_TOKEN 처리
            throw AuthApiException.of(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = claims.get("uid", Long.class);
        if (userId == null) {
            try {
                userId = Long.parseLong(claims.getSubject());
            } catch (Exception e) {
                throw AuthApiException.of(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
        }
        String sid = Optional.ofNullable(claims.get("sid")).map(Object::toString).orElse(null);

        // 2) 저장소에 실제로 보관된 refresh 인지 확인
        if (!tokenStore.isRefreshValid(userId, refreshToken)) {
            // 만료/폐기/위조 → 401
            throw AuthApiException.of(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3) 세션 일치성 확인 — 현재 보관된 sid 와 토큰의 sid 가 다르면 거부
        if (sid != null && !sidStore.isRefreshValid(userId, refreshToken)) {
            // Security 쪽에서 401/403 으로 매핑되도록 AccessDenied 로 던짐
            throw new AccessDeniedException("session conflict");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> AuthApiException.of(AuthErrorCode.ACCOUNT_NOT_FOUND));

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
        if (userId == null) {
            throw AuthApiException.of(AuthErrorCode.UNAUTHORIZED);
        }

        // 토큰이 넘어온 경우: 유효성 + uid 일치 검증 (안 넘어오면 생략)
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                // 유효성 검사 (서명/만료/형식)
                if (!tokenProvider.validateAccessToken(accessToken)) {
                    throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
                }

                // 토큰에서 uid 추출(우리가 만든 Authentication의 details에 uid가 들어있음)
                var authentication = tokenProvider.getAuthenticationFromAccess(accessToken);
                Object details = authentication.getDetails();

                Long uidFromToken = null;
                if (details instanceof java.util.Map<?,?> map) {
                    Object v = map.get("uid");
                    if (v instanceof Number n) uidFromToken = n.longValue();
                    else if (v != null)       uidFromToken = Long.valueOf(v.toString());
                }
                if (uidFromToken == null || !uidFromToken.equals(userId)) {
                    throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
                }
            } catch (AuthApiException e) {
                throw e; // 위에서 매핑한 코드 유지
            } catch (Exception e) {
                // 파싱/형변환 등 모든 예외를 잘못된 토큰으로 통일
                throw AuthApiException.of(AuthErrorCode.INVALID_ACCESS_TOKEN);
            }
        }

        // 실제 로그아웃 처리: refresh 폐기 + 현재 세션 sid 제거 (idempotent)
        try {
            tokenStore.revokeRefresh(userId);
        } catch (Exception ignored) { /* 이미 없으면 무시 */ }

        try {
            sidStore.evict(userId);
        } catch (Exception ignored) { /* 이미 없으면 무시 */ }

        log.info("[LOGOUT] userId={} refresh revoked & sid evicted", userId);
    }

    // ====================================================================
    // 소셜 로그인 (카카오/네이버) — "액세스토큰" 검증 기반
    // ====================================================================
    @Transactional
    public Map<String, Object> socialLoginByAccessToken(SocialProviderLoginRequest req) {
        var info = oAuthService.fetchUser(req.getProvider(), req.getAccessToken());

        Optional<SocialAccount> existing = socialAccountRepository
                .findByProviderAndProviderId(info.getProvider(), info.getProviderId());

        Users u;
        if (existing.isPresent()) {
            u = existing.get().getUser();
        } else {
            String email = (info.getEmail() != null && !info.getEmail().isBlank())
                    ? info.getEmail()
                    : info.getProvider() + "_" + info.getProviderId() + "@social.local";

            u = userRepository.findByEmailIgnoreCaseAndDeletedFalse(email).orElseGet(() ->
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

    @Transactional
    public void forceLogoutWithoutToken(ForceLogoutRequest req) {
        // 1) 사용자 조회 (존재 노출 방지: INVALID_CREDENTIALS로 통일)
        Users u = userRepository.findByEmailIgnoreCaseAndDeletedFalse(req.email().toLowerCase())
                .orElseThrow(() -> AuthApiException.of(AuthErrorCode.INVALID_CREDENTIALS));

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(req.password(), u.getPassword())) {
            throw AuthApiException.of(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 3) 세션/리프레시 모두 제거 (idempotent)
        tokenStore.revokeRefresh(u.getId());  // 저장된 refresh 제거
        sidStore.evict(u.getId());            // 현재 sid 제거

    }

}
