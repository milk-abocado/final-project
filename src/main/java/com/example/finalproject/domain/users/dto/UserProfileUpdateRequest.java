package com.example.finalproject.domain.users.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserProfileUpdateRequest {

    @Size(max = 50)
    private String nickname;

    @Pattern(
            regexp = "^0\\d{1,2}-?\\d{3,4}-?\\d{4}$",
            message = "휴대전화 형식이 올바르지 않습니다. 예) 010-1234-5678"
    )
    private String phoneNumber;

    //  주소 필드
    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String addressDetail;

    @Size(max = 10)
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
    private String zipCode;
}

