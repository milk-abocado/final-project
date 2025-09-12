package com.example.finalproject.domain.reviews.repository;

import com.example.finalproject.domain.reviews.entity.Reviews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ReviewsRepository
 * -------------------------------------------------
 * - 리뷰(Reviews) 엔티티에 대한 데이터 접근 계층
 * - JPA 메서드 네이밍 규칙을 활용하여 쿼리 자동 생성
 */
public interface ReviewsRepository extends JpaRepository<Reviews, Long> {

    /**
     * 특정 가게에 대한 리뷰 목록 조회
     * - 삭제되지 않은 리뷰만 조회 (isDeleted = false)
     * - 별점 범위(minRating ~ maxRating) 필터링
     * - 최신순(createdAt DESC) 정렬
     *
     * @param storeId    가게 ID
     * @param minRating  최소 별점
     * @param maxRating  최대 별점
     * @param pageable   페이지네이션 정보
     * @return           페이징된 리뷰 목록
     */
    Page<Reviews> findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
            Long storeId, Integer minRating, Integer maxRating, Pageable pageable
    );

    /**
     * 특정 주문에 대해 유저가 이미 리뷰를 작성했는지 확인
     * - 중복 리뷰 작성 방지용
     *
     * @param orderId  주문 ID
     * @param userId   유저 ID
     * @return         리뷰 존재 여부 (true: 이미 존재)
     */
    boolean existsByOrderIdAndUserId(Long orderId, Long userId);

    /**
     * 특정 유저가 특정 가게에 작성한 리뷰 목록 조회
     * - 삭제되지 않은 리뷰만 조회 (isDeleted = false)
     * - 별점 범위(minRating ~ maxRating) 필터링
     * - 최신순(createdAt DESC) 정렬
     *
     * @param userId    유저 ID
     * @param storeId   가게 ID
     * @param minRating 최소 별점
     * @param maxRating 최대 별점
     * @param pageable  페이지네이션 정보
     * @return          페이징된 리뷰 목록
     */
    Page<Reviews> findByUserIdAndStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
            Long userId, Long storeId, Integer minRating, Integer maxRating, Pageable pageable
    );
}

