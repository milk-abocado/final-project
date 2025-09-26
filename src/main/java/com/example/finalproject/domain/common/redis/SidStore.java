package com.example.finalproject.domain.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SidStore {
    private final StringRedisTemplate redis;
    private static final String KEY_PREFIX = "auth:sid:"; // auth:sid:{userId} -> sid

    public void set(long userId, String sid, long ttlSeconds) {
        redis.opsForValue().set(KEY_PREFIX + userId, sid, Duration.ofSeconds(ttlSeconds));
    }

    public String get(long userId) {
        return redis.opsForValue().get(KEY_PREFIX + userId);
    }

    public boolean matches(long userId, String sid) {
        if (sid == null) return false;
        String cur = get(userId);
        return sid.equals(cur);
    }

    public void bump(long userId, long ttlSeconds) {
        redis.expire(KEY_PREFIX + userId, Duration.ofSeconds(ttlSeconds));
    }

    public void clear(long userId) {
        redis.delete(KEY_PREFIX + userId);
    }
}