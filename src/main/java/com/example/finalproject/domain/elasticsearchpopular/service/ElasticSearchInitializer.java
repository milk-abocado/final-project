package com.example.finalproject.domain.elasticsearchpopular.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Component
public class ElasticSearchInitializer {

    private final ElasticsearchClient esClient;
    private static final String INDEX = "popular_searches_index";

    public ElasticSearchInitializer(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @PostConstruct
    public void createIndexIfNotExists() throws IOException {
        boolean exists = esClient.indices().exists(e -> e.index(INDEX)).value();

        if (!exists) {
            esClient.indices().create(c -> c
                    .index(INDEX)
                    .mappings(m -> m
                            .properties("keyword", p -> p.searchAsYouType(s -> s))
                            .properties("region", p -> p.keyword(k -> k))
                            .properties("search_count", p -> p.integer(i -> i))
                            .properties("ranking", p -> p.integer(i -> i))
                            .properties("created_at", p -> p.date(d -> d))
                            .properties("type", p -> p.keyword(k -> k)) //type 필드 생성
                    )
            );
            System.out.println("ElasticSearch index '" + INDEX + "' created with search_as_you_type mapping.");
        } else {
            System.out.println("ElasticSearch index '" + INDEX + "' already exists.");
        }
    }
}
