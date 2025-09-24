package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularSearchSyncService {

    /**
     * Redis -> Elastic Cloud
     */

    private final RedisTemplate<String, Integer> redisTemplate;
    private final ElasticsearchClient esClient;
    private static final String INDEX = "popular_searches_index";

    @Scheduled(fixedRate = 1000 * 60 * 60) // 1시간
    public void syncPopularSearches() throws Exception {
        Set<String> keys = redisTemplate.keys("popular:*");
        if (keys == null) return;

        Map<String, Integer> data = redisTemplate.opsForValue().multiGet(keys)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        k -> k.toString(),  // 명시적으로 String으로 변환
                        v -> v               // 값은 그대로
                ));

        for (String key : keys) {
            String[] parts = key.split(":");
            String region = parts[1];
            String keyword = parts[2];
            Integer count = redisTemplate.opsForValue().get(key);

            String docId = region + "_" + keyword;

            // Elastic에 문서 업데이트
            try {
                esClient.update(u -> u
                                .index(INDEX)
                                .id(docId)
                                .doc(Map.of("region", region, "keyword", keyword, "count", count))
                        , Map.class);
            } catch (Exception e) {
                esClient.index(i -> i
                        .index(INDEX)
                        .id(docId)
                        .document(Map.of(
                                "region", region,
                                "keyword", keyword,
                                "count", count,
                                "created_at", new Date()
                        ))
                );
            }
        }
    }
}
