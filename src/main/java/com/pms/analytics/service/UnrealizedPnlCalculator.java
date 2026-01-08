package com.pms.analytics.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.PortfolioUnrealizedPnlStatusDao;
import com.pms.analytics.dao.TransactionsDao;
import com.pms.analytics.dao.entity.TransactionsEntity;
import com.pms.analytics.dto.UnrealizedPnlDto;
import com.pms.analytics.externalRedis.ExternalPriceClient;
import com.pms.analytics.externalRedis.RedisPriceCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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

            List<UnrealizedPnlDto> unrealizedPnlDtos = new ArrayList<>();

            for (UUID portfolioId : portfolioIds) {

                unrealizedPnlService.computeUnrealizedPnlForSinglePortfolio(portfolioId);
                
            }

        } catch (Exception e) {
            System.err.println("Scheduler failed: " + e.getMessage());
        }
    }
}
