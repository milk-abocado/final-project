package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElasticsearchInitializer implements CommandLineRunner {

    /**
     * search_as_you_type 매핑: 애플리케이션 실행 시 자동 매핑 가능(인덱스)
     */
    private final ElasticsearchClient esClient;

    @Override
    public void run(String... args) throws Exception {
        String indexName = "popular_searches_index";

        boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
        if (!exists) {
            esClient.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("keyword", p -> p.searchAsYouType(s -> s))
                            .properties("region", p -> p.keyword(k -> k))
                            .properties("count", p -> p.integer(i -> i))
                            .properties("rank", p -> p.integer(i -> i))
                            .properties("created_at", p -> p.date(d -> d))
                    )
            );
        }
    }
}
