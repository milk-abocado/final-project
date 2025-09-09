package com.example.finalproject.domain.stores.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * StoreListItemResponse
 * -------------------------------------------------
 * - 가게 "목록 조회" 시 사용하는 DTO
 * - 상세 정보가 아닌, 리스트 뷰에 필요한 핵심 정보만 포함
 */
public record StoreListItemResponse(
        Long id,              // 가게 ID
        String name,          // 가게 이름
        String address,       // 주소
        Integer minOrderPrice,// 최소 주문 금액
        Integer deliveryFee,  // 배달비
        LocalTime opensAt,    // 영업 시작 시간
        LocalTime closesAt,   // 영업 종료 시간
        boolean openNow,      // 현재 영업 여부
        Double latitude,      // 위도
        Double longitude,     // 경도
        Double distanceMeters,// 좌표 기준 거리 (null 가능)
        LocalDateTime createdAt, // 생성일
        LocalDateTime updatedAt  // 수정일
) {}
