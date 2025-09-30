package com.example.finalproject.domain.reviews.service;

import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsCommentsResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * ReviewsCommentsService
 * -------------------------------------------------
 * - 리뷰에 대한 사장님 댓글(답글) 도메인 로직 담당 서비스
 * - 인증/인가: Spring Security의 SecurityContext 기반
 */
@Service
@RequiredArgsConstructor
public class ReviewsCommentsService {

    private final ReviewsCommentsRepository reviewsCommentsRepository;
    private final ReviewsRepository reviewsRepository;
    private final StoresRepository storesRepository;
    private final UsersRepository usersRepository;

    /** 사장님 댓글 수정 가능 시간 (작성 후 3일) */
    private static final long EDITABLE_HOURS = 72;

    /**
     * 현재 로그인 사용자 조회
     * - SecurityContextHolder → Authentication → email 추출
     * - email 기준으로 Users 엔티티 조회
     * - 인증 없음 / DB에 사용자 없음 → UNAUTHORIZED 예외 발생
     */
    private Users getCurrentUserOrThrow() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            // 미인증 접근 차단
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new StoresApiException(StoresErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        Object principal = authentication.getPrincipal();
        String email = (principal instanceof UserDetails ud) ? ud.getUsername() : authentication.getName();
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));


        // 이메일 대소문자 무시 검색(정규화 일관성 보장)
        return usersRepository.findByEmailIgnoreCase(norm)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

    }

    /**
     * 가게 소유 검증 유틸
     * - storeId로 가게 조회
     * - 현재 로그인한 사용자(ownerId)와 가게 주인(owner.id) 일치 여부 확인
     * - 불일치 시 FORBIDDEN 예외
     *
     * @param storeId  가게 ID
     * @param ownerId  로그인한 사용자 ID
     * @return Stores 엔티티
     */
    private Stores getOwnedStoreOrThrow(Long storeId, Long ownerId) {
        Stores store = storesRepository.findById(storeId)

                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // NPE 방지: store.getOwner()가 null 이면 설계/데이터 무결성 이슈 → FORBIDDEN 대신 명확한 메시지 선택 가능

        if (!store.getOwner().getId().equals(ownerId)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "본인 소유 가게가 아닙니다.");
        }
        return store;
    }

    /**
     * 사장님 댓글 작성
     * - OWNER만 가능 (currentUser == store.owner)
     * - 리뷰-가게 일치 검증
     * - 리뷰당 답글은 1개 제한 (삭제되지 않은 댓글 존재 여부 확인)
     * - content 필수
     *
     * @param storeId  가게 ID
     * @param reviewId 리뷰 ID
     * @param req      댓글 생성 요청 DTO
     * @return 생성된 댓글 DTO
     */
    @Transactional
    public ReviewsCommentsResponse create(Long storeId, Long reviewId, ReviewsCommentsCreateRequest req) {
        // 1) 인증 + 권한
        Users owner = getCurrentUserOrThrow();
        if (!"OWNER".equals(owner.getRole().name())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "댓글 등록은 OWNER만 가능합니다.");
        }

        // 2) 가게 소유권
        Stores store = getOwnedStoreOrThrow(storeId, owner.getId());

        // 3) 리뷰 존재
        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        // 4) 리뷰-가게 일치(교차 접근 방지)
        if (!review.getStore().getId().equals(store.getId())) {

            throw new ApiException(ErrorCode.FORBIDDEN, "해당 리뷰는 본인 소유 가게의 리뷰가 아닙니다.");
        }

        // 5) 리뷰당 답글 1개 제한(soft delete 제외)
        if (reviewsCommentsRepository.existsByReview_IdAndIsDeletedFalse(reviewId)) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "해당 리뷰에는 이미 사장님 답글이 존재합니다.");
        }

        // 6) content 유효성
        if (req.getContent() == null || req.getContent().isBlank()) {

            throw new ApiException(ErrorCode.BAD_REQUEST, "내용을 입력해 주세요.");

        }

        // 7) 엔티티 생성 및 저장
        ReviewsComments reply = new ReviewsComments();
        reply.setReview(review);
        reply.setOwner(owner);
        reply.setStore(store);
        reply.setContent(req.getContent().trim());  // 앞뒤 공백 제거

        ReviewsComments saved = reviewsCommentsRepository.save(reply);
        return toResponse(saved);
    }

    /**
     * 사장님 댓글 수정
     * - OWNER만 가능 (currentUser == store.owner)
     * - 본인 가게(storeId)의 본인(ownerId)이 작성한 답글만 수정 가능
     * - 작성 후 3일(72시간) 이내만 수정 허용
     * - content 필수
     *
     * @param storeId  가게 ID
     * @param reviewId 리뷰 ID
     * @param req      댓글 수정 요청 DTO
     * @return 수정된 댓글 DTO
     */
    @Transactional
    public ReviewsCommentsResponse update(Long storeId, Long reviewId, ReviewsCommentsUpdateRequest req) {
        // 1) 인증 + 권한
        Users owner = getCurrentUserOrThrow();
        if (!"OWNER".equals(owner.getRole().name())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "댓글 수정은 OWNER만 가능합니다.");
        }

        // 2) 가게 소유권
        getOwnedStoreOrThrow(storeId, owner.getId());

        // 3) 리뷰 존재 + 소속 검증
        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        if (!review.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 리뷰는 본인 소유 가게의 리뷰가 아닙니다.");
        }

        // 4) 삭제되지 않은 내 답글 조회(리뷰 기준 1개 정책 전제)
        ReviewsComments reply = reviewsCommentsRepository

                .findByReview_IdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사장님 답글이 존재하지 않습니다."));

        // 5) 본인이 작성한 댓글인지 확인
        if (!reply.getOwner().getId().equals(owner.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        // 6) 수정 허용 시간 초과 여부
        long hours = Duration.between(reply.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hours > EDITABLE_HOURS) {

            throw new ApiException(ErrorCode.CONFLICT, "작성 후 3일이 지나 수정할 수 없습니다.");
        }

        // 7) content 유효성
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "내용을 입력해 주세요.");
        }

        // 8) 변경 적용(더티 체킹으로 flush)
        reply.setContent(req.getContent().trim());
        reply.setUpdatedAt(LocalDateTime.now());
        return toResponse(reply);
    }

    /**
     * 사장님 댓글 삭제 (소프트 삭제)
     * - OWNER만 가능 (currentUser == store.owner)
     * - 본인 가게(storeId)의 본인(ownerId)이 작성한 답글만 삭제 가능
     * - 실제 삭제가 아닌 soft delete 처리 (isDeleted=true, deletedAt=now)
     *
     * @param storeId  가게 ID
     * @param reviewId 리뷰 ID
     */
    @Transactional
    public void delete(Long storeId, Long reviewId) {
        // 1) 인증 + 권한
        Users owner = getCurrentUserOrThrow();
        if (!"OWNER".equals(owner.getRole().name())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "댓글 삭제는 OWNER만 가능합니다.");
        }

        // 2) 가게 소유권
        getOwnedStoreOrThrow(storeId, owner.getId());

        // 3) 리뷰 존재 + 소속 검증
        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        if (!review.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 리뷰는 본인 소유 가게의 리뷰가 아닙니다.");
        }

        // 4) 내 댓글 조회(soft delete 되지 않은 것)
        ReviewsComments reply = reviewsCommentsRepository
                .findByReview_IdAndOwner_IdAndIsDeletedFalse(reviewId, owner.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사장님 답글이 존재하지 않습니다."));

        // 5) 삭제 허용 시간 초과 여부
        long hours = Duration.between(reply.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hours > EDITABLE_HOURS) {
            throw new ApiException(ErrorCode.CONFLICT, "작성 후 3일이 지나 삭제할 수 없습니다.");
        }

        // 6) soft delete 플래그/시각 설정(영속성 컨텍스트에 반영)
        reply.setDeleted(true);
        reply.setDeletedAt(LocalDateTime.now());
    }

    /**
     * 엔티티 → DTO 변환
     * - ReviewsComments 엔티티를 ReviewsCommentsResponse DTO로 변환
     */
    private ReviewsCommentsResponse toResponse(ReviewsComments r) {
        return new ReviewsCommentsResponse(
                r.getId(),
                r.getReview().getId(),
                r.getStore().getId(),
                r.getOwner().getId(),
                r.getContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
