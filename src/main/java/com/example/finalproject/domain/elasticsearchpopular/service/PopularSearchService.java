package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.entity.Region;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import com.example.finalproject.domain.elasticsearchpopular.repository.RegionRepository;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularSearchService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestHighLevelClient elasticClient;
    private final PopularSearchRepository popularSearchRepository;
    private final RegionRepository regionRepository;

    public void aggregationPopularSearches() throws IOException {
        popularSearchRepository.deleteAll(); //기존 데이터 초기화

        //DB에서 지역 목록 가져오기
        List<Region> regions = regionRepository.findAll();

        for (Region regionEntity : regions) {
            String region = regionEntity.getRegionName();
            String redisKey = "popular:keyword:" + region; //전국 단위 예시

            //Redis에서 Top10 가져오기
            Set<ZSetOperations.TypedTuple<Object>> topKeywords =
                    redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, 9);

            if (topKeywords == null || topKeywords.isEmpty()) continue;

            //ElasticSearch Bulk Insert
            BulkRequest bulkRequest = new BulkRequest();
            List<PopularSearch> regionPopularList = new ArrayList<>();
            int rank = 1;

            for (ZSetOperations.TypedTuple<Object> tuple : topKeywords) {
                String keyword = (String) tuple.getValue();
                double count = tuple.getScore() != null ? tuple.getScore() : 0;

                //ES 저장
                IndexRequest indexRequest = new IndexRequest("searches_index")
                        .source(Map.of(
                                "keyword", keyword,
                                "region", region,
                                "count", count,
                                "created_at", Instant.now().toString()
                        ));
                bulkRequest.add(indexRequest);

                // DB 저장용
                PopularSearch ps = new PopularSearch();
                ps.setKeyword(keyword);
                ps.setRegion(region);
                ps.setCount((int) count);
                ps.setRank(rank++);
                regionPopularList.add(ps);
            }
            elasticClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            popularSearchRepository.saveAll(regionpopularList);
        }
    }
}

