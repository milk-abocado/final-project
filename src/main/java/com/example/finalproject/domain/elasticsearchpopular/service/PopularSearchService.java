package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchErrorCode;
import com.example.finalproject.domain.elasticsearchpopular.exception.PopularSearchException;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import com.example.finalproject.domain.searches.entity.Searches;
import com.example.finalproject.domain.searches.exception.SearchesErrorCode;
import com.example.finalproject.domain.searches.exception.SearchesException;
import com.example.finalproject.domain.searches.repository.SearchesRepository;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PopularSearchService
 * 인기 검색어 처리 로직을 담당하는 서비스 클래스
 * <p>
 * - 검색 기록 저장 (Searches)
 * - 인기 검색어 저장 및 갱신 (PopularSearches, Redis)
 * - Elasticsearch 자동완성 및 Top-N 조회
 * - DB 기반 인기 검색어 조회
 */
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
     * 검색 기록을 저장하고 인기 검색어를 갱신하는 메소드
     * <p>
     * 동작 순서:
     * 1. 파라미터 유효성 검증 (keyword, region, userId)
     * 2. Searches 테이블에 사용자 검색 기록 저장/업데이트
     * 3. PopularSearches 테이블에 해당 검색어 카운트 갱신
     * 4. Redis 에도 검색 횟수 증가 반영
     * 5. 최종적으로 저장된 Searches 엔티티 정보를 DTO로 반환
     *
     * @param keyword 검색 키워드
     * @param region  검색 지역
     * @param userId  사용자 ID
     * @return 저장된 검색 기록에 대한 응답 DTO
     */
    @Transactional
    public SearchesResponseDto recordSearch(String keyword, String region, Long userId) {
        // keyword/region 누락 시 400
        if (keyword == null || keyword.isBlank() || region == null || region.isBlank()) {
            throw new SearchesException(SearchesErrorCode.BAD_REQUEST, "keyword/region이 누락되었습니다.");
        }
        // userId 누락 시 401
        if (userId == null) {
            throw new SearchesException(SearchesErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // userId가 존재하지 않는 경우 404
        if (!usersRepository.existsById(userId)) {
            throw new SearchesException(SearchesErrorCode.NOT_FOUND, "존재하지 않는 userId입니다.");
        }

        // 사용자별 동일 keyword + region 조합 검색 이력 조회
        List<Searches> searchesList =
                searchesRepository.findAllByUserIdAndKeywordAndRegion(userId, keyword, region);

        Searches searches;
        if (!searchesList.isEmpty()) {
            // 기존 검색 이력이 있으면 count 증가
            searches = searchesList.get(0);
            searches.setCount(searches.getCount() + 1);
        } else {
            // 검색 이력이 없으면 새로 생성
            searches = Searches.builder()
                    .keyword(keyword)
                    .region(region)
                    .userId(userId)
                    .count(1)
                    .build();
        }

        // 검색 로그 저장
        Searches saved = searchesRepository.save(searches);

        // 인기 검색어 저장/업데이트
        PopularSearches popular = popularSearchRepository
                .findByRegionAndKeyword(region, keyword)
                .orElseGet(() -> {
                    PopularSearches p = new PopularSearches();
                    p.setRegion(region);
                    p.setKeyword(keyword);
                    p.setSearchCount(0);              // ← null 방지
                    p.setRanking(0);                  // 별도 계산 전 기본값
                    p.setCreatedAt(LocalDateTime.now());
                    return p;
                });

        popular.setSearchCount(popular.getSearchCount() + 1);
        popular.setUpdatedAt(LocalDateTime.now());

        popularSearchRepository.save(popular);

        // Redis 카운트 증가
        String redisKey = "popular:" + region + ":" + keyword;
        redisTemplate.opsForValue().increment(redisKey, 1);

        // 응답 DTO 반환
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
     * Elasticsearch 기반 자동 완성 기능
     * - search_as_you_type 또는 matchPhrasePrefix 사용
     * - region + keyword 조합으로 검색
     * - 결과를 searchCount 내림차순 정렬
     *
     * @param keyword    검색 키워드
     * @param region     검색 지역
     * @param maxResults 최대 반환 개수
     * @return 자동완성 후보 리스트
     */
    public List<String> autoComplete(String keyword, String region, int maxResults) {
        validateParams(keyword, region);

        try {
            var resp = esClient.search(s -> s
                            .index(INDEX)
                            .size(Math.max(20, maxResults)) // 충분히 큰 수로 조회 후 상위 maxResults만 반환
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("region").value(region)))
                                    .must(m -> m.term(t -> t.field("type").value("redis")))
                                    .must(m -> m.matchPhrasePrefix(mp -> mp
                                            .field("keyword")
                                            .query(keyword)
                                    ))
                            ))
                            .sort(so -> so.field(f -> f.field("searchCount").order(SortOrder.Desc))),
                    Map.class
            );

            // 중복 제거 및 순서 유지
            Set<String> uniqueResults = resp.hits().hits().stream()
                    .map(hit -> Objects.requireNonNull(hit.source()).get("keyword").toString())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (uniqueResults.isEmpty()) {
                throw new PopularSearchException(PopularSearchErrorCode.NOT_FOUND, "자동완성 결과가 없습니다.");
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
        // keyword, region 유효성 검사 로직을 구현할 수 있는 자리
    }

    /**
     * Elasticsearch 기반 Top-N 인기 검색어 조회
     *
     * @param region 지역명
     * @param topN   가져올 개수
     * @return 인기 검색어 결과 리스트 (Map 구조)
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
                            .sort(so -> so.field(f -> f.field("searchCount").order(SortOrder.Desc))),
                    Map.class
            );

            List<Map<String, Object>> results = resp.hits().hits().stream()
                    .map(hit -> (Map<String, Object>) hit.source())
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                throw new PopularSearchException(PopularSearchErrorCode.NOT_FOUND, "해당 region에 대한 데이터가 존재하지 않습니다.");
            }

            return results;

        } catch (PopularSearchException e) {
            throw e;
        } catch (Exception e) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.ELASTIC_ERROR,
                    "Elasticsearch 조회 실패: " + e.getMessage()
            );
        }
    }

    private void validateRegion(String region) {
        // region 유효성 검사 로직을 구현할 수 있는 자리
    }

    /**
     * DB 기반 Top-N 인기 검색어 조회
     * - JPA Repository 이용
     * - region 값이 비어 있으면 예외 발생
     *
     * @param region 지역명
     * @param topN   가져올 개수
     * @return 인기 검색어 리스트
     */
    public List<PopularSearches> getTopFromDB(String region, int topN) {
        if (region == null || region.isBlank()) {
            throw new PopularSearchException(
                    PopularSearchErrorCode.BAD_REQUEST, "region 값이 비어 있습니다."
            );
        }

        Pageable pageable = PageRequest.of(0, topN);
        List<PopularSearches> results = popularSearchRepository.findTopByRegion(region, pageable);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        return results;
    }
}
