package com.example.finalproject.domain.reviews.entity;

import com.example.finalproject.domain.orders.entity.Orders;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Reviews 엔티티
 * -------------------------------------------------
 * - 유저가 특정 가게(Store)와 주문(Order)에 대해 작성한 리뷰를 저장
 * - 별점, 내용, 작성일시, 삭제 여부 등을 관리
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reviews")
public class Reviews {

    /**
     * 리뷰 삭제 주체
     * - USER  : 작성자 본인
     * - OWNER : 가게 오너
     * - ADMIN : 시스템/관리자 (확장성 고려)
     */
    public enum DeletedBy { USER, OWNER, ADMIN }

    /** 리뷰 고유 ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 리뷰가 작성된 가게 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    /** 리뷰 작성자 (유저) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /** 리뷰가 연결된 주문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    /** 별점 (1~5점) */
    @Column(nullable = false)
    private Integer rating; // 1 ~ 5

    /** 리뷰 내용 (최대 2000자) */
    @Column(length = 2000)
    private String content;

    /** 리뷰 작성일시 (생성 시 자동 세팅) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 리뷰 수정일시 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 리뷰 삭제 여부 (Soft Delete 플래그) */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /** 삭제 시각(소프트 삭제 시 세팅) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 누가 삭제했는지(정책 분기/감사용) */
    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_by", length = 16)
    private DeletedBy deletedBy;

    /** 생성 시 자동 세팅 */
    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;   // 최초 생성 시에도 동일하게 세팅
    }

    /** 수정 시 자동 세팅 */
    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 오너/운영자 소프트 삭제 */
    public void softDeleteByOwner() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = DeletedBy.OWNER;
    }

    /** 복구 (보관기간 내에서만 서비스단에서 허용) */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public void update(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
