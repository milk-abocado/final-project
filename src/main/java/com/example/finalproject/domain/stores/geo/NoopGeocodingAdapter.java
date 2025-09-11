package com.example.finalproject.domain.stores.geo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * NoopGeocodingAdapter
 * -------------------------------------------------
 * - 테스트 환경에서만 활성화되는 GeocodingPort 구현체
 * - 실제 외부 API(Kakao, Google 등)를 호출하지 않고,
 *   항상 Optional.empty() 를 반환
 */
@Component
@Profile("test") // 테스트 프로필에서만 활성화
public class NoopGeocodingAdapter implements GeocodingPort {
    @Override
    public Optional<LatLng> geocode(String address) {
        // 항상 빈 결과 반환 → 외부 의존성 제거
        return Optional.empty();
    }
}