package com.example.finalproject.domain.coupons.repository;

import com.example.finalproject.domain.coupons.entity.UserCoupons;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponsRepository extends JpaRepository<UserCoupons, Long> {
    List<UserCoupons> findByUserId(Long userId); // 유저가 가진 쿠폰 목록 조회
}