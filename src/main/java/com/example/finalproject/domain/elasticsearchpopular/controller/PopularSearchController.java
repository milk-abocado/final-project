package com.example.finalproject.domain.elasticsearchpopular.controller;

import com.example.finalproject.domain.elasticsearchpopular.dto.PopularSearchesResponse;
import com.example.finalproject.domain.elasticsearchpopular.dto.SearchRecordRequest;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchService;
import com.example.finalproject.domain.elasticsearchpopular.service.PopularSearchSyncService;
import com.example.finalproject.domain.searches.dto.SearchesResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * PopularSearchController
 * 인기 검색어 및 검색 기록과 관련된 API 엔드포인트를 제공하는 컨트롤러 클래스
 * <p>
 * - DB 기반 인기 검색어 조회
 * - 인기 검색어 동기화
 * - 검색 기록 저장 및 인기 검색어 반영
 * - 자동완성 기능
 */
@RestController
@RequestMapping("/api/popular-searches")
@RequiredArgsConstructor
@Validated
public class PopularSearchController {

    private final PopularSearchService popularSearchService;
    /**
     * -- GETTER --
     *  테스트용 getter
     *  PopularSearchSyncService Bean 접근용
     */
    @Getter
    private final PopularSearchSyncService popularSearchSyncService;

    /**
     * DB Top-N 인기 검색어 조회
     * 특정 지역(region)에서 상위 N개의 인기 검색어를 DB 에서 가져옴
     *
     * @param region 지역명 (필수)
     * @param topN   상위 검색어 개수 (기본값: 10, 최소값: 1)
     * @return 인기 검색어 리스트
     */
    @GetMapping("/popular/db")
    public List<PopularSearchesResponse> getTopFromDB(
            @RequestParam String region,
            @RequestParam(defaultValue = "10") int topN) {
        return popularSearchService.getTopFromDB(region, topN)
                .stream()
                .sorted(Comparator.comparingInt(PopularSearches::getRanking)) // 랭킹순 정렬
                .map(p -> new PopularSearchesResponse(
                        p.getId(),
                        p.getKeyword(),
                        p.getRegion(),
                        p.getRanking(),
                        p.getSearchCount()
                ))
                .toList();
    }

    /**
     * 수동 동기화
     * 외부 소스(Elasticsearch 등)에서 DB로 인기 검색어 동기화 수행
     *
     * @return 동기화 상태 메시지
     */
    @PostMapping("/sync")
    public Map<String, String> manualSync() throws Exception {
        popularSearchSyncService.syncPopularSearches();
        return Map.of("status", "synced");
    }

    /**
     * 검색 수행 (로그 + 인기 반영)
     * - 사용자가 검색한 키워드를 기록
     * - PopularSearches(인기 검색어 테이블) 갱신
     * - Searches(개별 검색 로그) 저장
     *
     * @param req 검색 요청 DTO (keyword, region, userId 포함)
     * @return 검색 결과 DTO (검색 키워드 관련 응답)
     */
    @PostMapping("/searches")
    public SearchesResponseDto search(@Valid @RequestBody SearchRecordRequest req) {
        // PopularSearchService.recordSearch(keyword, region, userId) 호출이 핵심
        return popularSearchService.recordSearch(req.getKeyword(), req.getRegion(), req.getUserId());
    }

    /**
     * 기본 Top 조회
     * /api/popular-searches 요청 시 DB 기반 Top-N 인기 검색어 제공
     *
     * @param region 지역명
     * @param topN   상위 검색어 개수
     * @return 인기 검색어 리스트
     */
    @GetMapping
    public List<PopularSearchesResponse> getTopByRegion(
            @RequestParam @NotBlank String region,
            @RequestParam(defaultValue = "10") @Min(1) int topN
    ) {
        return popularSearchService.getTopFromDB(region, topN).stream()
                .sorted(Comparator.comparingInt(PopularSearches::getRanking)) // 랭킹 오름차순
                .map(p -> new PopularSearchesResponse(
                        p.getId(),
                        p.getKeyword(),
                        p.getRegion(),
                        p.getRanking(),
                        p.getSearchCount()
                ))
                .toList();
    }

    /**
     * 자동 완성 기능
     * 사용자가 입력한 keyword + region을 기반으로 최대 10개의 자동 완성 후보 제공
     *
     * @param keyword 검색 키워드 (필수)
     * @param region  지역명 (필수)
     * @return 자동완성 추천어 리스트
     */
    @GetMapping("/autocomplete")
    public List<String> autoComplete(
            @RequestParam @NotBlank String keyword,
            @RequestParam @NotBlank String region
    ) {
        int maxResults = 10;
        return popularSearchService.autoComplete(keyword, region, maxResults);
    }
}