package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

    private final ElasticsearchClient esClient;

    public List<String> getTop10ByRegion(String region) throws IOException {
        SearchResponse<PopularSearch> response = esClient.search(s -> s
                        .index("popular_searches_index")
                        .query(q -> q
                                .term(t -> t
                                        .field("region")
                                        .value(region)
                                )
                        )
                        .size(10)
                        .sort(so -> so.field(f -> f.field("count").order(SortOrder.Desc)))
                , PopularSearch.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .map(PopularSearch::getKeyword)
                .collect(Collectors.toList());
    }

    public List<String> suggestKeywords(String region, String q) {
        return List.of();
    }

    public List<String> autoComplete(String keyword, String region) {
        return List.of();
    }

    public List<PopularSearch> getPopularKeywords(String region) {
        return List.of();
    }
}


