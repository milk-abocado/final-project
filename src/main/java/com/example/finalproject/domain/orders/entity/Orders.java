package com.example.finalproject.domain.orders.entity;

import com.example.finalproject.domain.coupons.entity.Coupons;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    @Column(nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "applied_coupon_id")
    private Coupons appliedCoupon;

    private Integer usedPoints;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        WAITING, ACCEPTED, COOKING, DELIVERING, COMPLETED, REJECTED, CANCELED
    }
}

