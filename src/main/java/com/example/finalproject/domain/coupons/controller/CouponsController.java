package com.example.finalproject.domain.coupons.controller;

import com.example.finalproject.domain.coupons.dto.CouponsDtos;
import com.example.finalproject.domain.coupons.service.CouponsService;
import com.example.finalproject.domain.users.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponsController {

    private final CouponsService couponsService;

    // 쿠폰 발급 (관리자 전용)
    @PostMapping
    public ResponseEntity<CouponsDtos.CouponResponse> createCoupon(
            @RequestBody CouponsDtos.CreateRequest request
    ) {
        CouponsDtos.CouponResponse response = couponsService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 쿠폰 등록
    @PostMapping("/register")
    public ResponseEntity<CouponsDtos.UserCouponResponse> registerUserCoupon(
            @RequestBody CouponsDtos.RegisterUserCouponRequest request
    ) {
        Users user = new Users();
        user.setId(request.getUserId());

        CouponsDtos.UserCouponResponse response = couponsService.registerUserCoupon(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 보유 쿠폰 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<CouponsDtos.UserCouponListResponse> getUserCoupons(
            @PathVariable Long userId
    ) {
        CouponsDtos.UserCouponListResponse response = couponsService.getUserCoupons(userId);
        return ResponseEntity.ok(response);
    }

    // 쿠폰 사용 처리
    @PostMapping("/use/{userCouponId}")
    public ResponseEntity<Void> useCoupon(@PathVariable Long userCouponId) {
        couponsService.useCoupon(userCouponId);
        return ResponseEntity.noContent().build();
    }
}
