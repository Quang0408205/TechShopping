package com.example.Tech.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** HTTP clients for the AI service: a short timeout for recommendations, a long one for the chatbot. */
@Configuration
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiConfig {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    @Bean
    @Qualifier("aiRestClient")
    public RestClient aiRestClient(AiServiceProperties properties) {
        return client(properties, properties.timeout());
    }

    @Bean
    @Qualifier("aiChatRestClient")
    public RestClient aiChatRestClient(AiServiceProperties properties) {
        return client(properties, properties.chatTimeout());
    }

    private static RestClient client(AiServiceProperties properties, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(INTERNAL_KEY_HEADER, properties.internalKey())
                .requestFactory(factory)
                .build();
    }
}
