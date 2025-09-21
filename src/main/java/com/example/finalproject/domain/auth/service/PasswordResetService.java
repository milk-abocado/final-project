package com.example.finalproject.domain.auth.service;

import com.example.finalproject.domain.auth.dto.password.*;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration SEND_RATE_TTL = Duration.ofSeconds(60);
    private static final int MAX_SEND_PER_MIN = 3;
    private static final int MAX_VERIFY_TRIES = 5;

    private final StringRedisTemplate redis;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordMailService mailService;

    private String codeKey(String email)   { return "PW:code:"     + email.toLowerCase(); }
    private String triesKey(String email)  { return "PW:tries:"    + email.toLowerCase(); }
    private String rateKey(String email)   { return "RL:pwreq:"    + email.toLowerCase(); }
    private String verifiedKey(String email){ return "PW:verified:" + email.toLowerCase(); } // ✅ 검증 플래그


    private String genCode() {
        SecureRandom r = new SecureRandom();
        int n = r.nextInt(1_000_000); // 0~999999
        return String.format("%06d", n);
    }

    public void sendCode(SendResetCodeRequest req) {
        String email = req.email().toLowerCase();

        // rate limit
        Long cnt = redis.opsForValue().increment(rateKey(email));
        if (cnt != null && cnt == 1) {
            redis.expire(rateKey(email), SEND_RATE_TTL);
        }
        if (cnt != null && cnt > MAX_SEND_PER_MIN) {
            throw new IllegalStateException("요청이 너무 많습니다. 잠시 후 다시 시도하세요.");
        }

        // 유저 존재 시에만 실제 저장/발송 (없어도 동일 응답)
        Optional<Users> maybe = usersRepository.findByEmail(email);
        if (maybe.isPresent()) {
            String code = genCode();
            redis.opsForValue().set(codeKey(email), code, CODE_TTL);
            redis.delete(triesKey(email));
            // 이전 검증 플래그는 안전을 위해 제거
            redis.delete(verifiedKey(email));
            mailService.sendResetCode(email, code);
        }
        // 컨트롤러에서 항상 200 OK 반환
    }

    public boolean verifyCode(VerifyResetCodeRequest req) {
        String email = req.email().toLowerCase();
        String saved = redis.opsForValue().get(codeKey(email));
        if (saved == null) return false;

        // 시도 횟수 제한
        Long t = redis.opsForValue().increment(triesKey(email));
        if (t != null && t == 1) redis.expire(triesKey(email), CODE_TTL);
        if (t != null && t > MAX_VERIFY_TRIES) {
            redis.delete(codeKey(email));
            return false;
        }

        boolean ok = saved.equals(req.code());
        if (ok) {
            // 검증 완료 플래그 설정
            redis.opsForValue().set(verifiedKey(email), "1", CODE_TTL);
            // 코드/시도 카운터 소진
            try {
                redis.delete(codeKey(email));
                redis.delete(triesKey(email));
            } catch (DataAccessException ignored) {}
        }
        return ok;
    }

    @Transactional
    public void changeAfterVerified(String email, ChangePasswordRequest req) {
        email = email.toLowerCase();

        // 1) 검증 완료 플래그 확인
        String v = redis.opsForValue().get(verifiedKey(email));
        if (v == null) {
            throw new IllegalStateException("NOT_VERIFIED"); // 코드 검증 미완료 또는 만료
        }

        // 2) 새 비밀번호 확인 일치
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("PASSWORD_MISMATCH");
        }

        // 3) 사용자 조회 & 옛 비밀번호 검사
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("INVALID_OLD_PASSWORD");
        }

        // 4) 변경
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);

        // 5) 플래그 소진 (1회성)
        try { redis.delete(verifiedKey(email)); } catch (DataAccessException ignored) {}

    }
}
