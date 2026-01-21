package com.pms.analytics.scheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.PortfolioValueHistoryDao;
import com.pms.analytics.dao.PortfolioValueStatusDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.PortfolioValueHistoryEntity;
import com.pms.analytics.externalRedis.RedisPriceCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioValueScheduler {

    private final AnalysisDao analysisDao;
    private final RedisPriceCache priceCache;
    private final PortfolioValueHistoryDao historyDao;
    private final PortfolioValueStatusDao portfolioValueStatusDao;
    
    // @Transactional
    // @Scheduled(cron = "0 59 23 * * ?", zone = "Asia/Kolkata")
    // public void calculatePortfolioValue() {

    @Transactional
    @Scheduled(cron = "${scheduler.portfolio-value.cron}", zone = "${scheduler.portfolio-value.timezone}")
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

                if(portfolioValueStatusDao.computedRecently(portfolioId))
                {
                    log.info("Portfolio value for this portfolio {} have been calculated within 23 hours.",portfolioId);
                    return;
                }

                if(!portfolioValueStatusDao.tryAdvisoryLock(portfolioId))
                {
                    log.info("Portfolio value for this portfolio {} is been calculating by another instance.",portfolioId);
                    return;
                }

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

                portfolioValueStatusDao.updateLastComputed(portfolioId);
            });
    }
}
