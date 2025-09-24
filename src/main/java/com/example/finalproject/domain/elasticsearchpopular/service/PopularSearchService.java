package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.finalproject.domain.elasticsearchpopular.dto.PopularSearchDoc;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

    private final ElasticsearchClient esClient;

    public List<String> suggestKeywords(String region, String input) throws IOException, java.io.IOException {
        String indexName = "popular_searches_index";

        SearchResponse<PopularSearchDoc> response = esClient.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .must(m -> m.term(t -> t.field("region").value(region))) // 지역 필터
                                        .must(m -> m.multiMatch(mm -> mm
                                                .query(input)
                                                .type(TextQueryType.BoolPrefix)
                                                .fields("keyword", "keyword._2gram", "keyword._3gram")
                                        ))
                                )
                        )
                        .size(10), // 최대 10개
                PopularSearchDoc.class
        );

        return response.hits().hits().stream()
                .map(hit -> hit.source().getKeyword())
                .distinct()
                .toList();
    }

    public List<String> autoComplete(String keyword, String region) {
        return List.of();
    }

    public List<PopularSearch> getPopularKeywords(String region) {
        return List.of();
    }
}

