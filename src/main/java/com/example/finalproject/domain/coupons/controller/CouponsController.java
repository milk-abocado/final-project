package com.example.finalproject.domain.coupons.controller;

import com.example.finalproject.domain.coupons.dto.CouponsDtos;
import com.example.finalproject.domain.coupons.service.CouponsService;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponsController {

    private final CouponsService couponsService;
    private final UsersRepository usersRepository;

    // 쿠폰 발급 (관리자 전용)
    @PostMapping
    public ResponseEntity<CouponsDtos.CouponResponse> createCoupon(
            @RequestBody CouponsDtos.CreateRequest request
    ) {
        CouponsDtos.CouponResponse response = couponsService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 쿠폰 등록
    @PostMapping("/user-coupons")
    public ResponseEntity<CouponsDtos.UserCouponResponse> registerUserCoupon(
            @RequestBody CouponsDtos.RegisterUserCouponRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );
       Users user = usersRepository.findById(userId).orElse(null);

        CouponsDtos.UserCouponResponse response = couponsService.registerUserCoupon(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 보유 쿠폰 조회
    @GetMapping("/user-coupons/{userId}")
    public ResponseEntity<CouponsDtos.UserCouponListResponse> getUserCoupons(
            @PathVariable Long userId
    ) {
        CouponsDtos.UserCouponListResponse response = couponsService.getUserCoupons(userId);
        return ResponseEntity.ok(response);
    }

    // 쿠폰 사용 처리
    @PostMapping("/user-coupons/use")
    public ResponseEntity<Void> useCoupon(@RequestBody CouponsDtos.UseCouponRequest request) {
        couponsService.useCoupon(request.getUserId(), request.getCouponCode(), request.getOrderId());
        return ResponseEntity.noContent().build();
    }
}