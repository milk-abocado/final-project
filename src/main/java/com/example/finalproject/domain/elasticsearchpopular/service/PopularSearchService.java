package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

    private final ElasticsearchClient esClient;
    private final PopularSearchRepository popularSearchRepository;

    // 자동완성: search_as_you_type
    public List<String> autoComplete(String input, String region) throws IOException, java.io.IOException {
        SearchResponse<JsonNode> response = esClient.search(s -> s
                .index("popular_searches_index")
                .query(q -> q
                        .multiMatch(m -> m
                                .fields("keyword")
                                .query(input)
                                .type(TextQueryType.BoolPrefix) //s_a_y_t 자동완성
                        )
                )
                .size(10)
                .postFilter(f -> f.term(t -> t.field("region").value(region))),
                JsonNode.class
        );

        return response.hits().hits()
                .stream()
                .map(hit -> hit.source().get("keyword").asText())
                .toList();
    }

    //DB에서 인기검색어 조회
    public List<PopularSearch> getPopularKeywords(String region) {
        return popularSearchRepository.findTop10ByRegionOrderByRankAsc(region);
    }

    //배치 작업: ES 집계 -> DB 저장
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void refreshPopularKeywords() throws IOException, java.io.IOException {
        //1. ES에서 집계 쿼리 실행
        AggregateResponse aggregateResponse = esClient.search(s -> s
                .index("popular_searches_index")
                .size(0)
                .aggregations("by_region", a -> a
                        .terms(t -> t.field("region.keyword"))
                        .aggregations("top_keywords", aa -> aa
                                .terms(t -> t.field("keyword.keyword").size(10))
                        )
                ),
                JsonNode.class
        ).aggregations();

        // 2. 결과를 DB에 반영 (지역별 상위 10개 저장)
        popularSearchRepository.deleteAll(); // 최신화
        // ... aggResponse 파싱 → PopularSearch 엔티티 저장
    }

    public List<String> suggestKeywords(String region, String q) {
    }
}
