package com.pms.analytics.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pms.analytics.dao.TransactionsDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnrealizedPnlCalculator {

    private final TransactionsDao transactionsDao;
    private final UnrealizedPnlService unrealizedPnlService;

    public void computeUnRealisedPnlAndBroadcast() {

        try {
            log.info("Calculating Unrealized pnl ...");
            List<UUID> portfolioIds
                    = transactionsDao.findDistinctPortfolioIdsWithOpenPositions();

            for (UUID portfolioId : portfolioIds) {

                unrealizedPnlService.computeUnrealizedPnlForSinglePortfolio(portfolioId);
                
            }

        } catch (Exception e) {
            System.err.println("Scheduler failed: " + e.getMessage());
        }
    }
}
