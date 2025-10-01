package com.example.finalproject.domain.reviews.repository;

import com.example.finalproject.domain.reviews.entity.ReviewsComments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ReviewsCommentsRepository
 * -------------------------------------------------
 * - 리뷰 답글(ReviewsComments) 전용 JPA Repository
 * - 기본 CRUD 외에 soft delete(isDeleted=false) 조건을 반영한 조회/검증 메서드 제공
 * - 오너(owner)와 스토어(store) 기준 접근 제어 보장
 */
public interface ReviewsCommentsRepository extends JpaRepository<ReviewsComments, Long> {

    boolean existsByReview_IdAndIsDeletedFalse(Long reviewId);

    List<ReviewsComments> findByReview_IdInAndIsDeletedFalse(List<Long> reviewIds);

    /**
     * 오너 본인 댓글 단건 조회(리뷰 기준)
     * - 삭제되지 않은 댓글만
     * - 삭제/수정에서 "내 댓글 아님" 상황을 404로 분리 처리하기 위해 사용
     */
    Optional<ReviewsComments> findByReview_IdAndOwner_IdAndIsDeletedFalse(Long reviewId, Long ownerId);

    /**
     * 리뷰 + 스토어 + 오너로 단건 조회
     * - 스토어 일치까지 강하게 보장해야 할 때 사용(정합성 강화)
     */
    Optional<ReviewsComments> findByReview_IdAndStore_IdAndOwner_IdAndIsDeletedFalse(
            Long reviewId, Long storeId, Long ownerId
    );

    /**
     * 리뷰에 달린 삭제되지 않은 댓글 단건 조회
     * - 존재 여부(404)만 빠르게 확인할 때 사용
     */
    Optional<ReviewsComments> findByReview_IdAndIsDeletedFalse(Long reviewId);
}
