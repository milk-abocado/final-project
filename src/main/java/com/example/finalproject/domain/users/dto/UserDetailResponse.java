package com.example.finalproject.domain.users.dto;

import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;

import java.time.LocalDateTime;

public record UserDetailResponse(
        Long id,
        String email,
        String name,
        String nickname,
        UserRole role,
        boolean socialLogin,
        String phoneNumber,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserDetailResponse from(Users u) {
        return new UserDetailResponse(
                u.getId(), u.getEmail(), u.getName(), u.getNickname(),
                u.getRole(), u.isSocialLogin(), u.getPhoneNumber(),
                u.isDeleted(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }
}