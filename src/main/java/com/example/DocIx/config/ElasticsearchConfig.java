package com.example.DocIx.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUrl;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Parse URL
        String host = "localhost";
        int port = 9200;

        if (elasticsearchUrl.startsWith("http://")) {
            String[] parts = elasticsearchUrl.replace("http://", "").split(":");
            host = parts[0];
            if (parts.length > 1) {
                port = Integer.parseInt(parts[1]);
            }
        }

        // Create the low-level client
        RestClient restClient = RestClient.builder(
            new HttpHost(host, port, "http")
        ).build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper()
        );

        // And create the API client
        return new ElasticsearchClient(transport);
    }
}
