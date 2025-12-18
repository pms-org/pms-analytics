package com.pms.analytics.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioSectorAnalysisDto {
    private UUID portfolioId;
    private List<SectorMetricsDto> sectors;
}
