package com.example.finalproject.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.jsonwebtoken.io.IOException;
import jakarta.annotation.PostConstruct;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ElasticSearchConfig {
    @PostConstruct
    public void createIndexIfNotExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest("searches_index");
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);

        if (!exists) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("searches_index");
            createIndexRequest.mapping(
                    "{\n" +
                            "  \"properties\": {\n" +
                            "    \"keyword\": { \"type\": \"keyword\" },\n" +
                            "    \"region\": { \"type\": \"keyword\" },\n" +
                            "    \"user_id\": { \"type\": \"long\" },\n" +
                            "    \"created_at\": { \"type\": \"date\" }\n" +
                            "  }\n" +
                            "}",
                    XContentType.JSON
            );
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        }
    }

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
}
