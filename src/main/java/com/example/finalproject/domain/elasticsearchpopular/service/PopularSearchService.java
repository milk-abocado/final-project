package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
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
    private final RestHighLevelClient client;
    private final PopularSearchRepository repository;
    private final com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchRepository popularSearchRepository;

    public void aggregationPopularSearches() throws IOException {
        String redisKey = "popular:keyword:all"; //전국 단위 예시

        //Redis에서 Top10 가져오기
        Set<ZSetOperations.TypedTuple<Object>> topKeywords =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, 9);

        if (topKeywords == null) return;

        //ElasticSearch Bulk Insert
        BulkRequest bulkRequest = new BulkRequest();
        for (ZSetOperations.TypedTuple<Object> tuple : topKeywords) {
            String keyword = (String) tuple.getValue();
            double count = tuple.getScore() != null ? tuple.getScore() : 0;

            IndexRequest indexRequest = new IndexRequest("searches_index")
                    .source(Map.of(
                            "keyword", keyword,
                            "count", count,
                            "created_at", Instant.now().toString()
                    ));
            bulkRequest.add(indexRequest);
        }
        elasticClient.bulk(bulkRequest, RequestOptions.DEFAULT);

        //DB 업데이트 (캐시 테이블)
        popularSearchRepository.deleteAll();
        int rank = 1;
        List<PopularSearch> popularList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : topKeywords) {
        PopularSearch popularSearch = new PopularSearch();
        popularSearch.setKeyword((String) tuple.getValue());
        popularSearch.setCount(tuple.getScore() != null ? tuple.getScore().intValue() : 0);
        popularList.setRank(rank++);
        popularList.add(popularSearch);
        }
        popularSearchRepository.saveAll(popularList);
    }
    }
