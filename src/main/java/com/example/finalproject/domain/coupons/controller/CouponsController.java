package com.example.finalproject.domain.coupons.controller;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import com.example.finalproject.domain.coupons.dto.CouponsDtos;
import com.example.finalproject.domain.coupons.exception.CouponErrorCode;
import com.example.finalproject.domain.coupons.exception.CouponException;
import com.example.finalproject.domain.coupons.service.CouponsService;
import com.example.finalproject.domain.users.UserRole;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponsController {

    private final CouponsService couponsService;
    private final UsersRepository usersRepository;

    private Long verifiedUser(Authentication authentication) {
        // 로그인한 사용자의 userId 가져오기
        Long userId = Long.valueOf(
                ((Map<String, Object>) authentication.getDetails()).get("uid").toString()
        );

        // OWNER 권한 접근 방지
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"))) {
            throw new AccessDeniedException("OWNER는 쿠폰에 접근할 수 없습니다.");
        }

        return userId;
    }

    private void verifiedADMIN(Authentication authentication) {
        // OWNER 접근 차단
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("OWNER"))) {
            throw new AccessDeniedException("OWNER는 쿠폰에 접근할 수 없습니다.");
        }

        // USER 접근 차단
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("USER"))) {
            throw new AccessDeniedException("USER는 접근할 수 없습니다.");
        }
    }

    // 쿠폰 발급 (관리자 전용)
    @PostMapping
    public ResponseEntity<CouponsDtos.CouponResponse> createCoupon(
            @RequestBody CouponsDtos.CreateRequest request,
            Authentication authentication
    ) {
        verifiedADMIN(authentication);
        CouponsDtos.CouponResponse response = couponsService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 쿠폰 등록
    @PostMapping("/user-coupons")
    public ResponseEntity<CouponsDtos.UserCouponResponse> registerUserCoupon(
            @RequestBody CouponsDtos.RegisterUserCouponRequest request,
            Authentication authentication
    ) {
        Long userId = verifiedUser(authentication);
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.USER_NOT_FOUND));

        CouponsDtos.UserCouponResponse response = couponsService.registerUserCoupon(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 보유 쿠폰 조회
    @GetMapping("/user-coupons/{userId}")
    public ResponseEntity<CouponsDtos.UserCouponListResponse> getUserCoupons(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long checkUserId = verifiedUser(authentication);
        Users user = usersRepository.findById(checkUserId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.USER && !user.getId().equals(userId)) {
            throw new AccessDeniedException("본인의 쿠폰만 조회할 수 있습니다.");
        }

        CouponsDtos.UserCouponListResponse response = couponsService.getUserCoupons(userId);
        return ResponseEntity.ok(response);
    }

    // 쿠폰 사용 처리
    @PostMapping("/user-coupons/use")
    public ResponseEntity<Void> useCoupon(
            @RequestBody CouponsDtos.UseCouponRequest request,
            Authentication authentication
    ) {
        verifiedUser(authentication);
        couponsService.useCoupon(request.getUserId(), request.getCouponCode(), request.getOrderId());
        return ResponseEntity.noContent().build();
    }
}
