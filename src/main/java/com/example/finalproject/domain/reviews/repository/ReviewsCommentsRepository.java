package com.example.finalproject.domain.reviews.repository;

import com.example.finalproject.domain.reviews.entity.ReviewsComments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ReviewsCommentsRepository
 * -------------------------------------------------
 * - 리뷰 답글(ReviewsComments) 엔티티 전용 JPA Repository
 * - 기본적인 CRUD 외에 소프트 삭제(isDeleted = false) 조건을 포함한 조회 메서드 제공
 */
public interface ReviewsCommentsRepository extends JpaRepository<ReviewsComments, Long> {

    /**
     * 특정 리뷰 ID에 대한 답글 단건 조회
     * - isDeleted = false 조건 포함 (삭제되지 않은 답글만 조회)
     *
     * @param reviewId 리뷰 ID
     * @return Optional<ReviewsComments>
     */
    Optional<ReviewsComments> findByReview_IdAndIsDeletedFalse(Long reviewId);

    /**
     * 특정 리뷰 ID에 대해 삭제되지 않은 답글이 존재하는지 여부 확인
     *
     * @param reviewId 리뷰 ID
     * @return true / false
     */
    boolean existsByReview_IdAndIsDeletedFalse(Long reviewId);

    /**
     * 여러 리뷰 ID 목록에 대해 삭제되지 않은 답글들을 한 번에 조회
     *
     * @param reviewIds 리뷰 ID 리스트
     * @return List<ReviewsComments>
     */
    List<ReviewsComments> findByReview_IdInAndIsDeletedFalse(List<Long> reviewIds);
}
