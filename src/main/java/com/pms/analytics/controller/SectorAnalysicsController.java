package com.pms.analytics.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.analytics.dto.SectorCatalogDto;
import com.pms.analytics.dto.SectorMetricsDto;
import com.pms.analytics.dto.SymbolMetricsDto;
import com.pms.analytics.service.SectorAnalysisServie;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
public class SectorAnalysicsController {

    @Autowired
    SectorAnalysisServie sectorAnalysisServie;

    @GetMapping("/overall")
    public List<SectorMetricsDto> overallSectorAnalysis() {
        return sectorAnalysisServie.overallSectorAnalysis();
    }

    @GetMapping("/sector-wise/{sector}")
    public List<SymbolMetricsDto> sectorWiseAnalysis(@PathVariable String sector) {
        return sectorAnalysisServie.sectorWiseAnalysis(sector);
    }

    @GetMapping("/portfolio-wise/{portfolioId}")
    public List<SectorMetricsDto> portfolioSectorAnalysis(
            @PathVariable UUID portfolioId) {
        return sectorAnalysisServie.portfolioSectorAnalysis(portfolioId);
    }

    @GetMapping("/portfolio-wise/{portfolioId}/sector-wise/{sector}")
    public List<SymbolMetricsDto> sectorWisePortfolioAnalysis(
            @PathVariable UUID portfolioId,
            @PathVariable String sector) {
        return sectorAnalysisServie.sectorWisePortfolioAnalysis(portfolioId, sector);
    }

    @GetMapping("/sector-catalog")
    public List<SectorCatalogDto> sectorCatalog() {
        return sectorAnalysisServie.sectorCatalog();
    }
}

