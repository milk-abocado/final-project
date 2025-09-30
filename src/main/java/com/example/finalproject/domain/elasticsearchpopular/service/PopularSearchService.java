package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.entity.Searches;
import com.example.finalproject.domain.searches.exception.SearchesErrorCode;
import com.example.finalproject.domain.searches.exception.SearchesException;
import com.example.finalproject.domain.searches.repository.SearchesRepository;
import com.example.finalproject.domain.users.repository.UsersRepository;
import org.springframework.data.domain.PageRequest;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchErrorCode;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchException;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

    private final ElasticsearchClient esClient;
    private final StringRedisTemplate redisTemplate;
    private final UsersRepository usersRepository;
    private final SearchesRepository searchesRepository;
    private final PopularSearchRepository popularSearchRepository;
    private static final String INDEX = "popular_searches_index";

    /**
     * 인기 검색어 Redis 저장 (DB 기록 포함)
     */
    public SearchesResponseDto recordSearch(String keyword, String region, Long userId) {
        // 400: keyword/region 누락
        if (keyword == null || keyword.isBlank() || region == null || region.isBlank()) {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "keyword/region 누락");
        }
        // 401: 로그인 검증
        if (userId == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인 필요");
        }
        // 404: 존재하지 않는 userId
        if (!usersRepository.existsById(userId)) {
            throw new SearchesException(SearchesErrorCode.NOT_FOUND, "존재하지 않는 userId");
        }

        // DB 조회: 동일 조합 여러 개일 수 있으므로 List로 받음
        List<Searches> searchesList = searchesRepository.findAllByUserIdAndKeywordAndRegion(userId, keyword, region);

        Searches searches;
        if (!searchesList.isEmpty()) {
            // 첫 번째만 사용하고 count 증가
            searches = searchesList.get(0);
            searches.setCount(searches.getCount() + 1);
        } else {
            // 새로 생성
            searches = Searches.builder()
                    .keyword(keyword)
                    .region(region)
                    .userId(userId)
                    .count(1)
                    .build();
        }

        Searches saved = searchesRepository.save(searches);

        // Redis 증가
        String redisKey = "popular:" + region + ":" + keyword;
        redisTemplate.opsForValue().increment(redisKey, 1);

        // 결과 반환
        return SearchesResponseDto.builder()
                .id(saved.getId())
                .keyword(saved.getKeyword())
                .region(saved.getRegion())
                .userId(saved.getUserId())
                .updatedAt(saved.getUpdatedAt())
                .count(saved.getCount())
                .build();
    }

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
                            .sort(so -> so.field(f -> f.field("search_count").order(SortOrder.Desc))),
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
                            .sort(so -> so.field(f -> f.field("search_count").order(SortOrder.Desc))),
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
    public List<PopularSearches> getTopFromDB(String region, int topN) {
        if  (region == null || region.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST, "region 값이 비어있습니다."
            );
        }

        List<PopularSearches> results = popularSearchRepository
                .findTopNByRegionOrderByCountDesc(region, PageRequest.of(0, topN));

        if (results.isEmpty()) {
            throw new PopularSearchException(PopularSearchErrorCode.NOT_FOUND, "DB에서 해당 region 데이터 없음");
        }

        return results;
    }
}
