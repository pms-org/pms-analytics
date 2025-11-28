package com.pms.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient finnhubClient() {
        return WebClient.builder()
                .baseUrl("https://finnhub.io/api/v1")
                .build();
    }
}

