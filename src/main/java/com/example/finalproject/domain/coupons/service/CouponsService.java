package com.example.finalproject.domain.coupons.service;


import com.example.finalproject.domain.coupons.dto.CouponsDtos;
import com.example.finalproject.domain.coupons.entity.CouponType;
import com.example.finalproject.domain.coupons.entity.Coupons;
import com.example.finalproject.domain.coupons.entity.UserCoupons;
import com.example.finalproject.domain.coupons.exception.CouponException;
import com.example.finalproject.domain.coupons.repository.CouponsRepository;
import com.example.finalproject.domain.coupons.repository.UserCouponsRepository;
import com.example.finalproject.domain.users.entity.Users;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponsService {

    private final CouponsRepository couponsRepository;
    private final UserCouponsRepository userCouponsRepository;


     //쿠폰 발급 (관리자용)
    @Transactional
    public CouponsDtos.CouponResponse createCoupon(CouponsDtos.CreateRequest request) {

        if (couponsRepository.existsByCode(request.getCode())) {
            throw new CouponException("이미 존재하는 쿠폰 코드입니다: " + request.getCode());
        }
        Coupons coupon = new Coupons(
                request.getCode(),
                Enum.valueOf(CouponType.class, request.getType()),
                request.getDiscountValue(),
                request.getMaxDiscount(),
                request.getExpireAt()
        );

        Coupons saved = couponsRepository.save(coupon);

        CouponsDtos.CouponResponse response = new CouponsDtos.CouponResponse();
        response.setCouponId(saved.getId());
        response.setCode(saved.getCode());
        response.setType(saved.getType().name());
        response.setDiscountValue(saved.getDiscountValue());
        response.setMaxDiscount(saved.getMaxDiscount());
        response.setExpireAt(saved.getExpireAt());
        response.setCreatedAt(saved.getCreatedAt());

        return response;
    }


     // 사용자 쿠폰 등록
    @Transactional
    public CouponsDtos.UserCouponResponse registerUserCoupon(Users user, CouponsDtos.RegisterUserCouponRequest request) {
        Coupons coupon = couponsRepository.findByCode(request.getCouponCode())
                .orElseThrow(() -> new CouponException("쿠폰이 존재하지 않습니다."));

        if (coupon.getExpireAt() != null && coupon.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new CouponException("만료된 쿠폰입니다.");
        }

        UserCoupons userCoupon = new UserCoupons(user, coupon);
        UserCoupons saved = userCouponsRepository.save(userCoupon);

        CouponsDtos.UserCouponResponse response = new CouponsDtos.UserCouponResponse();
        response.setUserCouponId(saved.getId());
        response.setUserId(user.getId());
        response.setCouponId(coupon.getId());
        response.setCouponCode(coupon.getCode());
        response.setUsed(saved.isUsed());
        response.setCreatedAt(saved.getCreatedAt());
        response.setUsedAt(saved.getUsedAt());

        return response;
    }


     //유저 보유 쿠폰 조회
    public CouponsDtos.UserCouponListResponse getUserCoupons(Long userId) {
        List<UserCoupons> userCoupons = userCouponsRepository.findByUserId(userId);

        List<CouponsDtos.UserCouponResponse> coupons = userCoupons.stream().map(uc -> {
            CouponsDtos.UserCouponResponse dto = new CouponsDtos.UserCouponResponse();
            dto.setUserCouponId(uc.getId());
            dto.setUserId(uc.getUser().getId());
            dto.setCouponId(uc.getCoupon().getId());
            dto.setCouponCode(uc.getCoupon().getCode());
            dto.setUsed(uc.isUsed());
            dto.setCreatedAt(uc.getCreatedAt());
            dto.setUsedAt(uc.getUsedAt());
            return dto;
        }).toList();

        CouponsDtos.UserCouponListResponse response = new CouponsDtos.UserCouponListResponse();
        response.setUserId(userId);
        response.setCoupons(coupons);

        return response;
    }


     // 쿠폰 사용 처리
     @Transactional
     public void useCoupon(Long userId, String couponCode, Long orderId) {
         UserCoupons userCoupon = userCouponsRepository
                 .findByUserIdAndCouponCode(userId, couponCode)
                 .orElseThrow(() -> new CouponException("사용자 쿠폰이 존재하지 않습니다."));

         if (userCoupon.isUsed()) {
             throw new CouponException("이미 사용된 쿠폰입니다.");
         }

         if (userCoupon.getCoupon().getExpireAt() != null &&
                 userCoupon.getCoupon().getExpireAt().isBefore(LocalDateTime.now())) {
             throw new CouponException("만료된 쿠폰입니다.");
         }

         userCoupon.useCoupon();
         userCouponsRepository.save(userCoupon);
     }
}
