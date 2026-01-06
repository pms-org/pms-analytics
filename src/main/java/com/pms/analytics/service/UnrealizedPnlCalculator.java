package com.pms.analytics.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.TransactionsDao;
import com.pms.analytics.dao.entity.TransactionsEntity;
import com.pms.analytics.dto.UnrealizedPnlDto;
import com.pms.analytics.externalRedis.ExternalPriceClient;
import com.pms.analytics.externalRedis.RedisPriceCache;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UnrealizedPnlCalculator {

    private final TransactionsDao transactionsDao;
    private final RedisPriceCache priceCache;
    private final ExternalPriceClient externalPriceClient;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, BigDecimal> lastKnownPrices = new ConcurrentHashMap<>();

    public void computeUnRealisedPnlAndBroadcast() {

        try {
            List<UUID> portfolioIds =
                    transactionsDao.findDistinctPortfolioIdsWithOpenPositions();

            for (UUID portfolioId : portfolioIds) {

                // fetch open transactions for portfolio
                List<TransactionsEntity> openTxns =
                        transactionsDao.findOpenPositionsByPortfolioId(portfolioId);

                if (openTxns.isEmpty()) continue;

                Map<String, BigDecimal> symbolUnrealized = new HashMap<>();
                BigDecimal totalUnrealized = BigDecimal.ZERO;

                for (TransactionsEntity txn : openTxns) {
                    try {
                        String symbol = txn.getTrade().getSymbol();
                        Long remainingQty = txn.getQuantity();
                        BigDecimal buyPrice = txn.getBuyPrice();

                        if (remainingQty <= 0 || buyPrice == null) continue;

                        // get current price
                        BigDecimal currentPrice = priceCache.getPrice(symbol);

                        if (currentPrice == null) {
                            currentPrice = lastKnownPrices.get(symbol);
                        }

                        if (currentPrice == null) {
                            try {
                                Mono<BigDecimal> monoPrice = externalPriceClient.fetchPriceAsync(symbol);
                                currentPrice = monoPrice.block(Duration.ofSeconds(3));
                            } catch (Exception ignored) {
                                
                            }
                        }

                        if (currentPrice == null) continue;

                        lastKnownPrices.put(symbol, currentPrice);

                        // Unrealized PnL
                        BigDecimal unrealized = currentPrice.subtract(buyPrice)
                                .multiply(BigDecimal.valueOf(remainingQty));

                        // Add to per-symbol
                        symbolUnrealized.put(symbol,
                                symbolUnrealized.getOrDefault(symbol, BigDecimal.ZERO).add(unrealized));

                        // Add to total
                        totalUnrealized = totalUnrealized.add(unrealized);

                    } catch (Exception e) {
                        System.err.println("Error computing unrealized PnL: " + e.getMessage());
                    }
                }

                // Create payload
                UnrealizedPnlDto payload = new UnrealizedPnlDto(
                        symbolUnrealized,
                        totalUnrealized,
                        portfolioId.toString()
                );

                try {
                    messagingTemplate.convertAndSend("/topic/unrealized-pnl", payload);
                } catch (Exception e) {
                    System.err.println("Failed to send unrealized PnL: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Scheduler failed: " + e.getMessage());
        }
    }
}
