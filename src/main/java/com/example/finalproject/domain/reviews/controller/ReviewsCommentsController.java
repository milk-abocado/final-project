package com.example.finalproject.domain.reviews.controller;

import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsCommentsResponse;
import com.example.finalproject.domain.reviews.service.ReviewsCommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ReviewsCommentsController
 * -------------------------------------------------
 * - 리뷰에 대한 사장님 댓글(답글) CRUD API 제공
 * - 경로: /stores/{storeId}/reviews/{reviewId}/comments
 *   → 특정 가게(storeId), 특정 리뷰(reviewId)에 매핑되는 사장님 댓글을 관리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storeId}/reviews/{reviewId}/comments")
public class ReviewsCommentsController {

    private final ReviewsCommentsService reviewsCommentsService;

    /**
     * [POST] 리뷰에 대한 사장님 댓글 작성
     *
     * @param storeId        가게 ID
     * @param reviewId       리뷰 ID
     * @param currentUserId  현재 로그인된 유저 ID (헤더: X-User-Id)
     * @param role           현재 로그인된 유저 역할 (헤더: X-Role, e.g., OWNER)
     * @param req            댓글 생성 요청 DTO (content 포함)
     * @return ReviewsCommentsResponse (생성된 댓글 정보 반환)
     */
    @PostMapping
    public ResponseEntity<ReviewsCommentsResponse> create(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader("X-Role") String role,
            @RequestBody ReviewsCommentsCreateRequest req
    ) {
        return ResponseEntity.ok(
                reviewsCommentsService.create(currentUserId, storeId, reviewId, role, req)
        );
    }

    /**
     * [PATCH] 리뷰에 대한 사장님 댓글 수정
     *
     * @param storeId        가게 ID
     * @param reviewId       리뷰 ID
     * @param currentUserId  현재 로그인된 유저 ID (헤더)
     * @param role           현재 로그인된 유저 역할 (헤더)
     * @param req            댓글 수정 요청 DTO (content 포함)
     * @return ReviewsCommentsResponse (수정된 댓글 정보 반환)
     */
    @PatchMapping
    public ResponseEntity<ReviewsCommentsResponse> update(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader("X-Role") String role,
            @RequestBody ReviewsCommentsUpdateRequest req
    ) {
        return ResponseEntity.ok(
                reviewsCommentsService.update(currentUserId, storeId, reviewId, role, req)
        );
    }

    /**
     * [DELETE] 리뷰에 대한 사장님 댓글 삭제 (소프트 삭제 처리)
     *
     * @param storeId        가게 ID
     * @param reviewId       리뷰 ID
     * @param currentUserId  현재 로그인된 유저 ID (헤더)
     * @param role           현재 로그인된 유저 역할 (헤더)
     * @return message: "사장님 댓글이 삭제되었습니다."
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader("X-Role") String role
    ) {
        reviewsCommentsService.delete(currentUserId, storeId, reviewId, role);

        Map<String, String> response = new HashMap<>();
        response.put("message", "사장님 댓글이 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }
}
