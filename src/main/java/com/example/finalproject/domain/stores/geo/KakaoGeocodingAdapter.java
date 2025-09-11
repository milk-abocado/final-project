package com.example.finalproject.domain.stores.geo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * KakaoGeocodingAdapter
 * -------------------------------------------------
 * - Kakao Local API 를 이용해 주소 → 위도/경도 변환을 수행하는 어댑터
 * - GeocodingPort 인터페이스의 구현체 (헥사고날 아키텍처 - 어댑터 계층)
 * - test 프로필에서는 비활성화 (@Profile("!test"))
 */
@Slf4j
@Component
@Profile("!test") // test 프로필이 아닐 때만 활성화 (dev/prod 환경에서만 사용)
@RequiredArgsConstructor
public class KakaoGeocodingAdapter implements GeocodingPort {

    private final WebClient kakaoWebClient;

    @Value("${kakao.local.rest-api-key}") // application.yml / properties 에 설정 필요
    private String kakaoRestApiKey;

    /**
     * 주소 문자열을 위도/경도로 변환
     * @param address 변환할 주소
     * @return Optional<LatLng>, 실패 시 Optional.empty()
     */
    @Override
    public Optional<LatLng> geocode(String address) {
        if (address == null || address.isBlank()) return Optional.empty();

        try {
            // Kakao Local API 요청
            KakaoAddressResponse res = kakaoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address.trim()) // query 자동 인코딩
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                String msg = "Kakao Local API error: "
                                        + clientResponse.statusCode() + " body=" + body;
                                log.warn("{}", msg);
                                return Mono.error(new IllegalStateException(msg));
                            }))
                    .bodyToMono(KakaoAddressResponse.class)
                    .block();

            // 응답 검증
            if (res == null) {
                log.warn("Kakao response is null. address='{}'", address);
                return Optional.empty();
            }
            if (res.documents == null || res.documents.length == 0) {
                // 정상적인 '주소 미매칭' 상황
                log.info("No geocoding match. address='{}'", address);
                return Optional.empty();
            }

            // 첫 번째 매칭 결과 사용
            KakaoAddressResponse.Doc doc = res.documents[0];
            double lng = parseDoubleSafe(doc.x);
            double lat = parseDoubleSafe(doc.y);

            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                log.warn("Invalid lat/lng parsed. address='{}', x='{}', y='{}'", address, doc.x, doc.y);
                return Optional.empty();
            }

            // ⚠️ Kakao API는 x=경도(lng), y=위도(lat)
            return Optional.of(new LatLng(lat, lng));
        } catch (Exception e) {
            log.warn("Geocoding failed. address='{}', reason={}", address, e.toString());
            return Optional.empty();
        }
    }

    /**
     * 안전한 double 파싱 메서드
     */
    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
    }

    /**
     * Kakao API 응답 DTO (내부 전용)
     */
    static class KakaoAddressResponse {
        public Doc[] documents;
        static class Doc {
            public String x; // 경도 (longitude)
            public String y; // 위도 (latitude)
        }
    }
}
