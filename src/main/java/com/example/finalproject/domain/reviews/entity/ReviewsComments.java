package com.example.finalproject.domain.reviews.entity;

import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ReviewsComments (리뷰 답글 엔티티)
 * -------------------------------------------------
 * - 특정 리뷰(Reviews)에 대해 사장님이 작성한 답글(코멘트)을 저장
 * - 리뷰 1개당 답글은 최대 1개만 허용 (unique 제약 조건)
 * - 작성자(사장님), 가게, 내용, 생성/수정/삭제 상태 등을 관리
 */
@Entity
@Table(
        name = "review_comments",
        uniqueConstraints = {
                // 리뷰 1개에는 답글 1개만 달 수 있도록 제약 조건 설정
                @UniqueConstraint(name = "uk_reviews_comments_review", columnNames = "review_id")
        }
)
@Getter
@Setter
public class ReviewsComments {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 리뷰 (1:1 관계)
     * - 리뷰 하나에 답글은 최대 1개
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Reviews review;

    /**
     * 답글 작성자 (사장님, Users 엔티티 참조)
     * - 다수의 답글을 작성할 수 있으므로 ManyToOne
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;

    /**
     * 답글이 속한 가게 (Stores 엔티티 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    /** 답글 내용 (최대 1000자) */
    @Column(nullable = false, length = 1000)
    private String content;

    /** 생성일시 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 수정일시 */
    private LocalDateTime updatedAt;

    /** 삭제 여부 */
    @Column(nullable = false)
    private boolean isDeleted = false;

    /** 삭제일시 */
    private LocalDateTime deletedAt;

    /**
     * 엔티티 저장 시(createdAt 자동 세팅)
     */
    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
