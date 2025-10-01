package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 인기 검색어 랭킹 집계 배치 잡
 * - 일정 주기로 Redis / Elasticsearch 데이터를 읽어 와서
 *   RDB 에 인기 검색어 랭킹을 업데이트
 */
@Component
@RequiredArgsConstructor
public class PopularSearchRankJob {

    private final ElasticsearchClient esClient;              // Elasticsearch 클라이언트
    private final PopularSearchRepository popularSearchRepository; // RDB 저장소
    private final StringRedisTemplate stringRedisTemplate;   // Redis 템플릿

    private static final String INDEX = "popular_searches_index";

    /**
     * 1시간마다 랭킹 갱신 (테스트 환경에서는 15초마다 실행)
     */
    @Scheduled(
            fixedRate = 1000 * 60 * 60,   // 1시간마다 실행
            initialDelay = 1000 * 60 * 5  // 5분 지연 후 첫 실행
    )
    @Transactional
    public void recomputeRanking() {
        // 1) 지역 목록 수집 (ES 우선, 실패 시 Redis 사용)
        Set<String> regions = collectRegionsFromES();
        if (regions.isEmpty()) {
            regions = collectRegionsFromRedis();
        }

        // 2) 지역별 Top 키워드 가져와서 DB 반영
        for (String region : regions) {
            List<Map<String, Object>> topList = fetchTopFromES(region);
            if (topList.isEmpty()) {
                topList = fetchTopFromRedis(region);
            }

            // --- 랭킹 초기화 로직 ---
            // Top 키워드 목록만 유지, 나머지는 ranking=0 처리
            List<String> topKeywords = topList.stream()
                    .map(m -> String.valueOf(m.get("keyword")))
                    .filter(Objects::nonNull)
                    .toList();

            if (topKeywords.isEmpty()) {
                // Top 결과가 없으면 해당 지역 전체 초기화
                popularSearchRepository.resetAllRankingByRegion(region);
            } else {
                // Top 키워드 외 나머지 초기화
                popularSearchRepository.resetRankingByRegionExcluding(region, topKeywords);
            }

            // --- 랭킹 반영 ---
            int rank = 1;
            List<PopularSearches> batch = new ArrayList<>();
            for (Map<String, Object> item : topList) {
                String keyword = String.valueOf(item.get("keyword"));
                Number countNum = (Number) item.getOrDefault("searchCount", 0);
                long count = (countNum == null) ? 0L : countNum.longValue();

                // 기존 DB에 있으면 가져오고, 없으면 신규 생성
                PopularSearches entity = popularSearchRepository
                        .findByRegionAndKeyword(region, keyword)
                        .orElseGet(() -> {
                            PopularSearches p = new PopularSearches();
                            p.setRegion(region);
                            p.setKeyword(keyword);
                            p.setSearchCount(0);
                            return p;
                        });

                // 검색 수와 랭킹 갱신
                entity.setSearchCount((int) count);
                entity.setRanking(rank++);
                batch.add(entity);
            }

            // 배치 저장
            if (!batch.isEmpty()) {
                popularSearchRepository.saveAll(batch);
            }
        }
    }

    /**
     * ES 집계에서 지역 목록 추출
     * - 우선 region.keyword 필드로 시도
     * - 실패하면 region 필드로 재시도
     */
    private Set<String> collectRegionsFromES() {
        Set<String> regions = tryTermsAggOnField("region.keyword");
        if (!regions.isEmpty()) return regions;
        return tryTermsAggOnField("region");
    }

    /**
     * 특정 필드 기준 terms aggregation 수행
     * @param field 집계 필드
     */
    private Set<String> tryTermsAggOnField(String field) {
        try {
            SearchResponse<Map> resp = esClient.search(s -> s
                            .index(INDEX)
                            .size(0) // 문서는 불필요, 집계만 수행
                            .query(q -> q.term(t -> t.field("type.keyword").value("redis")))
                            .aggregations("regions", a -> a.terms(t -> t
                                    .field(field)
                                    .size(1000)
                            )),
                    Map.class
            );

            Aggregate agg = resp.aggregations().get("regions");
            if (agg == null || agg.sterms() == null || agg.sterms().buckets() == null) {
                return Collections.emptySet();
            }

            // 버킷 API는 array / keyed 두 가지 지원 → 둘 다 처리
            Set<String> out = new HashSet<>();
            var buckets = agg.sterms().buckets();

            // 일반 배열 버킷 처리
            if (buckets.array() != null) {
                for (StringTermsBucket b : buckets.array()) {
                    if (b.key() != null) out.add(b.key().stringValue());
                }
            }

            // Keyed 버킷 처리
            if (buckets.keyed() != null) {
                for (var entry : buckets.keyed().entrySet()) {
                    var b = entry.getValue();
                    if (b.key() != null) out.add(b.key().stringValue());
                }
            }
            return out;

        } catch (Exception e) {
            // 예외: 매핑 불일치, field_data 비활성 등
            return Collections.emptySet();
        }
    }

    /**
     * Redis 키 스캔을 통한 지역 목록 수집 (백업 경로)
     */
    private Set<String> collectRegionsFromRedis() {
        Set<String> regions = new HashSet<>();
        for (String key : scan("popular:*")) {
            String[] parts = key.split(":", 3);
            if (parts.length >= 3) {
                regions.add(parts[1]);
            }
        }
        return regions;
    }

    /**
     * ES 에서 특정 지역 상위 키워드 조회
     */
    private List<Map<String, Object>> fetchTopFromES(String region) {
        try {
            SearchResponse<Map> resp = esClient.search(s -> s
                            .index(INDEX)
                            .size(100) // 최대 100개
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("region.keyword").value(region)))
                                    .must(m -> m.term(t -> t.field("type.keyword").value("redis")))
                            ))
                            .sort(so -> so.field(f -> f.field("searchCount").order(SortOrder.Desc))),
                    Map.class
            );

            // 검색 결과에서 _source만 추출
            return resp.hits().hits().stream()
                    .map(h -> {
                        Map<String, Object> src = h.source();
                        return (src == null) ? Map.<String, Object>of() : src;
                    })
                    .filter(m -> !m.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 실패 시 빈 리스트 반환
            return Collections.emptyList();
        }
    }

    /**
     * Redis 에서 특정 지역 상위 키워드 조회 (백업 경로)
     */
    private List<Map<String, Object>> fetchTopFromRedis(String region) {
        Map<String, Long> counter = new HashMap<>();
        for (String key : scan("popular:" + region + ":*")) {
            String[] parts = key.split(":", 3);
            if (parts.length < 3) continue;
            String keyword = parts[2];
            String v = stringRedisTemplate.opsForValue().get(key);
            long c = (v == null) ? 0L : Long.parseLong(v);
            counter.put(keyword, c);
        }

        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(100)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("region", region);
                    m.put("keyword", e.getKey());
                    m.put("searchCount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Redis SCAN 유틸 메서드
     */
    private List<String> scan(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            throw new RuntimeException("Redis scan 실패", e);
        }
        return keys;
    }
}
