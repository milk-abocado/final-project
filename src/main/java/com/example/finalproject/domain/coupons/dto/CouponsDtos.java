package com.example.finalproject.domain.coupons.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class CouponsDtos {

    // 쿠폰 생성 요청 (관리자용)
    @Getter
    @Setter
    public static class CreateRequest {
        private String code;
        private String type;          // RATE / AMOUNT
        private int discountValue;    // 할인 값
        private Integer maxDiscount;  // 할인율 쿠폰일 경우 최대 할인 금액
        private LocalDateTime expireAt;
    }

    // 쿠폰 발급 응답
    @Getter
    @Setter
    public static class CouponResponse {
        private Long couponId;
        private String code;
        private String type;
        private int discountValue;
        private Integer maxDiscount;
        private LocalDateTime expireAt;
        private LocalDateTime createdAt;
    }

    // 사용자 쿠폰 등록 요청
    @Getter
    @Setter
    public static class RegisterUserCouponRequest {
        private Long userId;
        private String couponCode; // 등록할 쿠폰 코드
    }

    // 사용자 쿠폰 정보 (단건)
    @Getter
    @Setter
    public static class UserCouponResponse {
        private Long userCouponId;
        private Long userId;
        private Long couponId;
        private String couponCode;
        private boolean isUsed;
        private LocalDateTime createdAt;
        private LocalDateTime usedAt;
    }

    // 유저 보유 쿠폰 목록 (다건)
    @Getter
    @Setter
    public static class UserCouponListResponse {
        private Long userId;
        private List<UserCouponResponse> coupons;
    }
}
