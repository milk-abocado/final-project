package com.example.finalproject.domain.stores.dto.response;

import com.example.finalproject.domain.stores.category.StoreCategory;
import com.example.finalproject.domain.stores.entity.StoreCategoryLink;
import com.example.finalproject.domain.stores.entity.Stores;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * StoreListItemResponse
 * -------------------------------------------------
 * - 가게 "목록 조회" 시 사용하는 DTO
 * - 상세 정보가 아닌, 리스트 뷰에 필요한 핵심 정보만 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StoreListItemResponse {
    private Long id;                    // 가게 ID
    private String name;                // 가게 이름
    private String address;             // 주소
    private Integer minOrderPrice;      // 최소 주문 금액
    private Integer deliveryFee;        // 배달비
    private LocalTime opensAt;          // 영업 시작 시간
    private LocalTime closesAt;         // 영업 종료 시간
    private boolean openNow;            // 현재 영업 여부
    private Double latitude;            // 위도
    private Double longitude;           // 경도
    private Double distanceMeters;      // 좌표 기준 거리 (null 가능)
    private LocalDateTime createdAt;    // 생성일
    private LocalDateTime updatedAt;    // 수정일
    private List<StoreCategory> categories; // 카테고리 목록

    /**
     * 엔티티 Stores 객체를 받아서 StoreListItemResponse DTO로 변환
     * @param s Stores 엔티티
     * @param distanceMeters 좌표 기준 거리 (없을 경우 null)
     * @return StoreListItemResponse DTO
     */
    public static StoreListItemResponse of(Stores s, Double distanceMeters) {
        // 카테고리 링크에서 카테고리 enum만 추출 후 이름순 정렬
        var cats = s.getCategoryLinks().stream()
                .map(StoreCategoryLink::getCategory)
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        // DTO 생성
        return new StoreListItemResponse(
                s.getId(),
                s.getName(),
                s.getAddress(),
                s.getMinOrderPrice(),
                s.getDeliveryFee(),
                s.getOpensAt(),
                s.getClosesAt(),
                s.isActive(),   // 현재 영업 여부 (Stores.active 값)
                s.getLatitude(),
                s.getLongitude(),
                distanceMeters,
                s.getCreatedAt(),
                s.getUpdatedAt(),
                cats
        );
    }
}
