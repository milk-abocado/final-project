package com.example.finalproject.domain.users.controller;

import com.example.finalproject.config.CustomUserPrincipal;
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

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponse> detail(@PathVariable Long id) {
        Users user = userRepository.findById(id)
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

