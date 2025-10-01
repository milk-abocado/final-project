package com.example.finalproject.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionIndexService implements SessionIndexService {

    private final StringRedisTemplate redis;
    private final TokenProvider tokenProvider; // TokenProvider만 참조(순환 제거)

    private String key(Long userId) {
        return "sid:" + userId;
    }

    @Override
    public void set(Long userId, String sid, long ttlSeconds) {
        if (userId == null || sid == null || sid.isBlank()) return;
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

    /** 인터페이스 시그니처에 맞춰 null 반환 */
    @Override
    public String get(Long userId) {
        if (userId == null) return null;
        return redis.opsForValue().get(key(userId));
    }

    @Override
    public boolean isRefreshValid(Long userId, String refreshToken) {
        try {
            if (userId == null || refreshToken == null || refreshToken.isBlank()) return false;

            // 1) 토큰 파싱(서명/만료 검사 포함)
            Claims c = tokenProvider.parseRefresh(refreshToken);

            // 2) uid/sid 추출
            Long uid = null;
            Object u = c.get("uid");
            if (u instanceof Number n) {
                uid = n.longValue();
            } else if (u != null) {
                try { uid = Long.parseLong(u.toString()); } catch (NumberFormatException ignore) {}
            }
            String sidInToken = c.get("sid", String.class); // null 가능

            // 3) 저장된 sid 조회
            String savedSid = get(userId);

            // 4) 규칙: uid 일치 && 저장된 sid 존재 && (토큰에 sid가 있으면 그것도 일치)
            if (uid == null || !userId.equals(uid)) return false;
            if (savedSid == null || savedSid.isBlank()) return false;
            if (sidInToken != null && !savedSid.equals(sidInToken)) return false;

            return true;
        } catch (Exception e) {
            log.warn("isRefreshValid: invalid refresh token - {}", e.getMessage());
            return false;
        }
    }
}
