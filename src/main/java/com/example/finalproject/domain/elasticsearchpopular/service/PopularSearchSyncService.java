package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Redis → Elasticsearch 데이터 동기화 서비스
 * - Redis에 쌓인 검색어 집계 데이터를 주기적으로 Elasticsearch로 반영
 * - Upsert 방식을 통해 새로운 키워드는 생성, 기존 키워드는 갱신
 */
@Service
@RequiredArgsConstructor
public class PopularSearchSyncService {

    private final StringRedisTemplate stringRedisTemplate;  // 문자열 기반 Redis 템플릿
    private final ElasticsearchClient esClient;             // Elasticsearch 클라이언트
    private static final String INDEX = "popular_searches_index";

    /**
     * 동기화 스케줄러
     * - 기본 주기: 10초마다 실행 (테스트용)
     * - 운영 시: 1시간 주기 실행 권장
     */
    //    @Scheduled(fixedRate = 1000 * 60 * 60) // 실제 운영: 1시간
    @Scheduled(
            fixedRate = 1000 * 60 * 60,   // 1시간마다 실행
            initialDelay = 1000 * 60 * 5  // 5분 지연 후 첫 실행
    )
    public void syncPopularSearches() {
        // Redis에서 키 목록 가져오기
        List<String> keys = scan();
        if (keys.isEmpty()) return;

        for (String key : keys) {
            try {
                // 키 포맷: popular:{region}:{keyword}
                String[] parts = key.split(":", 3);
                if (parts.length < 3) continue; // 잘못된 키 스킵

                String region = parts[1];
                String keyword = parts[2];
                if (isBlank(region) || isBlank(keyword)) continue;

                // Redis 값(검색 횟수) 읽기
                String value = stringRedisTemplate.opsForValue().get(key);
                long count = parseLongSafe(value);

                // Elasticsearch 문서 ID (region+keyword 조합)
                String docId = region + "_" + keyword;

                // update 시 적용될 문서
                Map<String, Object> doc = new HashMap<>();
                doc.put("region", region);
                doc.put("keyword", keyword);
                doc.put("searchCount", count);
                doc.put("type", "redis");
                doc.put("updated_at", new Date());

                // 최초 insert 시 기본값 포함 (created_at 지정)
                Map<String, Object> upsert = new HashMap<>();
                upsert.put("region", region);
                upsert.put("keyword", keyword);
                upsert.put("searchCount", count);
                upsert.put("type", "redis");
                Date now = new Date();
                upsert.put("created_at", now);
                upsert.put("updated_at", now);

                // Elasticsearch upsert 실행
                esClient.update(u -> u
                                .index(INDEX)
                                .id(docId)
                                .doc(doc)
                                .docAsUpsert(true)   // 없으면 새로 생성
                                .upsert(upsert)      // 일부 ES 버전 호환성을 위해 명시
                                .refresh(Refresh.True) // 테스트 시 즉시 검색 반영 (운영은 False 권장)
                        , Map.class);

            } catch (Exception e) {
                // 개별 키 단위 에러 로깅 후 무시 (다른 키 동기화는 계속 진행)
                System.err.println("[PopularSync][ERROR] key=" + key + " msg=" + e.getMessage());
            }
        }
    }

    /**
     * Redis SCAN 유틸
     * - pattern: popular:*
     * - match + count 옵션으로 키를 효율적으로 수집
     */
    private List<String> scan() {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match("popular:*").count(1000).build())) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            throw new RuntimeException("Redis scan 실패", e);
        }
        return keys;
    }

    /**
     * 안전한 Long 파싱 (null 또는 숫자 아님 → 0)
     */
    private long parseLongSafe(String s) {
        if (s == null) return 0L;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 문자열 공백/널 체크
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
