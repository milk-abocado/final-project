package com.example.finalproject.domain.users.controller;

import com.example.finalproject.domain.auth.dto.UserProfileResponse;
import com.example.finalproject.domain.auth.dto.UserUpdateRequest;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
class UserController {
    private final UsersRepository usersRepository;


    @GetMapping
    public ResponseEntity<UserProfileResponse> me(@RequestHeader("X-USER-ID") Long userId){
        Users u = usersRepository.findByIdAndDeletedFalse(userId).orElseThrow();
        return ResponseEntity.ok(UserProfileResponse.builder()
                .userId(u.getId()).email(u.getEmail()).nickname(u.getNickname())
                .role(u.getRole().name()).build());
    }


    @PatchMapping
    public ResponseEntity<?> update(@RequestHeader("X-USER-ID") Long userId,
                                    @RequestBody UserUpdateRequest req){
        Users u = usersRepository.findByIdAndDeletedFalse(userId).orElseThrow();
        if(req.getNickname()!=null) u.setNickname(req.getNickname());
        return ResponseEntity.ok(Map.of("message","updated"));
    }
}