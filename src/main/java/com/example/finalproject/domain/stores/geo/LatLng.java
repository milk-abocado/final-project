package com.example.finalproject.domain.stores.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LatLng
 * -------------------------------------------------
 * - 위도(latitude), 경도(longitude) 정보를 담는 단순 DTO 클래스
 * - 불변 객체(필드가 final)로 설계하여 값 변경 불가
 * - 외부 지오코딩 API 응답을 도메인에서 사용할 때 매핑 용도로 활용
 */
@Getter
@AllArgsConstructor
public class LatLng {
    private final double lat; // 위도
    private final double lng; // 경도
}
