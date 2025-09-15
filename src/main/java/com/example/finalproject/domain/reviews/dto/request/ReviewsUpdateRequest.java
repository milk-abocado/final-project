package com.example.finalproject.domain.reviews.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ReviewsUpdateRequest
 * -------------------------------------------------
 * - 리뷰 수정 시 클라이언트로부터 전달받는 요청 DTO
 * - 기존 리뷰의 별점과 내용을 수정할 때 사용
 * - 주문 ID 등은 변경할 수 없음 (생성 시에만 지정)
 */
@Setter
@Getter
public class ReviewsUpdateRequest {

    /**
     * 리뷰 별점 (1~5점 사이)
     * - 최소 1점, 최대 5점으로 제한
     * - 반드시 존재해야 하므로 @NotNull 적용
     */
    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하이어야 합니다.")
    private Integer rating;

    /**
     * 리뷰 내용
     * - 공백 불가(@NotBlank)
     * - 최대 2000자 제한 (@Size)
     * - 수정 시 반드시 입력해야 함
     */
    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 2000, message = "리뷰 내용은 2000자 이하로 작성해야 합니다.")
    private String content;

    /**
     * 기본 생성자
     * - DTO 직렬화/역직렬화 시 필요
     */
    public ReviewsUpdateRequest() {}
}
