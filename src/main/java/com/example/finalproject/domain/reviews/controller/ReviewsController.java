package com.example.finalproject.domain.reviews.controller;

import com.example.finalproject.domain.reviews.dto.request.ReviewsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsItemResponse;
import com.example.finalproject.domain.reviews.dto.response.ReviewsUpdateResponse;
import com.example.finalproject.domain.reviews.dto.response.ReviewsWithCommentResponse;
import com.example.finalproject.domain.reviews.service.ReviewsService;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.users.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * ReviewsController
 * ----------------------------------------------------------------------------
 * - 특정 가게(storeId)의 리뷰 CRUD 및 조회 API 제공
 * - 사용자(USER)와 오너(OWNER) 권한에 따라 접근 가능한 기능 분리
 * - 인증 정보는 SecurityContextHolder 에서 조회하여 UserRole 매핑
 */
@RestController
@RequestMapping("/stores/{storeId}/reviews")
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewsService reviewsService;

    /**
     * 현재 로그인한 사용자의 권한(Role) 확인
     * - SecurityContextHolder 에서 Authentication 조회
     * - ROLE_USER, ROLE_OWNER → UserRole.USER, UserRole.OWNER 변환
     */
    private UserRole currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roleString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
        if (roleString == null) throw new ApiException(ErrorCode.UNAUTHORIZED, "권한이 없습니다.");
        return UserRole.valueOf(roleString.replace("ROLE_", ""));
    }

    /**
     * 리뷰 작성 (USER 전용)
     * - 경로: POST /stores/{storeId}/reviews
     * - body: ReviewsCreateRequest
     * - USER만 가능, OWNER는 차단
     */
    @PostMapping
    public ResponseEntity<ReviewsItemResponse> createReview(@PathVariable Long storeId,
                                                            @RequestBody @Valid ReviewsCreateRequest req) {
        // 컨트롤러에서는 상위 역할만 간단히 걸러주고, 상세 검증은 서비스에서 처리
        UserRole role = currentRole();
        if (role == UserRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 작성은 USER만 가능합니다.");
        }
        ReviewsItemResponse response = reviewsService.create(storeId, req);
        URI location = URI.create("/stores/" + storeId + "/reviews/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * 리뷰 목록 조회 (공용)
     * - 경로: GET /stores/{storeId}/reviews
     * - 비로그인: 공개 리뷰 전체 조회
     * - 로그인 USER: 본인 리뷰만 조회
     */
    @GetMapping
    public ResponseEntity<Page<ReviewsItemResponse>> listReviews(@PathVariable Long storeId,
                                                                 @RequestParam(required = false) Integer minRating,
                                                                 @RequestParam(required = false) Integer maxRating,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        Page<ReviewsItemResponse> response =
                reviewsService.getPublicOrMine(storeId, minRating, maxRating, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    /**
     * 오너 전용 리뷰 목록 조회
     * - 경로: GET /stores/{storeId}/reviews/owner
     * - OWNER만 접근 가능
     * - 본인 소유 가게의 리뷰만 반환
     */
    @GetMapping("/owner")
    public ResponseEntity<Page<ReviewsItemResponse>> listOwnerReviews(@PathVariable Long storeId,
                                                                      @RequestParam(required = false) Integer minRating,
                                                                      @RequestParam(required = false) Integer maxRating,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        if (currentRole() != UserRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 리뷰 목록 조회는 OWNER만 가능합니다.");
        }
        Page<ReviewsItemResponse> reviews =
                reviewsService.getReviewsByOwner(storeId, minRating, maxRating, PageRequest.of(page, size));
        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 + 사장님 댓글 동시 조회
     * - 경로: GET /stores/{storeId}/reviews/with-comment
     * - 별점 범위(minRating~maxRating) 필터링 가능
     * - 공개 API (권한 제한 없음)
     */
    @GetMapping("/with-comment")
    public ResponseEntity<Page<ReviewsWithCommentResponse>> listWithReply(@PathVariable Long storeId,
                                                                          @RequestParam(required = false) Integer minRating,
                                                                          @RequestParam(required = false) Integer maxRating,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                reviewsService.getStoreReviewsWithComment(storeId, minRating, maxRating, PageRequest.of(page, size))
        );
    }

    /**
     * 리뷰 수정 (USER 전용)
     * - 경로: PATCH /stores/{storeId}/reviews/{reviewId}
     * - 본인 리뷰 + 작성 24시간 이내만 수정 가능
     */
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewsUpdateResponse> updateReview(@PathVariable Long storeId,
                                                              @PathVariable Long reviewId,
                                                              @RequestBody @Valid ReviewsUpdateRequest req) {
        if (currentRole() != UserRole.USER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
        return ResponseEntity.ok(reviewsService.update(storeId, reviewId, req));
    }

    /**
     * 리뷰 삭제 (USER 전용)
     * - 경로: DELETE /stores/{storeId}/reviews/{reviewId}
     * - 본인 리뷰 + 작성 24시간 이내만 하드 삭제
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Long storeId,
                                                            @PathVariable Long reviewId) {
        if (currentRole() != UserRole.USER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
        return reviewsService.deleteAsUser(storeId, reviewId);
    }

    /**
     * 리뷰 삭제 (OWNER 전용, 소프트 삭제)
     * - 경로: DELETE /stores/{storeId}/reviews/owner/{reviewId}
     * - 본인 소유 가게의 리뷰만 softDelete
     * - 1년간 보관 후 완전 삭제
     */
    @DeleteMapping("/owner/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteByOwner(
            @PathVariable Long storeId,
            @PathVariable Long reviewId
    ) {
        return reviewsService.deleteByOwner(storeId, reviewId);
    }

    /**
     * 리뷰 복구 (OWNER 전용)
     * - 경로: PATCH /stores/{storeId}/reviews/owner/{reviewId}/restore
     * - 본인 소유 가게의 softDeleted 리뷰만
     * - 삭제 후 1년 이내 복구 가능
     */
    @PatchMapping("/owner/{reviewId}/restore")
    public ResponseEntity<Map<String, String>> restoreByOwner(
            @PathVariable Long storeId,
            @PathVariable Long reviewId
    ) {
        return reviewsService.restoreByOwner(storeId, reviewId);
    }
}
