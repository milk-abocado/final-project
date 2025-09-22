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

    // 본인만 프로필 수정 가능 (인증 주체의 userId와 path {id} 비교)
    @PatchMapping("/{id}/profile")
    public ResponseEntity<UserDetailResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserProfileUpdateRequest request,
            // CustomUserDetails에 getUserId()가 있다고 가정하고 SpEL로 바로 추출
            @AuthenticationPrincipal(expression = "userId") Long authUserId
    ) {
        if (authUserId == null || !id.equals(authUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 프로필만 수정할 수 있습니다.");
        }

        var updated = usersService.updateProfile(id, request);
        return ResponseEntity.ok(updated);
    }
}