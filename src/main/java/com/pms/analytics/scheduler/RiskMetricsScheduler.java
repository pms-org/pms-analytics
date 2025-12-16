package com.pms.analytics.scheduler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.service.RiskMetricsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskMetricsScheduler {

    private final AnalysisDao analysisDao;
    private final RiskMetricsService riskMetricsService;

    // Runs every 10 seconds to compute risk metrics and store in outbox
    @Scheduled(fixedRate = 10000)
    public void computeRiskMetricsForAllPortfolios() {

        // Fetch all portfolio IDs from AnalysisDao
        List<UUID> portfolioIds = analysisDao.findAll().stream()
                .map(a -> a.getId().getPortfolioId())
                .distinct()
                .collect(Collectors.toList());

        if (portfolioIds.isEmpty()) {
            System.out.println("[Scheduler] No portfolios found to compute risk metrics.");
            return;
        }

        System.out.println("[Scheduler] Computing risk metrics for " + portfolioIds.size() + " portfolios...");

        // Compute risk metrics for each portfolio
        portfolioIds.forEach(portfolioId -> {
            riskMetricsService.computeRiskEvent(portfolioId);
        });
    }
}
