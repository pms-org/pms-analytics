package com.pms.analytics.dao;

import com.pms.analytics.dao.entity.TransactionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TransactionsDao extends JpaRepository<TransactionsEntity, UUID> {

    @Query("SELECT DISTINCT t.trade.portfolioId FROM TransactionsEntity t WHERE t.trade.side = com.pms.analytics.utilities.TradeSide.BUY AND t.quantity > 0")
    List<UUID> findDistinctPortfolioIdsWithOpenPositions();

    @Query("SELECT t FROM TransactionsEntity t WHERE t.trade.portfolioId = :portfolioId AND t.trade.side = com.pms.analytics.utilities.TradeSide.BUY AND t.quantity > 0")
    List<TransactionsEntity> findOpenPositionsByPortfolioId(UUID portfolioId);

    @Query("SELECT DISTINCT t.trade.symbol FROM TransactionsEntity t WHERE t.trade.side = com.pms.analytics.utilities.TradeSide.BUY AND t.quantity > 0")
    List<String> findAllActiveSymbols();

}
