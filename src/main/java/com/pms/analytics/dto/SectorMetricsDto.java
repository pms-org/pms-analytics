package com.pms.analytics.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectorMetricsDto {
    private String sector;
    private Double percentage;
    private Long totalHoldings;
    private BigDecimal totalInvested;
    private BigDecimal realizedPnl;

    public SectorMetricsDto(
            String sector,
            Long totalHoldings,
            BigDecimal totalInvested,
            BigDecimal realizedPnl
    ) {
        this.sector = sector;
        this.totalHoldings = totalHoldings;
        this.totalInvested = totalInvested;
        this.realizedPnl = realizedPnl;
    }
}

