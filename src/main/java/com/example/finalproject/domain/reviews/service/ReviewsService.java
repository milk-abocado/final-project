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
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ReviewsService
 * ----------------------------------------------------------------------------
 * 리뷰 생성/조회/수정/삭제/복구 도메인 로직 담당
 * - USER: 본인 주문에 대해서만 리뷰 작성/수정/삭제 가능 (24시간 제약)
 * - OWNER: 본인 가게의 리뷰 조회/소프트 삭제/복구 가능 (보관기간 1년)
 * - 권한/무결성/비즈니스 규칙 검증 후 DB 처리
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
     * 현재 로그인 사용자 조회
     * - SecurityContextHolder 에서 인증 객체 꺼내 email → Users 조회
     * - 인증 없음 or DB 없음 → UNAUTHORIZED 예외
     */
    private Users getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new StoresApiException(StoresErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");


        Object principal = authentication.getPrincipal();
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = authentication.getName(); // fallback
        }
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

    }

    /**
     * 리뷰 가능한 가게인지 검증
     * - 활성(active=true) & 폐업(retiredAt=null) 상태여야 함
     * - 아니면 GONE 예외
     */
    private Stores getReviewableStoreOrThrow(Long storeId) {
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));
        boolean retired = (s.getRetiredAt() != null) || (s.getActive() != null && !s.getActive());
        if (retired) throw new StoresApiException(StoresErrorCode.GONE, "폐업한 가게에는 리뷰를 작성/수정할 수 없습니다.");
        return s;
    }

    /**
     * 리뷰 작성 (USER 전용)
     * - 본인 주문만 작성 가능
     * - 주문 상태 = COMPLETED(배달 완료)여야 함
     * - 동일 (orderId, userId) 리뷰 중복 작성 방지
     * - 별점 1~5점 검증
     */
    @Transactional
    public ReviewsItemResponse create(Long storeId, ReviewsCreateRequest req) {
        Users me = getCurrentUserOrThrow();
        Stores store = getReviewableStoreOrThrow(storeId);

        // 별점 범위
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "별점은 1~5점 사이여야 합니다.");
        }

        // 주문 검증
        Orders order = ordersRepository.findById(req.getOrderId())
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (!Objects.equals(order.getUser().getId(), me.getId())) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "본인의 주문에 대해서만 리뷰를 작성할 수 있습니다.");
        }
        if (!Objects.equals(order.getStore().getId(), storeId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "해당 가게의 주문이 아닙니다.");
        }
        if (order.getStatus() != Orders.Status.COMPLETED) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "배달 완료된 주문에 대해서만 리뷰를 작성할 수 있습니다.");
        }

        // 동일 주문+유저 중복 리뷰 방지
        if (reviewsRepository.existsByOrderIdAndUserId(order.getId(), me.getId())) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "해당 주문에 대한 리뷰는 이미 작성되었습니다.");
        }

        Reviews review = Reviews.builder()
                .store(store)
                .user(me)
                .order(order)
                .rating(req.getRating())
                .content(req.getContent())
                .build();

        reviewsRepository.save(review);
        return toItemResponse(review);
    }

    /**
     * 리뷰 조회 (공용/내 리뷰)
     * - 비로그인 → 공개 리뷰 전체 조회
     * - 로그인 USER → 본인 리뷰만 조회
     * - 별점 범위(min~max) 필터 적용
     */
    @Transactional
    public Page<ReviewsItemResponse> getPublicOrMine(Long storeId,
                                                     Integer minRating,
                                                     Integer maxRating,
                                                     Pageable pageable) {

        int min = (minRating == null) ? 1 : minRating;
        int max = (maxRating == null) ? 5 : maxRating;
        if (min < 1 || max > 5 || min > max) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "별점 범위가 올바르지 않습니다.");
        }

        // 가게 존재만 확인 (비활성/폐업도 과거 리뷰 열람 허용하려면 상태 체크 생략)
        storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(String.valueOf(auth.getPrincipal())));

        if (!loggedIn) {
            // 공개 리뷰 전체
            return reviewsRepository
                    .findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(storeId, min, max, pageable)
                    .map(this::toItemResponse);
        } else {
            // 내 리뷰만
            Users me = getCurrentUserOrThrow();
            return reviewsRepository
                    .findByUserIdAndStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(me.getId(), storeId, min, max, pageable)
                    .map(this::toItemResponse);
        }
    }

    /**
     * 오너 전용 리뷰 조회
     * - 요청자(owner)가 소유한 storeId의 리뷰만 조회 가능
     * - 삭제되지 않은 리뷰만
     * - 별점 범위 필터 적용
     */
    @Transactional
    public Page<ReviewsItemResponse> getReviewsByOwner(Long storeId,
                                                       Integer minRating,
                                                       Integer maxRating,
                                                       Pageable pageable) {
        Users me = getCurrentUserOrThrow();

        int min = (minRating == null) ? 1 : minRating;
        int max = (maxRating == null) ? 5 : maxRating;
        if (min < 1 || max > 5 || min > max) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "별점 범위가 올바르지 않습니다.");
        }

        // 소유 검증
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));
        if (!Objects.equals(store.getOwner().getId(), me.getId())) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "자신의 가게 리뷰만 조회할 수 있습니다.");
        }

        return reviewsRepository
                .findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(storeId, min, max, pageable)
                .map(this::toItemResponse);
    }

    /**
     * 특정 가게의 리뷰 목록 + 사장님 댓글 함께 조회
     * - 삭제되지 않은 리뷰만 조회
     * - 별점 범위(min~max) 적용
     * - 각 리뷰에 달린 Owner 댓글을 DTO에 포함
     */
    @Transactional
    public Page<ReviewsWithCommentResponse> getStoreReviewsWithComment(Long storeId,
                                                                       Integer minRating,
                                                                       Integer maxRating,
                                                                       Pageable pageable) {
        int min = (minRating == null) ? 1 : minRating;
        int max = (maxRating == null) ? 5 : maxRating;
        if (min < 1 || max > 5 || min > max) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "별점 범위가 올바르지 않습니다.");
        }

        storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        Page<Reviews> reviewPage =
                reviewsRepository.findByStoreIdAndIsDeletedFalseAndRatingBetweenOrderByCreatedAtDesc(
                        storeId, min, max, pageable
                );

        if (reviewPage.isEmpty()) return Page.empty(pageable);

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
     * 리뷰 수정 (USER 전용)
     * - 본인 리뷰만
     * - 작성 후 24시간 이내만 수정 가능
     * - 삭제된 리뷰 수정 불가 (복구 후 가능)
     * - rating(1~5), content만 수정
     */
    @Transactional
    public ReviewsUpdateResponse update(Long storeId, Long reviewId, ReviewsUpdateRequest req) {
        Users me = getCurrentUserOrThrow();

        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        if (!Objects.equals(review.getStore().getId(), storeId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "해당 가게의 리뷰만 수정할 수 있습니다.");
        }
        if (!Objects.equals(review.getUser().getId(), me.getId())) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "자신의 리뷰만 수정할 수 있습니다.");
        }
        if (review.isDeleted()) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "삭제된 리뷰는 수정할 수 없습니다. 복구 후 다시 시도하세요.");
        }

        // 가게가 현재 리뷰 가능한 상태인지 (정책에 따라 허용/차단)
        getReviewableStoreOrThrow(storeId);

        // 24h 제한
        LocalDateTime deadline = review.getCreatedAt().plusHours(USER_DELETE_WINDOW_HOURS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "작성 후 24시간이 지나 수정할 수 없습니다.");
        }

        Integer rating = req.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "별점은 1~5점 사이여야 합니다.");
        }
        String content = req.getContent();
        if (content == null || content.isBlank()) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "리뷰 내용은 필수입니다.");
        }

        // 이전 값 보관
        Integer oldRating = review.getRating();
        String oldContent = review.getContent();
        LocalDateTime oldUpdatedAt = review.getUpdatedAt();

        // 엔티티 메서드 있으면 사용, 없으면 setter
        review.update(rating, content);
        reviewsRepository.save(review);

        return new ReviewsUpdateResponse(
                oldRating, oldContent, oldUpdatedAt,
                review.getRating(), review.getContent(), review.getUpdatedAt()
        );
    }

    /**
     * 리뷰 삭제 (USER 전용)
     * - 본인 리뷰만
     * - 작성 후 24시간 이내만 하드 삭제 가능
     */
    @Transactional
    public ResponseEntity<Map<String, String>> deleteAsUser(Long storeId, Long reviewId) {
        Users me = getCurrentUserOrThrow();

        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        if (!Objects.equals(review.getStore().getId(), storeId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "해당 가게의 리뷰만 삭제할 수 있습니다.");
        }
        if (!Objects.equals(review.getUser().getId(), me.getId())) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "자신의 리뷰만 삭제할 수 있습니다.");
        }

        LocalDateTime deadline = review.getCreatedAt().plusHours(USER_DELETE_WINDOW_HOURS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "작성 후 24시간이 지나 삭제할 수 없습니다.");
        }

        reviewsRepository.delete(review);
        return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
    }

    /**
     * 리뷰 삭제 (OWNER 전용, 소프트 삭제)
     * - 본인 소유 가게의 리뷰만 대상
     * - 이미 삭제된 리뷰라면 안내 메시지 반환
     * - 삭제 시 isDeleted=true, deletedAt=now, deletedBy=OWNER
     */
    @Transactional
    public ResponseEntity<Map<String, String>> deleteByOwner(Long storeId, Long reviewId) {
        Users owner = getCurrentUserOrThrow();

        Reviews review = reviewsRepository
                .findByIdAndStore_IdAndStore_Owner_Id(reviewId, storeId, owner.getId())
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.FORBIDDEN, "해당 가게의 본인 소유 리뷰만 삭제할 수 있습니다."));

        if (review.isDeleted()) {
            return ResponseEntity.ok(Map.of("message", "이미 삭제된 리뷰입니다. 보관기간 내 복구 가능합니다."));
        }

        review.softDeleteByOwner();
        reviewsRepository.save(review);
        return ResponseEntity.ok(Map.of("message", "사용자 리뷰를 삭제했습니다. (1년 보관, 복구 가능)"));
    }

    /**
     * 리뷰 복구 (OWNER 전용)
     * - 소프트 삭제된 리뷰만
     * - 삭제 시각 + 1년 이내만 복구 가능
     */
    @Transactional
    public ResponseEntity<Map<String, String>> restoreByOwner(Long storeId, Long reviewId) {
        Users owner = getCurrentUserOrThrow();

        Reviews review = reviewsRepository
                .findByIdAndStore_IdAndStore_Owner_Id(reviewId, storeId, owner.getId())
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.FORBIDDEN, "해당 가게의 본인 소유 리뷰만 복구할 수 있습니다."));

        if (!review.isDeleted()) {
            return ResponseEntity.ok(Map.of("message", "이미 활성 상태인 리뷰입니다."));
        }
        if (review.getDeletedAt() == null) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "삭제 시각 정보가 없어 복구할 수 없습니다.");
        }
        LocalDateTime expiry = review.getDeletedAt().plusYears(OWNER_RETENTION_YEARS);
        if (LocalDateTime.now().isAfter(expiry)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "보관기간(1년)이 지나 복구할 수 없습니다.");
        }

        review.restore();
        reviewsRepository.save(review);
        return ResponseEntity.ok(Map.of("message", "리뷰를 복구했습니다."));
    }

    /** 엔티티 -> DTO */
    private ReviewsItemResponse toItemResponse(Reviews r) {
        return new ReviewsItemResponse(
                r.getId(),
                r.getStore().getId(),
                r.getUser().getId(),
                r.getOrder().getId(),
                r.getRating(),
                r.getContent(),
                r.getCreatedAt()
        );
    }
}
