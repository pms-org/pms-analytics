package com.pms.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.StockDao;
import com.pms.analytics.dao.entity.StockEntity;
import com.pms.analytics.dto.SectorCatalogDto;
import com.pms.analytics.dto.SectorMetricsDto;
import com.pms.analytics.dto.SymbolMetricsDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectorAnalysisServie {

    private final AnalysisDao analysisDao;
    private final StockDao stockDao;

    public List<SectorMetricsDto> overallSectorAnalysis() {
        List<SectorMetricsDto> sectors = analysisDao.overallSectorAnalysis();
        calculateSectorPercentage(sectors);
        return sectors;
    }

    public List<SymbolMetricsDto> sectorWiseAnalysis(String sector) {
        List<SymbolMetricsDto> symbols = analysisDao.sectorWiseAnalysis(sector);
        calculateSymbolPercentage(symbols);
        return symbols;
    }

    public List<SectorMetricsDto> portfolioSectorAnalysis(UUID portfolioId) {
        List<SectorMetricsDto> sectors =
                analysisDao.portfolioSectorAnalysis(portfolioId);

        calculateSectorPercentage(sectors);
        return sectors;
    }

    public List<SymbolMetricsDto> sectorWisePortfolioAnalysis(
            UUID portfolioId,
            String sector
    ) {
        List<SymbolMetricsDto> symbols =
                analysisDao.sectorWisePortfolioAnalysis(portfolioId, sector);

        calculateSymbolPercentage(symbols);
        return symbols;
    }

    public List<SectorCatalogDto> sectorCatalog() {

        Map<String, List<String>> sectorMap =
                stockDao.findAll()
                        .stream()
                        .collect(Collectors.groupingBy(
                                StockEntity::getSectorName,
                                Collectors.mapping(
                                        StockEntity::getSymbol,
                                        Collectors.toList()
                                )
                        ));

        return sectorMap.entrySet()
                .stream()
                .map(e -> new SectorCatalogDto(e.getKey(), e.getValue()))
                .toList();
    }

    private void calculateSectorPercentage(List<SectorMetricsDto> sectors) {

        BigDecimal totalInvested = sectors.stream()
                .map(SectorMetricsDto::getTotalInvested)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalInvested.compareTo(BigDecimal.ZERO) == 0) return;

        sectors.forEach(sector -> {
            double percentage = sector.getTotalInvested()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalInvested, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            sector.setPercentage(percentage);
        });
    }

    private void calculateSymbolPercentage(List<SymbolMetricsDto> symbols) {

        BigDecimal totalInvested = symbols.stream()
                .map(SymbolMetricsDto::getTotalInvested)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalInvested.compareTo(BigDecimal.ZERO) == 0) return;

        symbols.forEach(symbol -> {
            double percentage = symbol.getTotalInvested()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalInvested, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            symbol.setPercentage(percentage);
        });
    }
}
