package com.example.finalproject.domain.coupons.entity;

import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name= "user_coupons")
@Getter
@NoArgsConstructor
public class UserCoupons {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //유저와 연결 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    //쿠폰과 연결(FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="coupon_id", nullable = false)
    private Coupons coupon;

    @Column(nullable = false)
    private boolean isUsed = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime usedAt;


    // 생성자
    public UserCoupons(Users user, Coupons coupon) {
        this.user = user;
        this.coupon = coupon;
    }

    //쿠폰 사용 처리
    public void useCoupon() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }
}
