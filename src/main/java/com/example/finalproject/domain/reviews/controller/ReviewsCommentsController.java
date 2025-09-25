package com.example.finalproject.domain.reviews.controller;

import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsCommentsResponse;
import com.example.finalproject.domain.reviews.service.ReviewsCommentsService;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * ReviewsCommentsController
 * -------------------------------------------------
 * - 리뷰에 대한 사장님 댓글(답글) CRUD API
 * - 경로: /stores/{storeId}/reviews/{reviewId}/comments
 * - 인증/인가: SecurityContext 기반
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storeId}/reviews/{reviewId}/comments")
public class ReviewsCommentsController {

    private final ReviewsCommentsService reviewsCommentsService;

    /**
     * 현재 로그인 사용자의 Role 조회
     * - SecurityContext 에서 Authentication 꺼내 권한 파싱
     * - ROLE_USER / ROLE_OWNER 문자열을 UserRole enum 으로 변환
     * - 인증 없음 / 권한 없음 시 UNAUTHORIZED 예외
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
     * 사장님 댓글 작성
     * - 요청자 Role: OWNER
     * - PathVariable: storeId, reviewId
     * - RequestBody: ReviewsCommentsCreateRequest (content 필수)
     * - 성공 시: 생성된 ReviewsCommentsResponse 반환
     */
    @PostMapping
    public ResponseEntity<ReviewsCommentsResponse> create(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @RequestBody ReviewsCommentsCreateRequest req
    ) {
        if (currentRole() != UserRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 답글은 사장님만 작성할 수 있습니다.");
        }
        return ResponseEntity.ok(reviewsCommentsService.create(storeId, reviewId, req));
    }

    /**
     * 사장님 댓글 수정
     * - 요청자 Role: OWNER
     * - 작성 후 3일(72시간) 이내만 수정 가능
     * - PathVariable: storeId, reviewId
     * - RequestBody: ReviewsCommentsUpdateRequest (content 필수)
     * - 성공 시: 수정된 ReviewsCommentsResponse 반환
     */
    @PatchMapping
    public ResponseEntity<ReviewsCommentsResponse> update(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @RequestBody ReviewsCommentsUpdateRequest req
    ) {
        if (currentRole() != UserRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 답글 수정은 사장님만 가능합니다.");
        }
        return ResponseEntity.ok(reviewsCommentsService.update(storeId, reviewId, req));
    }

    /**
     * 사장님 댓글 삭제
     * - 요청자 Role: OWNER
     * - 소프트 삭제 처리 (isDeleted = true, deletedAt = now)
     * - PathVariable: storeId, reviewId
     * - 성공 시: {"message": "사장님 댓글이 삭제되었습니다."} 반환
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long storeId,
            @PathVariable Long reviewId
    ) {
        if (currentRole() != UserRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 답글 삭제는 사장님만 가능합니다.");
        }
        reviewsCommentsService.delete(storeId, reviewId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "사장님 댓글이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
}
