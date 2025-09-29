package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularSearchService {
    private final ElasticsearchClient esClient;
    private static final String INDEX = "popular_searches_index";

    /**
     * 검색어 자동완성: search_as_you_type 기반
     */
    public List<String> autoComplete(String keyword, String region) throws Exception {
        var resp = esClient.search(s -> s
                        .index(INDEX)
                        .size(20)
                        .query(q -> q
                                .bool(b -> b
                                        .must(m -> m.term(t -> t.field("region").value(region)))
                                        .must(m -> m.term(t -> t.field("type").value("redis"))) //redis 기반만
                                        .must(m -> m.matchPhrasePrefix(mp -> mp
                                                .field("keyword") //search_as_you_type 필드
                                                .query(keyword)
                                        ))
                                )
                        )
                        .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc)))
                , Map.class
        );

        //중복제거 (LinkedHashSet -> 순서 보장 + 중복 제거)
        Set<String> uniqueResults = new LinkedHashSet<>();
        for (var hit : resp.hits().hits()) {
            uniqueResults.add(hit.source().get("keyword").toString());
            if (uniqueResults.size() >= 10) break; //최대 10개까지만
        }
        return new ArrayList<>(uniqueResults);
    }

    /**
     * Elastic 조회, 지역별 Top10
     */

    public List<Map<String, Object>> getTop10ByRegion(String region) throws Exception {
        var response = esClient.search(s -> s
                        .index(INDEX)
                        .size(10)
                        .query(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("region").value(region)))
                        .must(m -> m.term(t -> t.field("type").value("redis"))) //Redis 기반만 필터링
                ))
        .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc)))
                , Map.class);

        List<Map<String, Object>> top10 = new ArrayList<>();
        for (var hit : response.hits().hits()) {
            top10.add(hit.source());
        }
        return top10;
    }

    public List<PopularSearch> getPopularKeywords(String region) {
        return popularSearchRepository.findTop10ByRegionOrderByCountDesc(region);
    }
}

