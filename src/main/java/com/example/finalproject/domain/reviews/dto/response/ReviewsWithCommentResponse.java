package com.example.finalproject.domain.reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ReviewsWithCommentResponse
 * -------------------------------------------------
 * - 특정 리뷰(Review)와 해당 리뷰에 달린 사장님 댓글(OwnerComment)을 함께 응답하기 위한 DTO
 * - 리뷰 정보 + (nullable) 사장님 답글 정보를 묶어서 반환
 */
@Getter
@AllArgsConstructor
public class ReviewsWithCommentResponse {
    private Long reviewId;              // 리뷰 ID
    private Long storeId;               // 리뷰가 속한 가게 ID
    private Long userId;                // 리뷰 작성자(고객) ID
    private int rating;                 // 별점 (1~5)
    private String content;             // 리뷰 본문 내용
    private LocalDateTime createdAt;    // 리뷰 작성일

    /**
     * 사장님 댓글 정보 (null 가능)
     * - 리뷰에 답글이 없을 경우 null 반환
     */
    private OwnerCommentDto ownerComment;

    //  리뷰에 달린 사장님 댓글의 상세 응답 DTO
    @Getter
    @AllArgsConstructor
    public static class OwnerCommentDto {
        private Long commentId;             // 댓글 ID
        private Long ownerId;               // 댓글 작성자(사장님) ID
        private String content;             // 댓글 본문 내용
        private LocalDateTime createdAt;    // 댓글 작성일
        private LocalDateTime updatedAt;    // 댓글 수정일 (최초 작성 이후 수정된 경우만 값 있음)
    }
}
