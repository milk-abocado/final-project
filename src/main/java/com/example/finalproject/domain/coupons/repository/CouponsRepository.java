package com.example.finalproject.domain.coupons.repository;

import com.example.finalproject.domain.coupons.entity.Coupons;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponsRepository extends JpaRepository<Coupons, Long> {
    Optional<Coupons> findByCode(String code); // 쿠폰 코드로 조회
    boolean existsByCode(String code);
}