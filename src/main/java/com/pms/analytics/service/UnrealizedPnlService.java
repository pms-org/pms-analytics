package com.pms.analytics.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.PortfolioUnrealizedPnlStatusDao;
import com.pms.analytics.dao.TransactionsDao;
import com.pms.analytics.dao.entity.TransactionsEntity;
import com.pms.analytics.dto.UnrealizedPnlDto;
import com.pms.analytics.externalRedis.ExternalPriceClient;
import com.pms.analytics.externalRedis.RedisPriceCache;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnrealizedPnlService {

    private final RedisPriceCache priceCache;
    private final ExternalPriceClient externalPriceClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final PortfolioUnrealizedPnlStatusDao portfolioUnrealizedPnlStatusDao;
    private final TransactionsDao transactionsDao;

    @Value("${websocket.topics.unrealized-pnl}")
    private String unrealizedPnlTopic;

    private final Map<String, BigDecimal> lastKnownPrices = new ConcurrentHashMap<>();

    @Transactional
    public void computeUnrealizedPnlForSinglePortfolio(UUID portfolioId) {

        if (portfolioUnrealizedPnlStatusDao.computedRecently(portfolioId)) {
            log.info("Unrealized price has been recently computed for this portfolio {}.", portfolioId);
            return;
        }

        if (!portfolioUnrealizedPnlStatusDao.tryAdvisoryLock(portfolioId)) {
            log.info("Unrealized price for this portfolio: {} is been calculating by other instance.", portfolioId);
            return;
        }

        log.info("Acquired advisory lock for the portfolio: {} for calculating unrealized pnl.", portfolioId);

        UnrealizedPnlDto payload = computeUnrealizedPnl(portfolioId);

        portfolioUnrealizedPnlStatusDao.updateLastComputed(portfolioId);
        log.info("Updated last computed for the portfolio: {} by unrealized pnl calculator.", portfolioId);

        // log.info("Calculated unrealized pnl {}.",payload);

        if (payload == null) {
            log.info("Unrealized pnl payload is null for portfolio: {}.",portfolioId);
            return;
        }

        try {
            messagingTemplate.convertAndSend(unrealizedPnlTopic, payload);
            log.info("New unrealized p&l sent to web socket.", payload);
        } catch (Exception e) {
            System.err.println("Failed to send unrealized PnL: " + e.getMessage());
        }

    }

    public UnrealizedPnlDto computeUnrealizedPnl(UUID portfolioId) {
        // fetch open transactions for portfolio
        List<TransactionsEntity> openTxns
                = transactionsDao.findOpenPositionsByPortfolioId(portfolioId);

        log.info("Fetched {} open positions from transaction.", openTxns.size());

        if (openTxns.isEmpty()) {
            return null;
        }

        Map<String, BigDecimal> symbolUnrealized = new HashMap<>();
        BigDecimal totalUnrealized = BigDecimal.ZERO;

        for (TransactionsEntity txn : openTxns) {
            try {
                String symbol = txn.getTrade().getSymbol();
                Long remainingQty = txn.getQuantity();
                BigDecimal buyPrice = txn.getBuyPrice();

                if (remainingQty <= 0) {
                    log.info("Tnx quantity is less than zero,so skipping the tnx.");
                    continue;
                }

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
                        log.error("Cannot fetch price from external client");
                    }
                }

                if (currentPrice == null) {
                    continue;
                }

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

        return payload;

    }
}
