package com.example.finalproject.domain.reviews.service;

import com.example.finalproject.domain.orders.entity.Orders;
import com.example.finalproject.domain.orders.repository.OrdersRepository;
import com.example.finalproject.domain.reviews.dto.request.ReviewsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsItemResponse;
import com.example.finalproject.domain.reviews.dto.response.ReviewsUpdateResponse;
import com.example.finalproject.domain.reviews.dto.response.ReviewsWithCommentResponse;
import com.example.finalproject.domain.reviews.entity.Reviews;
import com.example.finalproject.domain.reviews.entity.ReviewsComments;
import com.example.finalproject.domain.reviews.repository.ReviewsCommentsRepository;
import com.example.finalproject.domain.reviews.repository.ReviewsRepository;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReviewsService
 * ----------------------------------------------------------------------------
 * 리뷰 생성/조회/삭제/복구 등 리뷰 관련 도메인 로직을 담당하는 서비스 레이어
 * - 데이터 무결성 및 접근 제어(권한/소유 검증)를 수행
 * - 엔티티 <-> DTO 변환을 통해 컨트롤러의 응답 형태를 정리
 */
@Service
@RequiredArgsConstructor
public class ReviewsService {

    private final ReviewsRepository reviewsRepository;
    private final StoresRepository storesRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final ReviewsCommentsRepository reviewsCommentsRepository;

    private static final long USER_DELETE_WINDOW_HOURS = 24;      // 작성자 자가 삭제 허용 창
    private static final long OWNER_RETENTION_YEARS   = 1;        // 오너 소프트삭제 보관기간

