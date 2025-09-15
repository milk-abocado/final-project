package com.example.finalproject.domain.stores.entity;

import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stores",
        indexes = {
                @Index(name = "idx_stores_active_name", columnList = "active,name"),
                @Index(name = "idx_stores_active_lat_lng", columnList = "active,latitude,longitude")
        })
public class Stores {

    // 기본 키 (PK), Auto Increment
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Users 엔티티와 다대일 관계 (한 유저는 여러 가게를 소유 가능)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false) // FK 매핑 (users.id)
    private Users owner;

    // 가게 이름
    @Column(nullable = false, length = 100)
    private String name;

    // 가게 주소
    @Column(nullable = false)
    private String address;

    // 위도/경도(지오코딩으로 세팅)
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    // 가게 카테고리
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StoreCategoryLink> categoryLinks;

    // 최소 주문 금액
    @Column(nullable = false)
    private Integer minOrderPrice;

    // 영업 시작 시간
    @Column(nullable = false)
    private LocalTime opensAt;

    // 영업 종료 시간
    @Column(nullable = false)
    private LocalTime closesAt;

    // 배달비 (0 이상, null 불가)
    @Column(nullable = false)
    private Integer deliveryFee;

    // active (영업 여부) → true = 영업, false = 폐업
    @Column(nullable = false)
    private Boolean active = true;

    // 폐업 처리 시각
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    // 생성 시각 (insert 시 고정)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시각 (update 시 갱신)
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * INSERT 되기 전 자동 실행
     * - createdAt, updatedAt 현재 시각으로 세팅
     * - deliveryFee 값이 null이면 0으로 초기화
     */
    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (deliveryFee == null) deliveryFee = 0;
    }

    /**
     * UPDATE 되기 전 자동 실행
     * - updatedAt 현재 시각으로 갱신
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** 폐업 처리 */
    public void retire() {
        this.active = false;
        this.retiredAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
}
