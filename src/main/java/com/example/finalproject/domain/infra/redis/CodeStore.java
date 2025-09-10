package com.example.finalproject.domain.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CodeStore {
    private final StringRedisTemplate redis;

    // 회원가입 인증 코드
    public void saveSignupCode(String email, String code){
        redis.opsForValue().set(RedisKeys.signupCode(email), code, Duration.ofMinutes(10));
    }
    public boolean verifySignupCode(String email, String code){
        String v = redis.opsForValue().get(RedisKeys.signupCode(email));
        return v != null && v.equals(code);
    }

    // 비밀번호 재설정 코드
    public void saveResetCode(String email, String code){
        redis.opsForValue().set("reset:code:"+email, code, Duration.ofMinutes(10));
    }
    public boolean verifyResetCode(String email, String code){
        String v = redis.opsForValue().get("reset:code:"+email);
        return v != null && v.equals(code);
    }
    public void clearResetCode(String email){
        redis.delete("reset:code:"+email);
    }
}