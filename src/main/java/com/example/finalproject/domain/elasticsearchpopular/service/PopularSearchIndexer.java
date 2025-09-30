package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.finalproject.domain.elasticsearchpopular.dto.PopularSearchDoc;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearches;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PopularSearchIndexer {

    private final PopularSearchRepository repository;
    private final ElasticsearchClient esClient;

    @Scheduled(cron = "0 0 * * * *") // 매시간 정각 실행
    public void indexPopularSearches() throws IOException, java.io.IOException {
        String indexName = "popular_searches_index";

        // 1. 모든 지역 조회 (DB에 있는 distinct 지역)
        List<String> regions = repository.findAll()
                .stream()
                .map(PopularSearches::getRegion)
                .distinct()
                .toList();

        // 2. 지역별 Top10 가져와서 Elasticsearch에 색인
        for (String region : regions) {
            List<PopularSearches> topSearches = repository.findTopByRegion(region, PageRequest.of(0, 10));

            for (PopularSearches search : topSearches) {
                PopularSearchDoc doc = PopularSearchDoc.builder()
                        .keyword(search.getKeyword())
                        .region(search.getRegion())
                        .searchCount(search.getSearchCount())
                        .createdAt(search.getCreatedAt())
                        .build();

                String keyword = "";
                esClient.index(i -> i
                        .index(indexName)
                        .id(region + "_" + search.getId()) // 지역 단위 고유 ID
                        .document(Map.of(
                                "region", search.getRegion(),
                                "keyword", search.getKeyword(),
                                "search_count", search.getSearchCount(),
                                "created_at", search.getCreatedAt(),
                                "type", "db" //DB -> ES 인덱싱, 문서 생성
                        ))
                );
            }
        }
    }
}
