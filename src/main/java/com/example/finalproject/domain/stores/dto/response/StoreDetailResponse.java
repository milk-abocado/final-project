package com.example.finalproject.domain.stores.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * StoreDetailResponse
 * -------------------------------------------------
 * - 가게 상세 정보를 내려줄 때 사용하는 DTO
 * - 단순 리스트 조회가 아닌, 가게의 모든 핵심 정보를 포함
 */
public record StoreDetailResponse(
        Long id,                // 가게 ID
        Long ownerId,           // 가게 주인 ID
        String name,            // 가게 이름
        String address,         // 주소
        Integer minOrderPrice,  // 최소 주문 금액
        Integer deliveryFee,    // 배달비
        LocalTime opensAt,      // 영업 시작 시간
        LocalTime closesAt,     // 영업 종료 시간
        boolean openNow,        // 현재 영업 여부
        Double latitude,        // 위도
        Double longitude,       // 경도
        List<MenuSummaryResponse> menus, // 메뉴 요약 리스트
        LocalDateTime createdAt,// 생성일
        LocalDateTime updatedAt // 수정일
) {}