    /**
     * 리뷰 작성
     * - 주문자 본인만 작성 가능
     * - 주문의 가게==요청 path storeId 검증
     * - 주문 상태가 COMPLETED(배달 완료)만 허용
     * - 동일 (orderId, userId) 중복 리뷰 차단
     */
    public ReviewsItemResponse create(Long storeId, ReviewsCreateRequest req, Long currentUserId) {
        // (1) 현재 사용자/가게/주문 엔티티 조회 및 존재 검증
        Users user = usersRepository.findById(currentUserId) // 현재 사용자 조회
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자 정보가 없습니다."));

        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // (2) 별점 범위 수동 검증 (Bean Validation 외 추가 방어)
        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "별점은 1~5점 사이여야 합니다.");
        }

        // 주문 조회 및 소유/가게/상태 검증
        Orders order = ordersRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

        // (3) 권한/소유/상태 검증
        // 3-1) 주문자 == 현재 사용자
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 주문에 대해서만 리뷰를 작성할 수 있습니다.");
        }

        // 3-2) 주문의 가게 == path storeId
        if (!order.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 가게의 주문이 아닙니다.");
        }

        // 3-3) 배달 완료 건만
        if (order.getStatus() != Orders.Status.COMPLETED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "배달 완료된 주문에 대해서만 리뷰를 작성할 수 있습니다.");
        }

        // (4) 동일 주문+유저 중복 리뷰 방지
        if (reviewsRepository.existsByOrderIdAndUserId(req.getOrderId(), user.getId())) {
            throw new ApiException(ErrorCode.CONFLICT, "해당 주문에 대한 리뷰는 이미 작성되었습니다.");
        }

        // (5) 저장
        Reviews review = Reviews.builder()
                .store(store)
                .user(user)
                .order(order) // 이미 조회한 order 사용
                .rating(req.getRating())
                .content(req.getContent())
                .build();

        reviewsRepository.save(review);
        return toItemResponse(review);
    }

    /**
     * 리뷰 조회 (공통)
     * - storeId는 필수 (특정 가게의 리뷰만 조회)
     * - 비로그인/권한 없음: 가게의 모든 공개 리뷰 조회
     * - USER: 해당 유저가 해당 가게에서 작성한 본인 리뷰만 조회
     * - OWNER 등 기타 역할은 허용하지 않음(별도 오너 전용 메서드 사용)
     */
    @Transactional
    public Page<ReviewsItemResponse> getReviews(Long userId, String role, Long storeId, Integer minRating, Integer maxRating, int page, int size) {
        // (1) 필터 기본값/범위 검증
        minRating = (minRating == null) ? 1 : minRating;
        maxRating = (maxRating == null) ? 5 : maxRating;

        if (minRating < 1 || minRating > 5) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "minRating은 1과 5 사이여야 합니다.");
        }
        if (maxRating < 1 || maxRating > 5) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "maxRating은 1과 5 사이여야 합니다.");
        }

        // (2) storeId 필수
        if (storeId == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "storeId는 필수 항목입니다.");
        }

        // (3) 권한에 따른 조회 행위 분기
        if (userId == null || role == null) {
            // 비로그인/역할 미지정: 공개 리뷰 (삭제되지 않은) 전체
            return reviewsRepository.findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
                    storeId, minRating, maxRating, PageRequest.of(page, size)
            ).map(this::toItemResponse);
        } else if ("USER".equals(role)) {
            // USER: 본인이 해당 가게에 남긴 리뷰만
            return reviewsRepository.findByUserIdAndStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
                    userId, storeId, minRating, maxRating, PageRequest.of(page, size)
            ).map(this::toItemResponse);
        } else {
            // OWNER 등은 별도 메서드(getReviewsByOwner) 사용
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
    }

    /**
     * 오너 전용 리뷰 조회
     * - 요청자(userId)가 소유한 가게 목록을 확인 후, 해당 storeId의 리뷰만 조회
     * - 삭제되지 않은 리뷰만, 별점 범위 필터 적용
     */
    @Transactional
    public Page<ReviewsItemResponse> getReviewsByOwner(Long userId, Long storeId, Integer minRating, Integer maxRating, int page, int size) {

        minRating = (minRating == null) ? 1 : minRating;
        maxRating = (maxRating == null) ? 5 : maxRating;

        // (1) 오너가 소유한 가게 목록 검증
        List<Long> storeIds = storesRepository.findStoreIdsByOwnerId(userId);
        if (storeIds.isEmpty()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "소유한 가게가 없습니다.");
        }
        // storeId가 소유한 가게인지 확인
        if (!storeIds.contains(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "자신의 가게가 아닌 리뷰는 조회할 수 없습니다.");
        }

        // (2) 해당 가게 리뷰 조회
        return reviewsRepository
                .findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
                        storeId, minRating, maxRating, PageRequest.of(page, size)
                ).map(this::toItemResponse);
    }

    /**
     * 특정 가게의 리뷰 목록과 해당 리뷰에 달린 사장님 댓글을 조회
     * - 리뷰는 삭제되지 않은 리뷰만 조회하고, 별점 범위(minRating, maxRating)에 맞는 리뷰만 반환
     * - 리뷰에 달린 사장님 댓글도 함께 조회하여 반환
     *
     * @param storeId    조회할 가게 ID
     * @param minRating  최소 별점 (1 ~ 5 사이)
     * @param maxRating  최대 별점 (1 ~ 5 사이)
     * @param page       페이지 번호 (0부터 시작)
     * @param size       페이지 크기
     * @return           리뷰와 댓글을 포함한 페이지 응답
     */
    public Page<ReviewsWithCommentResponse> getStoreReviewsWithComment(
            Long storeId, Integer minRating, Integer maxRating, int page, int size
    ) {
        int min = (minRating == null) ? 1 : minRating;
        int max = (maxRating == null) ? 5 : maxRating;
        if (min > max) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "별점 범위가 올바르지 않습니다.");
        }

        storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(page, size);

        Page<Reviews> reviewPage =
                reviewsRepository.findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
                        storeId, min, max, pageable
                );

        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> reviewIds = reviewPage.stream().map(Reviews::getId).toList();
        List<ReviewsComments> replies =
                reviewsCommentsRepository.findByReview_IdInAndIsDeletedFalse(reviewIds);

        Map<Long, ReviewsComments> replyMap = replies.stream()
                .collect(Collectors.toMap(rc -> rc.getReview().getId(), rc -> rc));

        List<ReviewsWithCommentResponse> items = reviewPage.getContent().stream()
                .map(r -> {
                    ReviewsComments rc = replyMap.get(r.getId());
                    ReviewsWithCommentResponse.OwnerCommentDto replyDto = (rc == null) ? null :
                            new ReviewsWithCommentResponse.OwnerCommentDto(
                                    rc.getId(),
                                    rc.getOwner().getId(),
                                    rc.getContent(),
                                    rc.getCreatedAt(),
                                    rc.getUpdatedAt()
                            );
                    return new ReviewsWithCommentResponse(
                            r.getId(),
                            r.getStore().getId(),
                            r.getUser().getId(),
                            r.getRating(),
                            r.getContent(),
                            r.getCreatedAt(),
                            replyDto
                    );
                })
                .toList();

        return new PageImpl<>(items, pageable, reviewPage.getTotalElements());
    }

    /**
     * 리뷰 수정 (작성자)
     * - 본인 리뷰만
     * - 작성 후 24시간 이내만
     * - 소프트삭제된 리뷰는 수정 불가(복구 후 수정)
     * - rating(1~5), content만 수정 허용
     */
    @Transactional
    public ReviewsUpdateResponse update(Long storeId,
                                        Long reviewId,
                                        ReviewsUpdateRequest req,
                                        Long userId,
                                        String role) {

        // 1) 권한 체크: USER만 허용
        if (!"USER".equals(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }

        // 2) 리뷰 조회
        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        // 3) 가게 일치 검증
        if (!review.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 가게의 리뷰만 수정할 수 있습니다.");
        }

        // 4) 본인 리뷰 검증
        if (!review.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "자신의 리뷰만 수정할 수 있습니다.");
        }

        // 5) 삭제 여부 검증
        if (review.isDeleted()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "삭제된 리뷰는 수정할 수 없습니다. 복구 후 다시 시도하세요.");
        }

        // 6) 시간 제한(작성 후 24h)
        LocalDateTime deadline = review.getCreatedAt().plusHours(USER_DELETE_WINDOW_HOURS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "작성 후 24시간이 지나 수정할 수 없습니다.");
        }

        // 7) 입력값 검증 (방어로직)
        Integer rating = req.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "별점은 1~5점 사이여야 합니다.");
        }
        String content = req.getContent();
        if (content == null || content.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "리뷰 내용은 필수입니다.");
        }

        // 수정 전 값 보관
        Integer oldRating = review.getRating();
        String oldContent = review.getContent();
        LocalDateTime oldUpdatedAt = review.getUpdatedAt();

        // 8) 수정 반영
        // (엔티티에 update 메서드가 없다면 setter로 반영)
        review.update(req.getRating(), req.getContent());
        reviewsRepository.save(review);

        return new ReviewsUpdateResponse(
                oldRating, oldContent, oldUpdatedAt,
                review.getRating(), review.getContent(), review.getUpdatedAt()
        );
    }

    /**
     * 리뷰 삭제 (작성자)
     * - 작성 후 24시간 이내만 하드 삭제 허용
     * - 본인 리뷰 + 해당 가게의 리뷰만
     */
    @Transactional
    public ResponseEntity<Map<String, String>> delete(Long reviewId, Long userId, String role, Long storeId) {
        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        // (1) 가게 일치 검증
        if (!review.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 가게의 리뷰만 삭제할 수 있습니다.");
        }

        // (2) 권한/소유 검증
        if (!"USER".equals(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
        if (!review.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "자신의 리뷰만 삭제할 수 있습니다.");
        }

        // (3) 시간 제한(작성 후 24h)
        LocalDateTime deadline = review.getCreatedAt().plusHours(USER_DELETE_WINDOW_HOURS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "작성 후 24시간이 지나 삭제할 수 없습니다.");
        }

        // (4) 하드 삭제 수행
        reviewsRepository.delete(review);

        Map<String, String> response = new HashMap<>();
        response.put("message", "리뷰가 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 삭제 (오너; 소프트 삭제)
     * - 오너는 본인 가게 리뷰를 소프트 삭제(isDeleted=true)하고 1년간 보관
     * - 동일 리뷰에 대해 중복 삭제 요청이면 메시지만 반환
     */
    @Transactional
    public ResponseEntity<Map<String, String>> deleteByOwner(Long reviewId, Long userId, String role) {
        if (!"OWNER".equals(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "오너 권한이 없습니다.");
        }

        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        // (1) 소유한 가게의 리뷰인지 검증
        if (!review.getStore().getOwner().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "자신의 가게 리뷰만 삭제할 수 있습니다.");
        }

        // (2) 이미 소프트 삭제된 경우
        if (review.isDeleted()) {
            Map<String, String> already = new HashMap<>();
            already.put("message", "이미 삭제된 리뷰입니다. 보관기간 내 복구 가능합니다.");
            return ResponseEntity.ok(already);
        }

        // (3) 소프트 삭제 처리
        review.softDeleteByOwner(); // isDeleted=true, deletedAt=now, deletedBy=OWNER
        reviewsRepository.save(review);

        Map<String, String> response = new HashMap<>();
        response.put("message", "사용자 리뷰를 삭제했습니다. (1년 보관, 보관기간 내 복구 가능)");
        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 복구 (오너)
     * - 소프트 삭제된 리뷰만 대상
     * - 삭제 시각 + 1년 이내에만 복구 가능
     */
    @Transactional
    public ResponseEntity<Map<String, String>> restoreByOwner(Long reviewId, Long userId, String role) {
        if (!"OWNER".equals(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "오너 권한이 없습니다.");
        }

        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        // (1) 소유한 가게의 리뷰인지 검증
        if (!review.getStore().getOwner().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "자신의 가게 리뷰만 복구할 수 있습니다.");
        }

        // (2) 현재 상태/삭제시각 검증
        if (!review.isDeleted()) {
            Map<String, String> rsp = new HashMap<>();
            rsp.put("message", "이미 활성 상태인 리뷰입니다.");
            return ResponseEntity.ok(rsp);
        }
        if (review.getDeletedAt() == null) {
            throw new ApiException(ErrorCode.CONFLICT, "삭제 시각 정보가 없어 복구할 수 없습니다.");
        }

        // (3) 보관기간 체크: deletedAt + 1년 이후에는 복구 불가
        LocalDateTime expiry = review.getDeletedAt().plusYears(OWNER_RETENTION_YEARS);
        if (LocalDateTime.now().isAfter(expiry)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "보관기간(1년)이 지나 복구할 수 없습니다.");
        }

        // (4) 복구
        review.restore();
        reviewsRepository.save(review);

        Map<String, String> response = new HashMap<>();
        response.put("message", "리뷰를 복구했습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 엔티티 -> 응답 DTO 변환
     * - 클라이언트에 필요한 필드만 노출
     */
    private ReviewsItemResponse toItemResponse(Reviews reviews) {
        return new ReviewsItemResponse(
                reviews.getId(),
                reviews.getStore().getId(),
                reviews.getUser().getId(),
                reviews.getOrder().getId(),
                reviews.getRating(),
                reviews.getContent(),
                reviews.getCreatedAt()
        );
    }
}
