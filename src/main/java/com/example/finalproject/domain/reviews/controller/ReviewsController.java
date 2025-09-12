package com.example.finalproject.domain.reviews.controller;

import com.example.finalproject.domain.reviews.dto.request.ReviewsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsItemResponse;
import com.example.finalproject.domain.reviews.dto.response.ReviewsUpdateResponse;
import com.example.finalproject.domain.reviews.service.ReviewsService;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ReviewsController
 * ----------------------------------------------------------------------------
 * - 리뷰 생성/조회/수정/삭제/복구 HTTP 엔드포인트 제공
 * - 인증/인가(헤더의 X-User-Id, X-Role)에 따라 서비스 레이어 호출 분기
 * - 공통: 경로 변수의 {storeId}는 대상 가게 식별자
 */
@RestController
@RequestMapping("/stores/{storeId}/reviews")
public class ReviewsController {

    private final ReviewsService reviewsService;

    public ReviewsController(ReviewsService reviewsService) {
        this.reviewsService = reviewsService;
    }

    /**
     * 리뷰 작성 (USER)
     * - Body: ReviewsCreateRequest (orderId, rating, content)
     * - Header: X-User-Id (필수), X-Role은 서비스에서 검증하지 않지만 보통 USER 컨텍스트에서 호출
     * - 응답: 단건 리뷰 아이템(생성 결과)
     */
    @PostMapping
    public ResponseEntity<ReviewsItemResponse> createReview(@PathVariable Long storeId,
                                                            @RequestBody ReviewsCreateRequest req,
                                                            @RequestHeader("X-User-Id") Long currentUserId) {
        // 리뷰 작성 처리
        ReviewsItemResponse response = reviewsService.create(storeId, req, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 목록 조회 (공통)
     * - 비로그인/역할 미지정: 해당 가게의 공개 리뷰 전체 조회
     * - USER: 본인이 해당 가게에 작성한 리뷰만 조회
     * - OWNER: 금지(아래 /owner 엔드포인트 사용)
     * - Query: minRating, maxRating, page, size
     * - 응답: 페이지네이션된 리뷰 목록
     */
    @GetMapping
    public ResponseEntity<Page<ReviewsItemResponse>> listReviews(
            @PathVariable Long storeId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,  // X-User-Id는 선택적
            @RequestHeader(value = "X-Role", required = false) String role) {  // X-Role도 선택적

        // 비로그인/역할 미지정 → 가게의 공개 리뷰
        if (userId == null || role == null) {
            // 일반적으로 가게에 등록된 모든 리뷰 조회
            Page<ReviewsItemResponse> response = reviewsService.getReviews(null, null, storeId, minRating, maxRating, page, size);
            return ResponseEntity.ok(response);
        }
        // USER → 본인 리뷰만
        else if ("USER".equals(role)) {
            Page<ReviewsItemResponse> response = reviewsService.getReviews(userId, role, storeId, minRating, maxRating, page, size);
            return ResponseEntity.ok(response);
        }
        // OWNER 등 기타 권한은 별도 엔드포인트 사용하도록 제한
        else {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
    }

    /**
     * 오너 전용 리뷰 목록 조회
     * - Header: X-User-Id(OWNER의 id), X-Role=OWNER
     * - OWNER가 소유한 가게의 리뷰만 접근 허용(서비스에서 소유 검증)
     */
    @GetMapping("/owner")
    public ResponseEntity<Page<ReviewsItemResponse>> listOwnerReviews(
            @PathVariable Long storeId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role") String role
    ) {
        if (!"OWNER".equals(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "오너 권한이 필요합니다.");
        }

        Page<ReviewsItemResponse> reviews = reviewsService.getReviewsByOwner(userId, storeId, minRating, maxRating, page, size);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 수정 (USER)
     * - Body: ReviewsUpdateRequest (rating, content)
     * - Header: X-User-Id(본인), X-Role=USER
     * - 정책: 본인 리뷰 + 작성 후 24시간 이내 + 미삭제 상태만
     * - 응답: 수정 전/후 비교 전용 DTO (ReviewsUpdateResponse)
     */
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewsUpdateResponse> updateReview(@PathVariable Long storeId,
                                                              @PathVariable Long reviewId,
                                                              @RequestBody ReviewsUpdateRequest req,
                                                              @RequestHeader("X-User-Id") Long userId,
                                                              @RequestHeader("X-Role") String role) {
        ReviewsUpdateResponse updated = reviewsService.update(storeId, reviewId, req, userId, role);
        return ResponseEntity.ok(updated);
    }

    /**
     * 리뷰 삭제 (USER; 하드 삭제)
     * - Header: X-User-Id(본인), X-Role=USER
     * - 정책: 본인 리뷰 + 작성 후 24시간 이내
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Long storeId,
                                                            @PathVariable Long reviewId,
                                                            @RequestHeader("X-User-Id") Long userId,
                                                            @RequestHeader("X-Role") String role) {
        return reviewsService.delete(reviewId, userId, role, storeId); // 서비스에서 삭제 처리
    }

    /**
     * 리뷰 삭제 (OWNER; 소프트 삭제)
     * - Header: X-User-Id(오너), X-Role=OWNER
     * - 정책: 본인 소유 가게의 리뷰만 소프트 삭제(복구 가능)
     */
    @DeleteMapping("/owner/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReviewByOwner(@PathVariable Long reviewId,
                                                                   @RequestHeader("X-User-Id") Long userId,
                                                                   @RequestHeader("X-Role") String role) {
        return reviewsService.deleteByOwner(reviewId, userId, role);
    }

    /**
     * 리뷰 복구 (OWNER)
     * - Header: X-User-Id(오너), X-Role=OWNER
     * - 정책: 소프트 삭제된 리뷰 + 삭제 시각으로부터 1년 이내
     */
    @PatchMapping("/owner/{reviewId}/restore")
    public ResponseEntity<Map<String, String>> restoreReviewByOwner(@PathVariable Long reviewId,
                                                                    @RequestHeader("X-User-Id") Long userId,
                                                                    @RequestHeader("X-Role") String role) {
        return reviewsService.restoreByOwner(reviewId, userId, role);
    }
}
