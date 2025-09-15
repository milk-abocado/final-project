package com.example.finalproject.domain.reviews.service;

import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsCreateRequest;
import com.example.finalproject.domain.reviews.dto.request.ReviewsCommentsUpdateRequest;
import com.example.finalproject.domain.reviews.dto.response.ReviewsCommentsResponse;
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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewsCommentsService {

    private final ReviewsCommentsRepository reviewsCommentsRepository;
    private final ReviewsRepository reviewsRepository;
    private final StoresRepository storesRepository;
    private final UsersRepository usersRepository;

    private static final long EDITABLE_HOURS = 72; // 3일

    @Transactional
    public ReviewsCommentsResponse create(Long currentUserId, Long storeId, Long reviewId, String role, ReviewsCommentsCreateRequest req) {
        // USER 차단
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 답글은 사장님만 작성할 수 있습니다.");
        }

        Users owner = usersRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "인증 정보가 올바르지 않습니다."));

        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // 소유 검증
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "다른 가게의 리뷰에는 답글을 달 수 없습니다.");
        }

        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

        // 리뷰-가게 일치 검증
        if (!review.getStore().getId().equals(store.getId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "리뷰와 가게 정보가 일치하지 않습니다.");
        }

        // 리뷰당 답글 1개 제약
        if (reviewsCommentsRepository.existsByReview_IdAndIsDeletedFalse(reviewId)) {
            throw new ApiException(ErrorCode.CONFLICT, "해당 리뷰에는 이미 사장님 답글이 존재합니다.");
        }

        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "내용을 입력해주세요.");
        }

        ReviewsComments reply = new ReviewsComments();
        reply.setReview(review);
        reply.setOwner(owner);
        reply.setStore(store);
        reply.setContent(req.getContent().trim());

        ReviewsComments saved = reviewsCommentsRepository.save(reply);
        return toResponse(saved);
    }

    @Transactional
    public ReviewsCommentsResponse update(Long currentUserId, Long storeId, Long reviewId, String role, ReviewsCommentsUpdateRequest req) {
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 답글 수정은 사장님만 가능합니다.");
        }

        ReviewsComments reply = reviewsCommentsRepository.findByReview_IdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사장님 답글이 존재하지 않습니다."));

        // 소유 검증
        if (!reply.getOwner().getId().equals(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "다른 사장님의 답글은 수정할 수 없습니다.");
        }
        if (!reply.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "가게 정보가 일치하지 않습니다.");
        }

        // 수정 가능 시간(3일) 체크
        LocalDateTime created = reply.getCreatedAt();
        long hours = Duration.between(created, LocalDateTime.now()).toHours();
        if (hours > EDITABLE_HOURS) {
            throw new ApiException(ErrorCode.FORBIDDEN, "작성 후 3일이 지나 수정할 수 없습니다.");
        }

        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "내용을 입력해주세요.");
        }

        reply.setContent(req.getContent().trim());
        reply.setUpdatedAt(LocalDateTime.now());
        return toResponse(reply);
    }

    @Transactional
    public void delete(Long currentUserId, Long storeId, Long reviewId, String role) {
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "리뷰 답글 삭제는 사장님만 가능합니다.");
        }

        ReviewsComments reply = reviewsCommentsRepository.findByReview_IdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사장님 답글이 존재하지 않습니다."));

        // 소유 검증
        if (!reply.getOwner().getId().equals(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "다른 사장님의 답글은 삭제할 수 없습니다.");
        }
        if (!reply.getStore().getId().equals(storeId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "가게 정보가 일치하지 않습니다.");
        }

        reply.setDeletedAt(LocalDateTime.now());
        reply.setDeleted(true);
    }

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
