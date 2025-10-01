package com.example.finalproject.domain.auth.service;

import com.example.finalproject.domain.auth.dto.password.ChangePasswordRequest;
import com.example.finalproject.domain.auth.dto.password.ConfirmResetPasswordRequest;
import com.example.finalproject.domain.auth.dto.password.SendResetCodeRequest;
import com.example.finalproject.domain.auth.dto.password.VerifyResetCodeRequest;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import com.example.finalproject.domain.auth.exception.AuthApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

import static com.example.finalproject.domain.auth.exception.AuthErrorCode.*;

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
        return String.format("%06d", r.nextInt(1_000_000));
    }

    /** 비밀번호 재설정 코드 전송(익명) */
    public void sendCode(SendResetCodeRequest req) {
        String email = req.email().toLowerCase();

        // rate limit
        Long cnt = redis.opsForValue().increment(rateKey(email));
        if (cnt != null && cnt == 1) {
            redis.expire(rateKey(email), SEND_RATE_TTL);
        }
        if (cnt != null && cnt > MAX_SEND_PER_MIN) {
            // 비번 재설정 영역의 코드로 응답(429가 가장 이상적이지만, 코드 테이블 범위 내에서 처리)
            throw AuthApiException.of(PASSWORD_RESET_INVALID_CODE);
        }

        // 유저가 존재할 때만 실제 코드 저장/발송(없는 이메일에 대해서도 동일 응답으로 정보 노출 방지)
        Optional<Users> maybe = usersRepository.findByEmailIgnoreCase(email);
        if (maybe.isPresent()) {
            String code = genCode();
            redis.opsForValue().set(codeKey(email), code, CODE_TTL);
            redis.delete(triesKey(email));
            redis.delete(verifiedKey(email)); // 이전 플래그 제거
            mailService.sendResetCode(email, code);
        }
        // 존재하지 않는 경우에도 무음 성공(보안상)
    }

    /** [선택] 코드 사전 검증(익명) — 성공 시 검증 플래그 저장 */
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

    /** 코드 + 새 비밀번호로 최종 확정(익명) */
    @Transactional
    public void confirm(ConfirmResetPasswordRequest req) {
        String email = req.email().toLowerCase();
        String saved = redis.opsForValue().get(codeKey(email));

        if (saved == null) {
            // 만료로 간주
            throw AuthApiException.of(PASSWORD_RESET_EXPIRED);
        }
        if (!saved.equals(req.code())) {
            Long t = redis.opsForValue().increment(triesKey(email));
            if (t != null && t == 1) redis.expire(triesKey(email), CODE_TTL);
            throw AuthApiException.of(PASSWORD_RESET_INVALID_CODE);
        }

        Users user = usersRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AuthApiException.of(PASSWORD_RESET_EMAIL_NOT_FOUND));

        // (선택) 정책 검사: 필요 시 강화
        if (!isValidPassword(req.newPassword())) {
            throw AuthApiException.of(PASSWORD_POLICY_VIOLATION);
        }

        user.changePassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);

        try {
            redis.delete(codeKey(email));
            redis.delete(triesKey(email));
            redis.delete(verifiedKey(email));
        } catch (DataAccessException ignored) {}
    }

    /** 로그인 상태에서 비밀번호 변경(본인 인증 완료) */
    @Transactional
    public void change(String email, ChangePasswordRequest req) {
        email = email.toLowerCase();

        if (!req.newPassword().equals(req.confirmPassword())) {
            // 코드 테이블 내에서 가장 가까운 항목 사용(메시지 오버라이드)
            throw AuthApiException.of(PASSWORD_POLICY_VIOLATION);
        }
        if (!isValidPassword(req.newPassword())) {
            throw AuthApiException.of(PASSWORD_POLICY_VIOLATION);
        }

        Users user = usersRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AuthApiException.of(PASSWORD_RESET_EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw AuthApiException.of(PASSWORD_OLD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);
    }

    /** 코드 검증(verified 플래그) 이후 한 번만 변경 */
    @Transactional
    public void changeAfterVerified(String email, ChangePasswordRequest req) {
        email = email.toLowerCase();

        // 1) 검증 완료 플래그 확인
        String v = redis.opsForValue().get(verifiedKey(email));
        if (v == null) {
            throw AuthApiException.of(PASSWORD_RESET_EXPIRED);
        }

        // 2) 새 비밀번호 확인/정책
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw AuthApiException.of(PASSWORD_POLICY_VIOLATION);
        }
        if (!isValidPassword(req.newPassword())) {
            throw AuthApiException.of(PASSWORD_POLICY_VIOLATION);
        }

        // 3) 사용자 조회 & 옛 비밀번호 검사
        Users user = usersRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AuthApiException.of(PASSWORD_RESET_EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw AuthApiException.of(PASSWORD_OLD_MISMATCH);
        }

        // 4) 변경
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);

        // 5) 플래그 소진 (1회성)
        try { redis.delete(verifiedKey(email)); } catch (DataAccessException ignored) {}
    }

    /** 팀 정책에 맞춰 강화 가능 */
    private boolean isValidPassword(String pw) {
        return pw != null && pw.length() >= 8;
    }
}
