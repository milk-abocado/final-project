package com.example.finalproject.domain.coupons.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor
public class Coupons {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // 쿠폰 코드

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    private CouponType type; // Rate, Amount

    @Column(nullable = false)
    private int discountValue; // 할인 값
    private Integer maxDiscount; //최대 할인 금액
    private LocalDateTime expireAt; // 만료일자
    private LocalDateTime createdAt = LocalDateTime.now();//생성일자

    //생성자
    public Coupons(String code, CouponType type, Integer discountValue, Integer maxDiscount, LocalDateTime expireAt) {
        this.code = code;
        this.type = type;
        this.discountValue = discountValue;
        this.maxDiscount = maxDiscount;
        this.expireAt = expireAt;
    }
}
