package com.pms.analytics.scheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.PortfolioValueHistoryDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.PortfolioValueHistoryEntity;
import com.pms.analytics.externalRedis.RedisPriceCache;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioValueScheduler {

    private final AnalysisDao analysisDao;
    private final RedisPriceCache priceCache;
    private final PortfolioValueHistoryDao historyDao;

    @Scheduled(cron = "0 20 12 * * ?")
    public void calculatePortfolioValue() {

        List<AnalysisEntity> positions = analysisDao.findAll();

        if (positions.isEmpty()) return;

        // Fetch all live prices from Redis once
        Map<String, BigDecimal> priceMap = priceCache.getAllPrices();

        // Get unique portfolio IDs
        positions.stream()
            .map(p -> p.getId().getPortfolioId())
            .distinct()
            .forEach(portfolioId -> {

                BigDecimal portfolioValue = positions.stream()
                        .filter(p -> p.getId().getPortfolioId().equals(portfolioId))
                        .map(p -> {
                            String symbol = p.getId().getSymbol();
                            BigDecimal price = priceMap.getOrDefault(symbol, BigDecimal.ZERO);
                            return price.multiply(BigDecimal.valueOf(p.getHoldings()));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                PortfolioValueHistoryEntity history = new PortfolioValueHistoryEntity();
                history.setPortfolioId(portfolioId);
                history.setDate(LocalDate.now());
                history.setPortfolioValue(portfolioValue);

                historyDao.save(history);
            });
    }
}
