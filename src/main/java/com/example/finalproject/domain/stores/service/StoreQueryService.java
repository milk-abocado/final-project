package com.example.finalproject.domain.stores.service;

import com.example.finalproject.domain.stores.auth.SecurityUtil;
import com.example.finalproject.domain.stores.dto.response.MenuSummaryResponse;
import com.example.finalproject.domain.stores.dto.response.StoreDetailResponse;
import com.example.finalproject.domain.stores.dto.response.StoreListItemResponse;
import com.example.finalproject.domain.stores.entity.Stores;
import com.example.finalproject.domain.stores.exception.ApiException;
import com.example.finalproject.domain.stores.exception.ErrorCode;
import com.example.finalproject.domain.stores.geo.GeocodingPort;
import com.example.finalproject.domain.stores.geo.LatLng;
import com.example.finalproject.domain.stores.menu.MenuReader;
import com.example.finalproject.domain.stores.repository.StoresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * StoreQueryService
 * -------------------------------------------------
 * - 가게 검색/조회 전용 서비스
 * - 지오코딩(주소→좌표), 반경 검색, 키워드 검색, 메뉴 로딩을 조합해 응답 DTO 구성
 */
@Service
@RequiredArgsConstructor
public class StoreQueryService {

    private final StoresRepository storesRepository;
    private final SecurityUtil security;
    private final Optional<MenuReader> menuReader; // 구현체 없을 수 있으므로 Optional 주입
    private final GeocodingPort geocoding;         // 주소→좌표 변환 포트

    /**
     * 가게 검색 규칙
     * -------------------------------------------------
     * - address 가 있으면: 지오코딩으로 lat/lng 채워서 반경 검색
     * - lat/lng & radiusKm 가 있으면: 좌표 기반 반경 검색
     * - 둘 다 없으면: 키워드 검색만 수행(거리 null)
     */
    public Page<StoreListItemResponse> search(
            String keyword,
            String address,
            Double lat, Double lng, Double radiusKm,
            Pageable pageable
    ) {
        String q = (keyword == null) ? "" : keyword.trim();

        // 1) 주소만 들어온 경우: 지오코딩 수행해 좌표 보정
        if ((lat == null || lng == null) && address != null && !address.isBlank()) {
            LatLng p = geocoding.geocode(address)
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.BAD_REQUEST, "유효한 주소를 입력하세요."
                    ));
            lat = p.getLat();
            lng = p.getLng();
            // radiusKm 는 컨트롤러 기본값(예: 3km) 사용
        }

        // 2) 좌표 검색을 위한 반경(m) 계산 (좌표/반경 모두 있을 때만)
        Double radiusMeters = (lat != null && lng != null && radiusKm != null) ? radiusKm * 1000.0 : null;

        // native 쿼리 바인딩을 위한 기본값 (반경 null이면 거리 계산은 WHERE 에서 무시됨)
        double qLat = (lat != null) ? lat : 0.0;
        double qLng = (lng != null) ? lng : 0.0;

        // 3) 이름 부분검색 + 반경 필터 + 거리 정렬 (원시 배열 반환)
        Page<Object[]> page = storesRepository.searchWithDistanceRaw(q, qLat, qLng, radiusMeters, pageable);

        // 4) Row → DTO 매핑
        return page.map(row -> {
            int i = 0;
            Long id = ((Number) row[i++]).longValue();
            i++; // owner_id skip
            String name = (String) row[i++];
            String addressCol = (String) row[i++];
            Double latitude = ((Number) row[i++]).doubleValue();
            Double longitude = ((Number) row[i++]).doubleValue();
            Integer minOrderPrice = ((Number) row[i++]).intValue();
            LocalTime opensAt = ((java.sql.Time) row[i++]).toLocalTime();
            LocalTime closesAt = ((java.sql.Time) row[i++]).toLocalTime();
            Integer deliveryFee = ((Number) row[i++]).intValue();
            i++; // active skip
            i++; // retired_at skip
            LocalDateTime createdAt = ((java.sql.Timestamp) row[i++]).toLocalDateTime();
            LocalDateTime updatedAt = ((java.sql.Timestamp) row[i++]).toLocalDateTime();
            Double distance = row[i] != null ? ((Number) row[i]).doubleValue() : null;

            return new StoreListItemResponse(
                    id, name, addressCol, minOrderPrice, deliveryFee,
                    opensAt, closesAt,
                    isOpenNow(opensAt, closesAt),   // 현재 영업 여부 계산
                    latitude, longitude,
                    distance,                       // 좌표 미전달 시 null
                    createdAt, updatedAt
            );
        });
    }

    /**
     * 가게 상세 조회 (일반 사용자용)
     * - ACTIVE=true 가게만 노출
     * - 메뉴는 MenuReader 가 있으면 조회, 없으면 빈 리스트
     */
    public StoreDetailResponse getOne(Long storeId) {
        Stores s = storesRepository.findByIdAndActiveTrue(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));
        List<MenuSummaryResponse> menus = menuReader
                .map(r -> r.findMenusOfStore(s.getId()))
                .orElseGet(List::of); // 구현이 없으면 빈 리스트
        return toDetail(s, menus);
    }

    /**
     * 가게 상세 조회 (오너 전용)
     * - 본인 소유 가게만 접근 가능 (그 외 403)
     * - ACTIVE 여부와 무관하게 조회(운영 정책에 맞게 조정 가능)
     */
    public StoreDetailResponse getOneForOwner(Long storeId) {
        Long uid = security.currentUserId();    // 미설정 시 UNAUTHORIZED 예외
        Stores s = storesRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 가게입니다."));
        if (!s.getOwner().getId().equals(uid))
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 소유 가게만 조회할 수 있습니다.");
        List<MenuSummaryResponse> menus = menuReader
                .map(r -> r.findMenusOfStore(s.getId()))
                .orElseGet(List::of);   // 구현체 없으면 빈 리스트
        return toDetail(s, menus);
    }

    /**
     * 엔티티 + 메뉴목록 → 상세 DTO 변환
     */
    private StoreDetailResponse toDetail(Stores s, List<MenuSummaryResponse> menus) {
        return new StoreDetailResponse(
                s.getId(), s.getOwner().getId(),
                s.getName(), s.getAddress(),
                s.getMinOrderPrice(), s.getDeliveryFee(),
                s.getOpensAt(), s.getClosesAt(),
                isOpenNow(s.getOpensAt(), s.getClosesAt()),
                s.getLatitude(), s.getLongitude(),
                menus,
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }

    /**
     * 현재 영업 여부 계산
     * - 일반 케이스: open < close → [open, close] 범위 안이면 영업 중
     * - 자정 넘김:   open > close → [open, 24:00) ∪ [00:00, close] 면 영업 중
     */
    private boolean isOpenNow(LocalTime open, LocalTime close) {
        if (open == null || close == null) return false;
        LocalTime now = LocalTime.now();
        if (open.isBefore(close)) return !now.isBefore(open) && !now.isAfter(close);
        // 영업시작이 종료보다 늦으면 자정 넘김
        return !now.isBefore(open) || !now.isAfter(close);
    }
}
