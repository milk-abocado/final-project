package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchErrorCode;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchException;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

    private final ElasticsearchClient esClient;
    private final PopularSearchRepository popularSearchRepository;
    private static final String INDEX = "popular_searches_index";

    /**
     * Elasticsearch 기반 자동완성 (search_as_you_type)
     */
    public List<String> autoComplete(String keyword, String region, int maxResults) {
        validateParams(keyword, region);

        try {
            var resp = esClient.search(s -> s
                            .index(INDEX)
                            .size(Math.max(20, maxResults)) // 충분히 큰 수
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m.term(t -> t.field("region").value(region)))
                                            .must(m -> m.term(t -> t.field("type").value("redis")))
                                            .must(m -> m.matchPhrasePrefix(mp -> mp
                                                    .field("keyword")
                                                    .query(keyword)
                                            ))
                                    )
                            )
                            .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc))),
                    Map.class
            );

            Set<String> uniqueResults = resp.hits().hits().stream()
                    .map(hit -> hit.source().get("keyword").toString())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (uniqueResults.isEmpty()) {
                throw new PopularSearchException(PopularSearchErrorCode.NOT_FOUND, "자동완성 결과 없음");
            }

            return uniqueResults.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());

        } catch (PopularSearchException e) {
            throw e;
        } catch (Exception e) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.ELASTIC_ERROR,
                    "Elasticsearch 조회 실패: " + e.getMessage()
            );
        }
    }

    private void validateParams(String keyword, String region) {
    }

    /**
     * Elasticsearch Top N 조회
     */
    public List<Map<String, Object>> getTopByRegion(String region, int topN) {
        validateRegion(region);

        try {
            var resp = esClient.search(s -> s
                            .index(INDEX)
                            .size(topN)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("region").value(region)))
                                    .must(m -> m.term(t -> t.field("type").value("redis")))
                            ))
                            .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc))),
                    Map.class
            );

            List<Map<String, Object>> results = resp.hits().hits().stream()
                    .map(hit -> (Map<String, Object>) hit.source())
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                throw new PopularSearchException(PopularSearchErrorCode.NOT_FOUND, "해당 region에 대한 데이터 없음");
            }

            return results;

        } catch (PopularSearchException e) {
            throw e;
        } catch (Exception e) {
            throw new PopularSearchException(PopularSearchErrorCode.ELASTIC_ERROR,
                    "Elasticsearch 조회 실패: " + e.getMessage());
        }
    }

    private void validateRegion(String region) {
    }

    /**
     * JPA(DB) 기반 Top N 조회
     */
    public List<PopularSearch> getTopFromDB(String region, int topN) {
        if  (region == null || region.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST, "region 값이 비어있습니다."
            );
        }

        List<PopularSearch> results = popularSearchRepository
                .findTopNByRegionOrderByCountDesc(region, PageRequest.of(0, topN));

        if (results.isEmpty()) {
            throw new PopularSearchException(PopularSearchErrorCode.NOT_FOUND, "DB에서 해당 region 데이터 없음");
        }

        return results;
    }
}
