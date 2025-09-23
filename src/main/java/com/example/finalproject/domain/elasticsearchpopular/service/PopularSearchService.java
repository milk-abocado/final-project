package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PopularSearchService {

    private final StringRedisTemplate redisTemplate;
    private final ElasticsearchClient esClient;
    private final PopularSearchRepository popularSearchRepository;

    // Redis → Top10 → Elasticsearch & DB
    public void aggregateAndStore() throws Exception {
        Set<String> keys = redisTemplate.keys("search_count:*");
        if (keys == null || keys.isEmpty()) return;

        Map<String, Map<String, Long>> regionKeywordCount = new HashMap<>();

        for (String key : keys) {
            String[] parts = key.split(":");
            if (parts.length < 3) continue;
            String region = parts[1];
            String keyword = parts[2];
            long count = Long.parseLong(redisTemplate.opsForValue().get(key));

            regionKeywordCount.computeIfAbsent(region, r -> new HashMap<>())
                    .merge(keyword, count, Long::sum);
        }

        List<PopularSearch> allResults = new ArrayList<>();
        List<BulkOperation> bulkOps = new ArrayList<>();

        for (Map.Entry<String, Map<String, Long>> entry : regionKeywordCount.entrySet()) {
            String region = entry.getKey();

            List<Map.Entry<String, Long>> top10 = entry.getValue().entrySet()
                    .stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .collect(Collectors.toList());

            int rank = 1;
            for (Map.Entry<String, Long> e : top10) {
                PopularSearch ps = new PopularSearch();
                ps.setRegion(region);
                ps.setKeyword(e.getKey());
                ps.setCount(e.getValue().intValue());
                ps.setRank(rank++);

                allResults.add(ps);

                bulkOps.add(BulkOperation.of(b -> b
                        .index(idx -> idx
                                .index("searches_index")
                                .id(region + "_" + ps.getKeyword())
                                .document(ps)
                        )
                ));
            }
        }

        if (!bulkOps.isEmpty()) {
            BulkResponse response = esClient.bulk(b -> b.operations(bulkOps));
            if (response.errors()) {
                System.err.println("Bulk indexing had errors: " + response);
            }
            popularSearchRepository.deleteAll();
            popularSearchRepository.saveAll(allResults);
        }
    }
}
