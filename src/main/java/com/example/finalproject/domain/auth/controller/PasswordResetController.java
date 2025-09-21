package com.example.finalproject.domain.auth.controller;

import com.example.finalproject.domain.auth.dto.password.ChangePasswordRequest;
import com.example.finalproject.domain.auth.dto.password.VerifyResetCodeRequest;
import com.example.finalproject.domain.auth.dto.password.SendResetCodeRequest;
import com.example.finalproject.domain.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService service;
    private final PasswordResetService passwordResetService;


    // 코드 발송 요청
    @PostMapping("/reset/code")
    public ResponseEntity<?> sendCode(@RequestBody @Valid SendResetCodeRequest req) {
        service.sendCode(req);
        // 이메일 존재 여부는 숨기고 동일 응답
        return ResponseEntity.ok().build();
    }

    // 코드 사전 검증
    @PostMapping("/reset/verify")
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyResetCodeRequest req) {
        boolean ok = service.verifyCode(req);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    // 최종 비밀번호 변경
    @PostMapping("/reset/confirm")
    public ResponseEntity<?> changeAfterVerified(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody @Valid ChangePasswordRequest req
    ) {
        passwordResetService.changeAfterVerified(principal.getUsername(), req);
        return ResponseEntity.ok().build();
    }
}