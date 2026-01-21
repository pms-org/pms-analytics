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

@Service
@RequiredArgsConstructor
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

        List<String> symbols = analysisDao.findAllSymbols();
        if (symbols.isEmpty()) return;

        symbols.forEach(symbol ->
            priceClient.fetchPriceAsync(symbol)
                    .subscribe(price -> priceCache.updatePrice(symbol, price))
        );

        unrealizedPnl.computeUnRealisedPnlAndBroadcast();

        riskMetrics.computeRiskMetricsForAllPortfolios();

    }
}
