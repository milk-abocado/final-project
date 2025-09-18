package com.example.finalproject.domain.reviews.repository;

import com.example.finalproject.domain.reviews.entity.ReviewsComments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ReviewsCommentsRepository
 * -------------------------------------------------
 * - 리뷰 답글(ReviewsComments) 전용 JPA Repository
 * - 기본 CRUD 메서드 외에 소프트 삭제(isDeleted=false) 조건을 반영한 조회/검증 메서드 제공
 * - 오너(owner)와 스토어(store) 기준으로 접근 제어를 위한 쿼리 메서드 포함
 */
public interface ReviewsCommentsRepository extends JpaRepository<ReviewsComments, Long> {

    /**
     * 특정 리뷰(reviewId)에 대해 삭제되지 않은 사장님 답글이 존재하는지 여부 확인
     *
     * @param reviewId 리뷰 ID
     * @return true: 삭제되지 않은 답글 존재 / false: 없음
     */
    boolean existsByReview_IdAndIsDeletedFalse(Long reviewId);

    /**
     * 여러 리뷰 ID에 대해 삭제되지 않은 답글을 한 번에 조회
     * - 리뷰 목록에 매핑된 모든 활성 답글 조회용
     *
     * @param reviewIds 리뷰 ID 리스트
     * @return 삭제되지 않은 답글 리스트
     */
    List<ReviewsComments> findByReview_IdInAndIsDeletedFalse(List<Long> reviewIds);

    /**
     * 특정 리뷰 + 스토어 + 오너 조건을 모두 만족하는 삭제되지 않은 답글 단건 조회
     * - 오너가 자신의 가게(storeId)에 달린 본인(ownerId)의 답글만 수정/삭제 가능하도록 보장
     *
     * @param reviewId 리뷰 ID
     * @param storeId  가게 ID
     * @param ownerId  오너(사용자) ID
     * @return Optional<ReviewsComments>
     */
    Optional<ReviewsComments> findByReview_IdAndStore_IdAndOwner_IdAndIsDeletedFalse(
            Long reviewId, Long storeId, Long ownerId
    );
}
