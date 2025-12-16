package com.pms.analytics.externalRedis;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pms.analytics.dto.FinnhubQuoteResponseDTO;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ExternalPriceClient {

    private final WebClient finnhubClient;

    @Value("${finnhub.api.key}")
    private String externalApiKey;

    public Mono<BigDecimal> fetchPriceAsync(String symbol) {
        return finnhubClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", externalApiKey)
                        .build()
                )
                .retrieve()
                .bodyToMono(FinnhubQuoteResponseDTO.class)
                .map(FinnhubQuoteResponseDTO::getCost);
    }
}
