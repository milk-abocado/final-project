package com.example.finalproject.domain.stores.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * StoreDetailResponse
 * -------------------------------------------------
 * - 가게 상세 정보를 내려줄 때 사용하는 DTO
 * - 단순 리스트 조회가 아닌, 가게의 모든 핵심 정보를 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailResponse {
    private Long id;                         // 가게 ID
    private Long ownerId;                    // 가게 주인 ID
    private String name;                     // 가게 이름
    private String address;                  // 주소
    private Integer minOrderPrice;           // 최소 주문 금액
    private Integer deliveryFee;             // 배달비
    private LocalTime opensAt;               // 영업 시작 시간
    private LocalTime closesAt;              // 영업 종료 시간
    private boolean openNow;                 // 현재 영업 여부
    private Double latitude;                 // 위도
    private Double longitude;                // 경도
    private List<MenuSummaryResponse> menus; // 메뉴 요약 리스트
    private LocalDateTime createdAt;         // 생성일
    private LocalDateTime updatedAt;         // 수정일

    private String statusMessage;            // 가게 상태 (영업 중 또는 폐업된 가게)
}
