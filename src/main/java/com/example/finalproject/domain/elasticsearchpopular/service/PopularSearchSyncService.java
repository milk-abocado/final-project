package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PopularSearchSyncService {

    private final StringRedisTemplate stringRedisTemplate;  // 문자열 기반
    private final ElasticsearchClient esClient;
    private static final String INDEX = "popular_searches_index";

    @Scheduled(fixedRate = 1000 * 60 * 5) // 1시간
    public void syncPopularSearches() throws Exception {
        Set<String> keys = stringRedisTemplate.keys("popular:*");
        if (keys == null) return;

        for (String key : keys) {
            String[] parts = key.split(":");
            String region = parts[1];
            String keyword = parts[2];
            String value = stringRedisTemplate.opsForValue().get(key);

            long count = (value == null) ? 0L : Long.parseLong(value);
            String docId = region + "_" + keyword;

            try {
                esClient.update(u -> u
                                .index(INDEX)
                                .id(docId)
                                .doc(Map.of(
                                        "region", region,
                                        "keyword", keyword,
                                        "count", count,
                                        "type", "redis" //Redis -> ES 동기화 시 색인
                                )),
                        Map.class);
            } catch (Exception e) {
                esClient.index(i -> i
                        .index(INDEX)
                        .id(docId)
                        .document(Map.of(
                                "region", region,
                                "keyword", keyword,
                                "count", count,
                                "created_at", new Date(),
                                "type", "redis" //신규 색인
                        ))
                );
            }
        }
    }
}
