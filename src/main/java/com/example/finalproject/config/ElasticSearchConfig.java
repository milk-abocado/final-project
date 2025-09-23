package com.example.finalproject.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.jsonwebtoken.io.IOException;
import jakarta.annotation.PostConstruct;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    @PostConstruct
    public void createPopularSearchIndex() throws IOException, java.io.IOException {
        ElasticsearchClient esClient = null;
        boolean exists = esClient.indices().exists(e -> e.index("popular_searches")).value();
        if (!exists) {
            esClient.indices().create(c -> c
                    .index("popular_searches")
                    .mappings(m -> m
                            .properties("keyword", p -> p.completion(cmp -> cmp))
                            .properties("region", p -> p.keyword(k -> k))
                            .properties("count", p -> p.integer(i -> i))
                            .properties("rank", p -> p.integer(i -> i))
                    )
            );
        }
    }

}
