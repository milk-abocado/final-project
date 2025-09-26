package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.request.StoreNoticeRequest;
import com.example.finalproject.domain.stores.dto.response.StoreNoticeResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.entity.StoresNotice;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoreNoticeRepository;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import com.example.finalproject.domain.users.entity.Users;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreNoticeService {

    private final StoresRepository storesRepository;
    private final StoreNoticeRepository storeNoticeRepository;

    /**
     * 가게 소유자 확인 메소드
     * @param storeId 가게 ID
     * @param email 소유자의 이메일
     * @return 가게의 소유자인지 확인
     */
    @Transactional
    public boolean isOwner(Long storeId, String email) {
        // 가게 조회
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // Lazy loading 초기화
        Users owner = store.getOwner();
        Hibernate.initialize(owner);

        // 이메일 비교하여 소유자 확인
        return owner.getEmail().equals(email);
    }

    /**
     * 가게 공지 만료된 것 정리
     * - 만료된 공지를 삭제하는 메소드
     * @param storeId 가게 ID
     */
    private void purgeExpired(Long storeId) {
        storeNoticeRepository.deleteExpiredByStore(storeId, LocalDateTime.now());
    }

    /**
     * 가게 공지 등록
     * - 공지 내용을 등록하고, 유효성 체크 후 저장
     * @param storeId 가게 ID
     * @param req 공지 등록 요청 정보
     * @return 저장된 공지 정보
     */
    @Transactional
    public StoreNoticeResponse create(Long storeId, StoreNoticeRequest req) {
        // 현재 인증된 사용자 이메일
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 소유자 확인
        if (!isOwner(storeId, currentEmail)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "공지 등록은 OWNER만 가능합니다.");
        }

        // 만료된 공지 정리
        purgeExpired(storeId);

        // 가게 존재 여부 확인
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // 공지 시간 검증
        validateWindow(req.getStartsAt(), req.getEndsAt(), req.getMinDurationHours(), req.getMaxDurationDays());

        // 공지 내용 검증
        if (!StringUtils.hasText(req.getContent())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 내용은 비어 있을 수 없습니다.");
        }

        // 동일한 시점에 이미 공지가 존재하는지 확인
        if (storeNoticeRepository.existsById(storeId)) {
            throw new ApiException(ErrorCode.CONFLICT, "해당 가게에는 이미 공지가 존재합니다.");
        }

        try {
            // 공지 저장
            StoresNotice saved = storeNoticeRepository.save(
                    StoresNotice.builder()
                            .store(store)
                            .content(req.getContent().trim())
                            .startsAt(req.getStartsAt())
                            .endsAt(req.getEndsAt())
                            .minDurationHours(req.getMinDurationHours())
                            .maxDurationDays(req.getMaxDurationDays())
                            .build()
            );
            return toResponse(saved);   // 공지 응답 반환
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.CONFLICT, "동일 시점에 공지가 이미 등록되었습니다.");
        }
    }

    /**
     * 가게 공지 목록 조회
     * - 활성 공지 목록 또는 전체 공지 목록을 조회
     * @param storeId 가게 ID
     * @param onlyActive 활성 공지 여부
     * @return 공지 목록
     */
    @Transactional
    public List<StoreNoticeResponse> findAll(Long storeId, boolean onlyActive) {
        // 가게 존재 여부 확인
        if (!storesRepository.existsById(storeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다.");
        }

        if (onlyActive) {
            // 현재 활성 공지 조회
            LocalDateTime now = LocalDateTime.now();
            return storeNoticeRepository.findActiveOneByStoreId(storeId, now)
                    .map(this::toResponse)  // 응답 객체로 변환
                    .map(List::of)          // 목록으로 변환
                    .orElse(List.of());     // 없으면 빈 리스트 반환
        } else {
            // 전체 공지 조회
            return storeNoticeRepository.findById(storeId)
                    .map(this::toResponse)
                    .map(List::of)
                    .orElse(List.of());
        }
    }

    /**
     * 가게 공지 수정
     * - 기존 공지의 내용을 수정
     * @param storeId 가게 ID
     * @param req 수정할 공지 정보
     * @return 수정된 공지 정보
     */
    @Transactional
    public StoreNoticeResponse update(Long storeId, StoreNoticeRequest req) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 소유자 확인
        if (!isOwner(storeId, currentEmail)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "오너만 공지를 수정할 수 있습니다.");
        }

        // 공지 조회
        StoresNotice storeNotice = storeNoticeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));

        // 공지 시간 검증
        validateWindow(req.getStartsAt(), req.getEndsAt(), req.getMinDurationHours(), req.getMaxDurationDays());

        // 공지 내용 검증
        if (!StringUtils.hasText(req.getContent())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 내용은 비어 있을 수 없습니다.");
        }

        // 공지 수정
        storeNotice.setContent(req.getContent().trim());
        storeNotice.setStartsAt(req.getStartsAt());
        storeNotice.setEndsAt(req.getEndsAt());
        storeNotice.setMinDurationHours(req.getMinDurationHours());
        storeNotice.setMaxDurationDays(req.getMaxDurationDays());

        // 수정된 공지 반환
        return toResponse(storeNoticeRepository.save(storeNotice));
    }

    /**
     * 가게 공지 삭제
     * @param storeId 가게 ID
     */
    @Transactional
    public void delete(Long storeId) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 소유자 확인
        if (!isOwner(storeId, currentEmail)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "오너만 공지를 삭제할 수 있습니다.");
        }

        // 공지 조회 후 삭제
        StoresNotice storeNotice = storeNoticeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));

        storeNoticeRepository.delete(storeNotice);
    }

    /**
     * 공지 기간 검증
     *  - 종료 시각 > 시작 시각
     *  - 최소/최대 유지 기간 충족 여부
     */
    private void validateWindow(LocalDateTime startsAt, LocalDateTime endsAt,
                                Integer minHours, Integer maxDays) {
        if (startsAt == null || endsAt == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 시작/종료 시각은 필수입니다.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "종료 시각은 시작 시각 이후여야 합니다.");
        }

        // 최소/최대 시간 검증
        if (minHours != null && minHours < 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "최소 유지 시간은 0시간 이상이어야 합니다.");
        }
        if (maxDays != null && maxDays < 1) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "최대 유지 일수는 1일 이상이어야 합니다.");
        }

        long hours = Duration.between(startsAt, endsAt).toHours();
        if (minHours != null && hours < minHours) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 유지 시간이 최소 시간보다 짧습니다.");
        }

        // 경계 정확도 향상: endsAt이 (startsAt + maxDays) '초과'하면 위반
        if (maxDays != null) {
            LocalDateTime maxAllowedEnd = startsAt.plusDays(maxDays);
            if (endsAt.isAfter(maxAllowedEnd)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "공지 유지 기간이 최대 일수를 초과했습니다.");
            }
        }
    }

    /**
     * StoresNotice 엔티티를 StoreNoticeResponse DTO로 변환
     * @param n StoresNotice 엔티티
     * @return 변환된 StoreNoticeResponse DTO
     */
    private StoreNoticeResponse toResponse(StoresNotice n) {
        LocalDateTime now = LocalDateTime.now();
        boolean active = !n.getStartsAt().isAfter(now) && !n.getEndsAt().isBefore(now);

        return StoreNoticeResponse.builder()
                .id(n.getId())
                .storeId(n.getStore().getId())
                .content(n.getContent())
                .startsAt(n.getStartsAt())
                .endsAt(n.getEndsAt())
                .minDurationHours(n.getMinDurationHours())
                .maxDurationDays(n.getMaxDurationDays())
                .active(active)
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}
