package com.example.finalproject.domain.users.dto;

import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String email,
        String name,
        String nickname,
        UserRole role,
        boolean socialLogin,
        String phoneNumber,
        LocalDateTime createdAt
) {
    public static UserSummaryResponse from(Users u) {
        return new UserSummaryResponse(
                u.getId(), u.getEmail(), u.getName(), u.getNickname(),
                u.getRole(), u.isSocialLogin(), u.getPhoneNumber(), u.getCreatedAt()
        );
    }
}