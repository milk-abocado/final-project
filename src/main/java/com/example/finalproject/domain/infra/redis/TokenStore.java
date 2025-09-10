package com.example.finalproject.domain.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor

public class TokenStore {
    private final StringRedisTemplate redis;
    public void saveRefresh(Long userId, String refresh, long ttlSeconds){ redis.opsForValue().set(RedisKeys.refreshToken(userId), refresh, Duration.ofSeconds(ttlSeconds)); }
    public boolean isRefreshValid(Long userId, String refresh){ return refresh.equals(redis.opsForValue().get(RedisKeys.refreshToken(userId))); }
    public void revokeRefresh(Long userId){ redis.delete(RedisKeys.refreshToken(userId)); }
    public void blacklistAccess(String jti, long ttlSeconds){ if(jti!=null) redis.opsForValue().set(RedisKeys.accessBlacklist(jti), "1", Duration.ofSeconds(ttlSeconds)); }
}