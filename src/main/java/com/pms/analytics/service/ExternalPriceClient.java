package com.pms.analytics.service;

import com.pms.analytics.dto.FinnhubQuoteResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExternalPriceClient {

    private final WebClient finnhubClient;

    @Value("${finnhub.api.key}")
    private  String externalApiKey;

    public BigDecimal getCurrentPrice(String symbol) {


        FinnhubQuoteResponseDTO quote = finnhubClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", externalApiKey)
                        .build()
                )
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Finnhub error: " + body)))
                .bodyToMono(FinnhubQuoteResponseDTO.class)
                .block();

        if (quote == null || quote.getCost() == null) {
            throw new RuntimeException("Price not available for symbol: " + symbol);
        }

        return quote.getCost();
    }
}
