package com.example.finalproject.domain.common.redis;

public final class RedisKeys {
    public static String signupCode(String email){ return "signup:code:"+email; }
    public static String refreshToken(Long userId){ return "token:refresh:"+userId; }
    public static String accessBlacklist(String jti){ return "bl:access:"+jti; }
}