package com.example.finalproject.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    // Cloud ID, Username, Password는 Elastic Cloud 콘솔에서 확인
    private final String CLOUD_ID = "qoduswn9810@gmail.com";
    private final String USERNAME = "elastic";
    private final String PASSWORD = "2LqZGkNwGcoKHTJUCK5He2ym";

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(USERNAME, PASSWORD)
        );

        RestClient restClient = RestClient.builder(
                HttpHost.create("https://" + CLOUD_ID)
        )
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credsProvider)
                ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
}
