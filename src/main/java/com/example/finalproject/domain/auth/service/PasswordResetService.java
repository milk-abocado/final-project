package com.example.finalproject.domain.auth.service;

import com.example.finalproject.domain.auth.dto.password.ChangePasswordRequest;
import com.example.finalproject.domain.auth.dto.password.ConfirmResetPasswordRequest;
import com.example.finalproject.domain.auth.dto.password.SendResetCodeRequest;
import com.example.finalproject.domain.auth.dto.password.VerifyResetCodeRequest;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private String codeKey(String email)     { return "PW:code:"      + email.toLowerCase(); }
    private String triesKey(String email)    { return "PW:tries:"     + email.toLowerCase(); }
    private String rateKey(String email)     { return "RL:pwreq:"     + email.toLowerCase(); }
    private String verifiedKey(String email) { return "PW:verified:"  + email.toLowerCase(); }

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

        // 유저가 존재할 때만 실제 코드 저장/발송 (없어도 항상 같은 응답을 주어 정보 노출 방지)
        Optional<Users> maybe = usersRepository.findByEmailIgnoreCase(email); // ★ repo 메서드
        if (maybe.isPresent()) {

            String code = genCode();
            redis.opsForValue().set(codeKey(email), code, CODE_TTL);
            redis.delete(triesKey(email));
            redis.delete(verifiedKey(email)); // 이전 검증 플래그 제거
            mailService.sendResetCode(email, code);
        }
    }

    /** [선택] 코드 사전 검증 (익명) — 성공 시 검증 플래그 저장 */
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
            // 검증 완료 플래그 설정(코드 TTL과 동일)
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
    public void confirm(ConfirmResetPasswordRequest req) {
        String email = req.email().toLowerCase();
        String saved = redis.opsForValue().get(codeKey(email));
        if (saved == null || !saved.equals(req.code())) {
            Long t = redis.opsForValue().increment(triesKey(email));
            if (t != null && t == 1) redis.expire(triesKey(email), CODE_TTL);
            throw new IllegalArgumentException("코드가 유효하지 않거나 만료되었습니다.");
        }

        Users user = usersRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "가입된 이메일이 아닙니다."));
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);

        try {
            redis.delete(codeKey(email));
            redis.delete(triesKey(email));
            redis.delete(verifiedKey(email));
        } catch (DataAccessException ignored) {}

    }

    @Transactional
    public void change(String email, ChangePasswordRequest req) {
        email = email.toLowerCase();

        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("PASSWORD_MISMATCH");
        }

        Users user = usersRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("INVALID_OLD_PASSWORD");
        }

        user.changePassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);

    }


    @Transactional
    public void changeAfterVerified(String email, ChangePasswordRequest req) {
        email = email.toLowerCase();

        // 1) 검증 완료 플래그 확인
        String v = redis.opsForValue().get(verifiedKey(email));
        if (v == null) {
            throw new IllegalStateException("NOT_VERIFIED"); // 코드 검증 미완료 또는 만료
        }

        // 2) 새 비밀번호 확인
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("PASSWORD_MISMATCH");
        }

        // 3) 사용자 조회 & 옛 비밀번호 검사
        Users user = usersRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("INVALID_OLD_PASSWORD");
        }

        // 4) 변경
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);

        // 5) 플래그 소진 (1회성)
        try { redis.delete(verifiedKey(email)); } catch (DataAccessException ignored) {}
    }
}
