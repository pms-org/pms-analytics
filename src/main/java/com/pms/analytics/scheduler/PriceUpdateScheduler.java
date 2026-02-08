package com.pms.analytics.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.externalRedis.ExternalPriceClient;
import com.pms.analytics.externalRedis.RedisPriceCache;
import com.pms.analytics.service.RiskMetricsCalculator;
import com.pms.analytics.service.UnrealizedPnlCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateScheduler {

    @Autowired
    ExternalPriceClient priceClient;

    @Autowired
    RedisPriceCache priceCache;

    @Autowired
    AnalysisDao analysisDao;

    @Autowired
    UnrealizedPnlCalculator unrealizedPnl;

    @Autowired
    RiskMetricsCalculator riskMetrics;

    // @Scheduled(fixedDelay = 30000)
    @Scheduled(fixedDelayString = "${scheduler.price-refresh.delay-ms}")
    public void refreshPrices() {
        
        log.info("[Scheduler] Starting price refresh from Finnhub...");

        List<String> symbols = analysisDao.findAllSymbols();
        log.info("[Scheduler] Found {} symbols to update prices for", symbols.size());
        
        if (symbols.isEmpty()) {
            log.warn("[Scheduler] No symbols found, skipping price refresh");
            return;
        }

        symbols.forEach(symbol -> {
            log.debug("[Scheduler] Fetching price for symbol: {}", symbol);
            priceClient.fetchPriceAsync(symbol)
                    .doOnSuccess(price -> log.debug("[Scheduler] Updated price for {}: {}", symbol, price))
                    .doOnError(error -> log.error("[Scheduler] Error fetching price for {}: {}", symbol, error.getMessage()))
                    .subscribe(price -> priceCache.updatePrice(symbol, price));
        });

        log.info("[Scheduler] Triggering unrealized PnL calculation...");
        unrealizedPnl.computeUnRealisedPnlAndBroadcast();

        log.info("[Scheduler] Triggering risk metrics calculation...");
        riskMetrics.computeRiskMetricsForAllPortfolios();
        
        log.info("[Scheduler] Price refresh cycle completed");

    }
}
