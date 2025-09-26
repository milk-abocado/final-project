package com.example.finalproject.domain.users.dto;

import com.example.finalproject.domain.users.entity.Users;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserDetailResponse {
    private Long id;
    private String email;
    private String nickname;

    private String phoneNumber;
    private String address;
    private String addressDetail;
    private String zipCode;

    public static UserDetailResponse from(Users u) {
        return UserDetailResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nickname(u.getNickname())
                .phoneNumber(u.getPhoneNumber())
                .address(u.getAddress())
                .addressDetail(u.getAddressDetail())
                .zipCode(u.getZipCode())
                .build();
    }
}