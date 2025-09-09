package com.example.finalproject.domain.stores.geo;

import java.util.Optional;

/**
 * GeocodingPort
 * -------------------------------------------------
 * - 주소(String)를 위도/경도 좌표(LatLng)로 변환하는 기능을 추상화한 포트 인터페이스
 * - 외부 지오코딩 API (예: Kakao Local API, Google Maps API 등) 와의 의존성을
 *   도메인 계층에서 분리하기 위해 정의됨 (헥사고날 아키텍처/포트-어댑터 패턴)
 */
public interface GeocodingPort {
    /**
     * 주소를 위도/경도로 변환
     *
     * @param address 변환할 주소
     * @return 위도/경도를 담은 Optional, 실패 시 empty
     */
    Optional<LatLng> geocode(String address);
}
