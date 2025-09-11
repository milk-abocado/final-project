package com.example.finalproject.domain.users.controller;


import com.example.finalproject.domain.users.dto.UserDetailResponse;
import com.example.finalproject.domain.users.dto.UserSummaryResponse;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final UserRepository userRepository;

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
}