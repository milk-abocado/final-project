package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import com.example.finalproject.domain.elasticsearchpopular.entity.PopularSearch;
import com.example.finalproject.domain.elasticsearchpopular.repository.PopularSearchRepository;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularSearchService {
    private final RestHighLevelClient client;
    private final PopularSearchRepository repository;

    public void aggregationPopularSearches() throws IOException {
        SearchRequest searchRequest = new SearchRequest("searches_index");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        sourceBuilder.query(QueryBuilders.rangeQuery("created_at")
                .gte("now-1h")
                .lte("now"));

        //aggregation
        TermsAggregationBuilder aggregation = AggregationBuilders
                .terms("popular_keywords")
                .field("keyword")
                .size(10);

        sourceBuilder.aggregation(aggregation);
        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms terms = response.getAggregations().get("popular_keywords");

        List<PopularSearch> popularSearches = new ArrayList<>();
        int rank = 1;
        for (Terms.Bucket bucket : terms.getBuckets()) {
            PopularSearch popularSearch = new PopularSearch();
            popularSearch.setKeyword(bucket.getKeyAsString());
            popularSearch.setCount((int) bucket.getDocCount());
            popularSearch.setRank(rank++);
            popularSearch.add(popularSearch);
        }

        repository.deleteAll(); //기존 캐싱 삭제
        repository.saveAll(popularSearches);
    }
}
