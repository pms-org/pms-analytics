package com.pms.analytics.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.AnalysisOutboxDao;
import com.pms.analytics.dao.PortfolioRiskStatusDao;
import com.pms.analytics.dao.entity.AnalysisOutbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskMetricsCalculator {

    private final AnalysisDao analysisDao;
    private final RiskMetricsService riskMetricsService;
    private final AnalysisOutboxDao analysisOutboxDao;
    

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

        List<AnalysisOutbox> batchedOutboxEntries = new ArrayList<>();

        System.out.println("[Scheduler] Computing risk metrics for " + portfolioIds.size() + " portfolios...");

        // Compute risk metrics for each portfolio
        for(UUID portfolioId : portfolioIds)
        {
            riskMetricsService.computeRiskForSinglePortfolio(portfolioId, batchedOutboxEntries);
        };

        //save as batch here in outbox
        log.info("Saving {} records in outbox.",batchedOutboxEntries.size());
        analysisOutboxDao.saveAll(batchedOutboxEntries);

    }
    
}
