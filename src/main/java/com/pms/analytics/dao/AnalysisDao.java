package com.pms.analytics.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.AnalysisEntity.AnalysisKey;

import org.springframework.data.jpa.repository.Query;

import com.pms.analytics.dto.SectorMetricsDto;
import com.pms.analytics.dto.SymbolMetricsDto;

public interface AnalysisDao extends JpaRepository<AnalysisEntity, AnalysisKey>{
    
    List<AnalysisEntity> findByIdPortfolioId(UUID portfolioId);

    @Query("SELECT DISTINCT a.id.symbol FROM AnalysisEntity a")
    List<String> findAllSymbols();

    @Query("""
        SELECT new com.pms.analytics.dto.SectorMetricsDto(
            s.sectorName,
            SUM(a.holdings),
            SUM(a.totalInvested),
            SUM(a.realizedPnl)
        )
        FROM AnalysisEntity a
        JOIN StockEntity s ON a.id.symbol = s.symbol
        GROUP BY s.sectorName
    """)
    List<SectorMetricsDto> overallSectorAnalysis();

    @Query("""
        SELECT new com.pms.analytics.dto.SymbolMetricsDto(
            a.id.symbol,
            SUM(a.holdings),
            SUM(a.totalInvested),
            SUM(a.realizedPnl)
        )
        FROM AnalysisEntity a
        JOIN StockEntity s ON a.id.symbol = s.symbol
        WHERE s.sectorName = :sector
        GROUP BY a.id.symbol
    """)
    List<SymbolMetricsDto> sectorWiseAnalysis(String sector);

    @Query("""
        SELECT new com.pms.analytics.dto.SectorMetricsDto(
            s.sectorName,
            SUM(a.holdings),
            SUM(a.totalInvested),
            SUM(a.realizedPnl)
        )
        FROM AnalysisEntity a
        JOIN StockEntity s ON a.id.symbol = s.symbol
        WHERE a.id.portfolioId = :portfolioId
        GROUP BY s.sectorName
    """)
    List<SectorMetricsDto> portfolioSectorAnalysis(UUID portfolioId);

    @Query("""
        SELECT new com.pms.analytics.dto.SymbolMetricsDto(
            a.id.symbol,
            SUM(a.holdings),
            SUM(a.totalInvested),
            SUM(a.realizedPnl)
        )
        FROM AnalysisEntity a
        JOIN StockEntity s ON a.id.symbol = s.symbol
        WHERE a.id.portfolioId = :portfolioId
          AND s.sectorName = :sector
        GROUP BY a.id.symbol
    """)
    List<SymbolMetricsDto> sectorWisePortfolioAnalysis(
            UUID portfolioId,
            String sector
    );
    
}
