package com.example.finalproject.domain.orders.entity;

import com.example.finalproject.domain.coupons.entity.Coupons;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

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
        WAITING {
            public Set<Status> next() { return Set.of(ACCEPTED, REJECTED, CANCELED); }
        },
        ACCEPTED {
            public Set<Status> next() { return Set.of(COOKING, CANCELED); }
        },
        COOKING {
            public Set<Status> next() { return Set.of(DELIVERING, CANCELED); }
        },
        DELIVERING {
            public Set<Status> next() { return Set.of(COMPLETED, CANCELED); }
        },
        COMPLETED {
            public Set<Status> next() { return Set.of(CANCELED); }
        }, REJECTED, CANCELED;

        public Set<Status> next() {
            return Set.of(); // 기본값
        }

        public boolean canTransitionTo(Status target) {
            return next().contains(target);
        }
    }
}

