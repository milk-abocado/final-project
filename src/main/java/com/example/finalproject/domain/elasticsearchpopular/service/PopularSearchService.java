package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchErrorCode;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchException;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularSearchService {
    private final ElasticsearchClient esClient;
    private static final String INDEX = "popular_searches_index";
    private final PopularSearchRepository popularSearchRepository;

    /**
     * 검색어 자동완성: search_as_you_type 기반
     */
    public List<String> autoComplete(String keyword, String region) {
        // ✅ 파라미터 검증
        if (region == null || region.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST,
                    "region 값이 비어있습니다."
            );
        }
        if (keyword == null || keyword.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST,
                    "keyword 값이 비어있습니다."
            );
        }

        try {
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
                            .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc))),
                    Map.class
            );

            // ✅ 결과 파싱 + 중복 제거
            Set<String> uniqueResults = new LinkedHashSet<>();
            for (var hit : resp.hits().hits()) {
                uniqueResults.add(hit.source().get("keyword").toString());
                if (uniqueResults.size() >= 10) break; //최대 10개
            }

            if (uniqueResults.isEmpty()) {
                throw new PopularSearchException(
                        PopularSearchErrorCode.NOT_FOUND,
                        "자동완성 결과 없음"
                );
            }

            return new ArrayList<>(uniqueResults);

        } catch (PopularSearchException e) {
            throw e; // 그대로 전달
        } catch (Exception e) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.ELASTIC_ERROR,
                    "Elasticsearch 조회 실패: " + e.getMessage()
            );
        }
    }

    /**
     * Elastic 조회, 지역별 Top10
     */
    public List<Map<String, Object>> getTop10ByRegion(String region) {
        // ✅ 파라미터 검증
        if (region == null || region.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST,
                    "region 값이 비어있습니다."
            );
        }

        try {
            var response = esClient.search(s -> s
                            .index(INDEX)
                            .size(10)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("region").value(region)))
                                    .must(m -> m.term(t -> t.field("type").value("redis"))) //Redis 기반만 필터링
                            ))
                            .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc))),
                    Map.class
            );

            List<Map<String, Object>> top10 = new ArrayList<>();
            for (var hit : response.hits().hits()) {
                top10.add(hit.source());
            }

            if (top10.isEmpty()) {
                throw new PopularSearchException(
                        PopularSearchErrorCode.NOT_FOUND,
                        "해당 region에 대한 데이터 없음"
                );
            }

            return top10;

        } catch (PopularSearchException e) {
            throw e; // 그대로 전달
        } catch (Exception e) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.ELASTIC_ERROR,
                    "Elasticsearch 조회 실패: " + e.getMessage()
            );
        }
    }

    /**
     * JPA 기반 조회 (DB)
     */
    public List<PopularSearch> getPopularKeywords(String region) {
        if (region == null || region.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST,
                    "region 값이 비어있습니다."
            );
        }

        List<PopularSearch> results = popularSearchRepository.findTop10ByRegionOrderByCountDesc(region);

        if (results.isEmpty()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.NOT_FOUND,
                    "DB에서 해당 region 데이터 없음"
            );
        }

        return results;
    }
}
