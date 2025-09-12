package com.example.finalproject.domain.reviews.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ReviewsCreateRequest
 * -------------------------------------------------
 * - 리뷰 작성 시 클라이언트로부터 전달받는 요청 DTO
 * - 유효성 검증(Validation)을 통해 잘못된 입력을 사전에 차단
 */
@Setter
@Getter
public class ReviewsCreateRequest {

    /**
     * 리뷰를 작성할 대상 주문 ID
     * - 어떤 주문에 대한 리뷰인지 식별하기 위해 필요
     * - 반드시 존재해야 하므로 @NotNull 적용
     */
    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    /**
     * 리뷰 별점 (1~5점 사이)
     * - 최소 1점, 최대 5점으로 제한
     * - 유효하지 않은 별점 입력 방지
     */
    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하이어야 합니다.")
    private Integer rating;

    /**
     * 리뷰 내용
     * - 공백 불가(@NotBlank)
     * - 최대 2000자 제한 (@Size)
     * - 리뷰 작성 시 필수적으로 입력해야 함
     */
    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 2000, message = "리뷰 내용은 2000자 이하로 작성해야 합니다.")
    private String content;

    /**
     * 기본 생성자
     * - DTO 직렬화/역직렬화 시 필요
     */
    public ReviewsCreateRequest() {}
}
