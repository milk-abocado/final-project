package com.example.finalproject.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionIndexService implements SessionIndexService {

    private final StringRedisTemplate redis;
    private final TokenProvider tokenProvider; // ← TokenProvider만 참조 (반대 방향 제거)

    private String key(Long userId) {
        return "sid:" + userId;
    }

    @Override
    public void set(Long userId, String sid, long ttlSeconds) {
        if (userId == null || sid == null) return;
        redis.opsForValue().set(key(userId), sid, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void bump(Long userId, long ttlSeconds) {
        if (userId == null) return;
        redis.expire(key(userId), Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void evict(Long userId) {
        if (userId == null) return;
        redis.delete(key(userId));
    }

    @Override
    public Optional<String> get(Long userId) {
        if (userId == null) return Optional.empty();
        String v = redis.opsForValue().get(key(userId));
        return Optional.ofNullable(v);
    }

    @Override
    public boolean isRefreshValid(Long userId, String refreshToken) {
        try {
            if (userId == null || refreshToken == null || refreshToken.isBlank()) {
                return false;
            }
            // 1) 토큰 파싱(서명/만료 검사 포함)
            Claims c = tokenProvider.parseRefresh(refreshToken);

            // 2) 토큰 uid/sid 추출
            Long uid = null;
            Object u = c.get("uid");
            if (u != null) {
                try { uid = (u instanceof Number) ? ((Number) u).longValue() : Long.parseLong(u.toString()); }
                catch (Exception ignore) {}
            }
            String sidInToken = (String) c.get("sid"); // 없을 수도 있음

            // 3) 저장된 sid 조회
            String savedSid = redis.opsForValue().get(key(userId));

            // 4) 규칙: uid 일치 && (sid가 있다면 sid도 일치) && 저장된 sid도 존재/일치
            if (uid == null || !userId.equals(uid)) return false;
            if (savedSid == null) return false;               // 저장된 sid가 없다면 불허
            if (sidInToken != null && !savedSid.equals(sidInToken)) return false;

            return true;
        } catch (Exception e) {
            log.warn("isRefreshValid: invalid refresh token - {}", e.getMessage());
            return false;
        }
    }
}
