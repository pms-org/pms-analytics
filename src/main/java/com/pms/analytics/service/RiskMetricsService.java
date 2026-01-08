package com.pms.analytics.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.PortfolioRiskStatusDao;
import com.pms.analytics.dao.PortfolioValueHistoryDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.AnalysisOutbox;
import com.pms.analytics.dao.entity.PortfolioValueHistoryEntity;
import com.pms.analytics.dto.RiskEventDto;
import com.pms.analytics.dto.RiskEventOuterClass;
import com.pms.analytics.externalRedis.RedisPriceCache;
import com.pms.analytics.mapper.RiskEventMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskMetricsService {

    private final PortfolioValueHistoryDao historyDao;
    private final RedisPriceCache priceCache;
    private final AnalysisDao analysisDao;
    private final PortfolioRiskStatusDao portfolioRiskStatusDao;

    private static final int SCALE = 8;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    @Transactional
    public void computeRiskForSinglePortfolio(UUID portfolioId, List<AnalysisOutbox> batchedOutboxEntries) {
        
        if (portfolioRiskStatusDao.computedRecently(portfolioId)) {
            log.info("Risk has been recently computed for this portfolio {}.", portfolioId);
            return;
        }

        if (!portfolioRiskStatusDao.tryAdvisoryLock(portfolioId)) {
            log.info("Risk for this portfolio: {} is been calculating by other instance.", portfolioId);
            return;
        }

        computeRiskEvent(portfolioId, batchedOutboxEntries);
        log.info("Risk has been caculated for this portfolio: {}.", portfolioId);

        portfolioRiskStatusDao.updateLastComputed(portfolioId);
        log.info("Updated last computed for the portfolio: {} by risk metrics calculator.", portfolioId);
    }

    public void computeRiskEvent(UUID portfolioId, List<AnalysisOutbox> batchedOutboxEntries) {

        List<PortfolioValueHistoryEntity> last29Days
                = historyDao.findTop29ByPortfolioIdOrderByDateDesc(portfolioId);

        // Must have 29 historical entries
        if (last29Days.size() < 29) {
            System.out.println("Cannot compute risk - it needs atleast 29 days of history");
            return;
        }

        List<AnalysisEntity> positions
                = analysisDao.findByIdPortfolioId(portfolioId);

        if (positions.isEmpty()) {
            return;
        }

        BigDecimal todayValue = positions.stream()
                .map(p -> {
                    BigDecimal price = priceCache.getPrice(p.getId().getSymbol());
                    if (price == null) {
                        price = BigDecimal.ZERO;
                    }
                    return price.multiply(BigDecimal.valueOf(p.getHoldings()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Combine today, historical values
        List<BigDecimal> ascValues = new ArrayList<>();
        ascValues.add(todayValue);
        last29Days.stream()
                .map(PortfolioValueHistoryEntity::getPortfolioValue)
                .forEach(ascValues::add);

        List<BigDecimal> values = ascValues.reversed();

        // List<BigDecimal> values = new ArrayList<>();
        // // Sort historical values oldest → newest, then map to portfolioValue
        // last29Days.stream()
        // .sorted(Comparator.comparing(PortfolioValueHistoryEntity::getDate)) // sort entities by date
        // .map(PortfolioValueHistoryEntity::getPortfolioValue)               // map to BigDecimal
        // .forEach(values::add);
        // // Add today at the end
        // values.add(todayValue);
        if (values.size() < 30) {
            return;
        }

        // Compute daily returns
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal sumNegativeSquared = BigDecimal.ZERO;
        int negativeCount = 0;

        for (int i = 0; i < values.size() - 1; i++) {
            BigDecimal today = values.get(i + 1);
            BigDecimal yesterday = values.get(i);

            // Daily return = (today - yesterday) / yesterday
            BigDecimal dailyReturn = today.subtract(yesterday)
                    .divide(yesterday, SCALE, RoundingMode.HALF_UP);

            sum = sum.add(dailyReturn);

            // store only negative returns for sortino
            if (dailyReturn.compareTo(BigDecimal.ZERO) < 0) {
                sumNegativeSquared = sumNegativeSquared.add(dailyReturn.pow(2, MC));
                negativeCount++;
            }
        }

        // Average Daily Return
        BigDecimal avgDailyReturn = sum.divide(
                BigDecimal.valueOf(values.size() - 1),
                SCALE,
                RoundingMode.HALF_UP
        );

        // Standard deviation (Sharpe denominator)
        BigDecimal variance = BigDecimal.ZERO;
        for (int i = 0; i < values.size() - 1; i++) {
            BigDecimal today = values.get(i + 1);
            BigDecimal yesterday = values.get(i);
            BigDecimal dailyReturn = today.subtract(yesterday)
                    .divide(yesterday, SCALE, RoundingMode.HALF_UP);

            variance = variance.add(
                    (dailyReturn.subtract(avgDailyReturn)).pow(2, MC)
            );
        }

        BigDecimal stdDev = variance
                .divide(BigDecimal.valueOf(values.size() - 2), MC)
                .sqrt(MC);

        // Downside deviation (Sortino denominator)
        BigDecimal downsideDev = (negativeCount > 0)
                ? sumNegativeSquared
                        .divide(BigDecimal.valueOf(negativeCount), MC)
                        .sqrt(MC)
                : BigDecimal.ZERO;

        RiskEventDto event = new RiskEventDto(
                portfolioId,
                avgDailyReturn.floatValue(), // average rate of return
                stdDev.compareTo(BigDecimal.ZERO) > 0
                ? avgDailyReturn.divide(stdDev, MC).floatValue()
                : 0f, // Sharpe Ratio
                downsideDev.compareTo(BigDecimal.ZERO) > 0
                ? avgDailyReturn.divide(downsideDev, MC).floatValue()
                : 0f // Sortino Ratio
        );

        System.out.println(event);

        RiskEventOuterClass.RiskEvent proto = RiskEventMapper.toProto(event);

        AnalysisOutbox outbox = new AnalysisOutbox();
        outbox.setPortfolioId(portfolioId);
        outbox.setPayload(proto.toByteArray());
        outbox.setStatus("PENDING");

        batchedOutboxEntries.add(outbox);
        System.out.println("Risk computed - stored in outbox");
    }
}
