package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.request.StoreNoticeRequest;
import com.example.finalproject.domain.stores.dto.response.StoreNoticeResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.entity.StoresNotice;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.repository.StoreNoticeRepository;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreNoticeService {

    private final StoresRepository storesRepository;
    private final StoreNoticeRepository storeNoticeRepository;

    /** 가게 단위 만료 공지 정리 */
    private void purgeExpired(Long storeId) {
        storeNoticeRepository.deleteExpiredByStore(storeId, LocalDateTime.now());
    }

    /**
     * 가게 공지 등록
     * - 한 가게당 공지 1개 정책: 기존 공지가 남아있으면 409
     * - 등록 전 해당 가게의 만료 공지 삭제
     * - 동시성(경합)으로 인한 PK 충돌은 409로 매핑
     */
    @Transactional
    public StoreNoticeResponse create(Long storeId, StoreNoticeRequest req) {
        purgeExpired(storeId);

        // 가게 존재 검증
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // 기간/내용 검증
        validateWindow(req.getStartsAt(), req.getEndsAt(), req.getMinDurationHours(), req.getMaxDurationDays());

        // 본문 공백 방지
        if (!StringUtils.hasText(req.getContent())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 내용은 비어 있을 수 없습니다.");
        }

        // 공지 단건 정책: 가게 기준으로 존재 여부 확인
        if (storeNoticeRepository.existsById(storeId)) {
            throw new ApiException(ErrorCode.CONFLICT, "해당 가게에는 이미 공지가 존재합니다.");
        }

        try {
            StoresNotice saved = storeNoticeRepository.save(
                    StoresNotice.builder()
                            .store(store) // @MapsId(id=storeId) 또는 Unique(store_id) 정책 모두 호환
                            .content(req.getContent().trim())
                            .startsAt(req.getStartsAt())
                            .endsAt(req.getEndsAt())
                            .minDurationHours(req.getMinDurationHours())
                            .maxDurationDays(req.getMaxDurationDays())
                            .build()
            );
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인해 Unique/PK 충돌 시 409로 매핑
            throw new ApiException(ErrorCode.CONFLICT, "동일 시점에 공지가 이미 등록되었습니다.");
        }
    }

    /**
     * 가게 공지 목록 조회 (정책상 0 또는 1개)
     * - onlyActive=true: 현재 시각 기준 활성 공지만
     * - onlyActive=false: 존재시 1건 반환
     */
    @Transactional(readOnly = true)
    public List<StoreNoticeResponse> findAll(Long storeId, boolean onlyActive) {
        // 가게 존재 여부 선검사 (조회 전 404)
        if (!storesRepository.existsById(storeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다.");
        }

        if (onlyActive) {
            LocalDateTime now = LocalDateTime.now();
            return storeNoticeRepository.findActiveOneByStoreId(storeId, now)
                    .map(this::toResponse)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            // 정책상 1건이므로 storeId 기준 단건 조회
            return storeNoticeRepository.findById(storeId)
                    .map(this::toResponse)
                    .map(List::of)
                    .orElse(List.of());
        }
    }

    /** 가게 공지 수정 */
    @Transactional
    public StoreNoticeResponse update(Long storeId, StoreNoticeRequest req) {
        // 가게 존재 여부 선검사
        if (!storesRepository.existsById(storeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다.");
        }

        // 만료 정리 후 수정
        purgeExpired(storeId);

        // 공지 식별도 storeId 기준
        StoresNotice n = storeNoticeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));

        validateWindow(req.getStartsAt(), req.getEndsAt(), req.getMinDurationHours(), req.getMaxDurationDays());

        if (!StringUtils.hasText(req.getContent())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 내용은 비어 있을 수 없습니다.");
        }

        n.setContent(req.getContent().trim());
        n.setStartsAt(req.getStartsAt());
        n.setEndsAt(req.getEndsAt());
        n.setMinDurationHours(req.getMinDurationHours());
        n.setMaxDurationDays(req.getMaxDurationDays());

        return toResponse(storeNoticeRepository.save(n));
    }

    /** 가게 공지 삭제 */
    @Transactional
    public void delete(Long storeId) {
        // 가게 존재 여부 선검사
        if (!storesRepository.existsById(storeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다.");
        }

        purgeExpired(storeId);

        StoresNotice n = storeNoticeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
        storeNoticeRepository.delete(n);
    }

    /**
     * 시간 검증:
     *  - 종료 > 시작
     *  - 최소/최대 유지기간 충족
     *  - 최소시간 비교는 분 단위로 엄밀히(시 단위 절삭으로 인한 경계 오류 방지)
     */
    private void validateWindow(LocalDateTime startsAt, LocalDateTime endsAt,
                                Integer minHours, Integer maxDays) {
        if (startsAt == null || endsAt == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공지 시작/종료 시각은 필수입니다.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "종료 시각은 시작 시각 이후여야 합니다.");
        }

        // 파라미터 범위 체크
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

    /** 엔티티 → DTO 변환 (now 기준 활성 여부 포함형: [startsAt, endsAt]) */
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
