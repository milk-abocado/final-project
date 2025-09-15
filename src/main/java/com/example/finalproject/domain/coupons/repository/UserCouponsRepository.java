package com.example.finalproject.domain.coupons.repository;

import com.example.finalproject.domain.coupons.entity.UserCoupons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponsRepository extends JpaRepository<UserCoupons, Long> {
    List<UserCoupons> findByUserId(Long userId); // 유저가 가진 쿠폰 목록 조회

    // userId와 couponCode로 사용자 쿠폰 조회
    @Query("SELECT uc FROM UserCoupons uc " +
            "JOIN uc.coupon c " +
            "WHERE uc.user.id = :userId AND c.code = :couponCode")
    Optional<UserCoupons> findByUserIdAndCouponCode(@Param("userId") Long userId,
                                                    @Param("couponCode") String couponCode);
}