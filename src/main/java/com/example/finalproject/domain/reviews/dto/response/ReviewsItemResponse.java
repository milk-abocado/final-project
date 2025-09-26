package com.example.finalproject.domain.reviews.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ReviewsItemResponse {

    private Long id;                // 리뷰 고유 ID
    private Long storeId;           // 리뷰가 작성된 가게 ID
    private Long userId;            // 리뷰 작성자(유저) ID
    private Long orderId;           // 리뷰가 연결된 주문 ID
    private Integer rating;         // 리뷰 별점 (1~5점)
    private String content;         // 리뷰 내용
    private LocalDateTime createdAt;    // 리뷰 작성일시

    // 기본 생성자
    public ReviewsItemResponse() {}

    /**
     * 전체 필드 생성자
     * - 모든 응답 값을 한번에 설정할 때 사용
     */
    public ReviewsItemResponse(Long id, Long storeId, Long userId, Long orderId, Integer rating, String content, LocalDateTime createdAt) {
        this.id = id;
        this.storeId = storeId;
        this.userId = userId;
        this.orderId = orderId;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }
}
