package com.example.finalproject.domain.users.controller;

import com.example.finalproject.domain.users.dto.UserDetailResponse;
import com.example.finalproject.domain.users.dto.UserProfileUpdateRequest;
import com.example.finalproject.domain.users.dto.UserSummaryResponse;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import com.example.finalproject.domain.users.service.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final UsersRepository userRepository;
    private final UsersService usersService;

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> list() {
        var users = userRepository.findAll()
                .stream()
                .map(UserSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{email:.+}")
    public ResponseEntity<UserDetailResponse> detail(
            @PathVariable String email,
            // 토큰의 username(=email)을 바로 주입
            @AuthenticationPrincipal(expression = "username") String authEmail,
            Authentication authentication
    ) {
        if (authEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 관리자 권한 보유 여부(권한이 'ADMIN' 또는 'ROLE_ADMIN' 둘 중 하나여도 통과)
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ADMIN") || a.equals("ROLE_ADMIN"));

        // 본인 이메일이 아니고 관리자도 아니면 403
        if (!isAdmin && !email.equalsIgnoreCase(authEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 정보만 조회할 수 있습니다.");
        }

        Users user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));

        return ResponseEntity.ok(UserDetailResponse.from(user));
    }

    // 본인만 프로필 수정 가능 (인증 주체의 userEmail와 path {Email} 비교)
    @PatchMapping("/{email:.+}/profile")
    public ResponseEntity<?> updateProfileByEmail(
            @PathVariable String email,
            @Valid @RequestBody UserProfileUpdateRequest req,
            @AuthenticationPrincipal(expression = "username") String authEmail
    ) {
        if (authEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!email.equalsIgnoreCase(authEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인만 수정할 수 있습니다.");
        }
        return ResponseEntity.ok(usersService.updateProfileByEmail(email, req));
    }
}

