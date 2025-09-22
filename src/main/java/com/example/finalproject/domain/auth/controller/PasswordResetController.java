package com.example.finalproject.domain.auth.controller;

import com.example.finalproject.domain.common.api.ErrorCode;
import com.example.finalproject.domain.common.api.AppException;
import com.example.finalproject.domain.auth.dto.password.ChangePasswordRequest;
import com.example.finalproject.domain.auth.dto.password.ConfirmResetPasswordRequest;
import com.example.finalproject.domain.auth.dto.password.SendResetCodeRequest;
import com.example.finalproject.domain.auth.dto.password.VerifyResetCodeRequest;
import com.example.finalproject.domain.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping(value = "/reset/code",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendCode(@RequestBody @Valid SendResetCodeRequest req) {
        passwordResetService.sendCode(req);
        return ResponseEntity.ok(Map.of("message", "코드 전송 요청이 접수되었습니다."));
    }

    @PostMapping(value = "/reset/verify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyResetCodeRequest req) {
        boolean ok = passwordResetService.verifyCode(req);
        if (!ok) {
            // 만료/오류를 서비스에서 구분해 AppException으로 던지도록 바꾸면 더 정확한 코드 매핑 가능
            throw new AppException(ErrorCode.INVALID_CODE);
        }
        return ResponseEntity.ok(Map.of("message", "코드가 확인되었습니다."));
    }

    @PostMapping(value = "/reset/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirm(@RequestBody @Valid ConfirmResetPasswordRequest req) {
        passwordResetService.confirm(req); // 코드 검증 + 비번 변경 + 코드 소진
        return ResponseEntity.ok(Map.of("message", "updated"));
    }

    @PostMapping(value = "/change",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> change(@AuthenticationPrincipal String email,
                                    @RequestBody @Valid ChangePasswordRequest req) {
        if (email == null) throw new AppException(ErrorCode.UNAUTHORIZED);
        passwordResetService.change(email, req);
        return ResponseEntity.ok(Map.of("message", "updated"));
    }

    /* ================= 로컬 예외 핸들러 (이 컨트롤러에만 적용) ================ */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleApp(AppException e) {
        var ec = e.error;
        return ResponseEntity.status(ec.status)
                .body(Map.of("code", ec.code, "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleBind(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_REQUEST", "message", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegal(IllegalStateException e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("요청이 너무 많습니다")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("code", "RATE_LIMIT", "message", m));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_REQUEST", "message", m.isEmpty() ? "요청이 잘못되었습니다." : m));
    }
}
