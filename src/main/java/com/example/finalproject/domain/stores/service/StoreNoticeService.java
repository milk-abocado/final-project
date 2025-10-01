package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.dto.request.StoreNoticeRequest;
import com.example.finalproject.domain.stores.dto.response.StoreNoticeResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.entity.StoresNotice;
import com.example.finalproject.domain.stores.exception.StoresApiException;
import com.example.finalproject.domain.stores.exception.StoresErrorCode;
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

/**
 * 가게 공지(StoresNotice) 도메인 서비스
 * - 오너 권한 검증, 공지 생성/조회/수정/삭제, 유효성 검증, 만료 공지 정리 담당
 * ---------------------------------------------------
 * 예외 정책
 *  - 401: 인증 없음
 *  - 403: 오너 아님(권한 은닉 정책을 쓰지 않는 경우)
 *  - 404: 존재하지 않는 가게/공지 (또는 권한 은닉 시 타인 가게 접근)
 *  - 400: 요청값 유효성 위반(시간/내용)
 *  - 409: 가게당 공지 중복(가게당 1개 정책)
 */
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
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // Lazy loading 초기화
        Users owner = store.getOwner();
        Hibernate.initialize(owner);

        // 이메일 비교하여 소유자 확인
        return owner.getEmail().equals(email);
    }

    /**
     * 만료된 공지 삭제(해당 가게 한정)
     * - 서비스 진입 시 선제적으로 만료 공지를 정리하여 쿼리/표시 일관성 확보
     */
    private void purgeExpired(Long storeId) {
        storeNoticeRepository.deleteExpiredByStore(storeId, LocalDateTime.now());
    }

    /**
     * 현재 인증 사용자 이메일을 가져오고, 인증이 없으면 401 발생
     */
    private String currentEmailOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new StoresApiException(StoresErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return auth.getName();
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
        // (1) 인증 가드
        String currentEmail = currentEmailOrThrow();

        // (2) 오너 검증(권한 은닉을 쓰려면 isOwner 내부에서 404를 던지도록 변경 가능)
        if (!isOwner(storeId, currentEmail)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "공지 등록은 해당 가게의 OWNER만 가능합니다.");
        }

        // (3) 만료 공지 정리(동시성/정합성 ↑)
        purgeExpired(storeId);

        // (4) 가게 존재 확인(위 isOwner 에서 이미 조회했지만, 안전상 재확인)
        Stores store = storesRepository.findById(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다."));

        // (5) 시간/내용 유효성 검증(400)
        validateWindow(req.getStartsAt(), req.getEndsAt(), req.getMinDurationHours(), req.getMaxDurationDays());
        if (!StringUtils.hasText(req.getContent())) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "공지 내용은 비어 있을 수 없습니다.");
        }

        // (6) 가게당 1개 정책: 중복 방지(409) — DB UNIQUE 제약(store_id unique) 병행 권장
        if (storeNoticeRepository.existsByStore_Id(storeId)) {
            throw new StoresApiException(StoresErrorCode.CONFLICT, "해당 가게에는 이미 공지가 존재합니다.");
        }

        // (7) 저장
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
            // DB UNIQUE 제약 위반 등 동시성에서 발생 시
            throw new StoresApiException(StoresErrorCode.CONFLICT, "동일 시점에 공지가 이미 등록되었습니다.");
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
        // (1) 가게 존재 검사(404) — 권한과 무관한 공개 조회라면 이 수준이면 충분
        if (!storesRepository.existsById(storeId)) {
            throw new StoresApiException(StoresErrorCode.NOT_FOUND, "가게를 찾을 수 없습니다.");
        }

        // (2) activeOnly 분기
        if (onlyActive) {
            // 현재 활성 공지 조회
            LocalDateTime now = LocalDateTime.now();
            return storeNoticeRepository.findActiveOneByStoreId(storeId, now)
                    .map(this::toResponse)  // 응답 객체로 변환
                    .map(List::of)          // 목록으로 변환
                    .orElse(List.of());     // 없으면 빈 리스트 반환
        } else {
            // 전체 공지 조회
            return storeNoticeRepository.findByStore_Id(storeId)
                    .map(this::toResponse).map(List::of).orElse(List.of());
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
        // (1) 인증 가드
        String currentEmail = currentEmailOrThrow();

        // (2) 오너 검증
        if (!isOwner(storeId, currentEmail)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "공지 수정은 해당 가게의 OWNER만 가능합니다.");
        }

        // (3) 대상 공지 조회(가게당 1개 정책 전제)
        StoresNotice notice = storeNoticeRepository.findByStore_Id(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));

        // (4) 유효성 검증(400)
        validateWindow(req.getStartsAt(), req.getEndsAt(), req.getMinDurationHours(), req.getMaxDurationDays());
        // 공지 내용 검증
        if (!StringUtils.hasText(req.getContent())) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "공지 내용은 비어 있을 수 없습니다.");
        }

        // (5) 수정 후 저장
        notice.setContent(req.getContent().trim());
        notice.setStartsAt(req.getStartsAt());
        notice.setEndsAt(req.getEndsAt());
        notice.setMinDurationHours(req.getMinDurationHours());
        notice.setMaxDurationDays(req.getMaxDurationDays());

        // 수정된 공지 반환
        return toResponse(storeNoticeRepository.save(notice));
    }

    /**
     * 가게 공지 삭제
     * @param storeId 가게 ID
     */
    @Transactional
    public void delete(Long storeId) {
        // (1) 인증 가드
        String currentEmail = currentEmailOrThrow();

        // (2) 오너 검증
        if (!isOwner(storeId, currentEmail)) {
            throw new StoresApiException(StoresErrorCode.FORBIDDEN, "공지 삭제는 해당 가게의 OWNER만 가능합니다.");
        }

        // (3) 대상 공지 조회(없으면 404)
        StoresNotice notice = storeNoticeRepository.findByStore_Id(storeId)
                .orElseThrow(() -> new StoresApiException(StoresErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));

        // (4) 삭제
        storeNoticeRepository.delete(notice);
    }

    /**
     * 공지 기간 검증
     *  - 종료 시각 > 시작 시각
     *  - 최소/최대 유지 기간 충족 여부
     */
    private void validateWindow(LocalDateTime startsAt, LocalDateTime endsAt,
                                Integer minHours, Integer maxDays) {
        if (startsAt == null || endsAt == null) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "공지 시작/종료 시각은 필수입니다.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "종료 시각은 시작 시각 이후여야 합니다.");
        }

        // 최소/최대 시간 검증
        if (minHours != null && minHours < 0) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "최소 유지 시간은 0시간 이상이어야 합니다.");
        }
        if (maxDays != null && maxDays < 1) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "최대 유지 일수는 1일 이상이어야 합니다.");
        }

        long hours = Duration.between(startsAt, endsAt).toHours();
        if (minHours != null && hours < minHours) {
            throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "공지 유지 시간이 최소 시간보다 짧습니다.");
        }

        // 경계 정확도 향상: endsAt이 (startsAt + maxDays) '초과'하면 위반
        if (maxDays != null) {
            LocalDateTime maxAllowedEnd = startsAt.plusDays(maxDays);
            if (endsAt.isAfter(maxAllowedEnd)) {
                throw new StoresApiException(StoresErrorCode.BAD_REQUEST, "공지 유지 기간이 최대 일수를 초과했습니다.");
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