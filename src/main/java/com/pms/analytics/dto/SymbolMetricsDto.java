package com.pms.analytics.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SymbolMetricsDto {
    private String symbol;
    private Double percentage;
    private Long holdings;
    private BigDecimal totalInvested;
    private BigDecimal realizedPnl;

    public SymbolMetricsDto(
            String symbol,
            Long holdings,
            BigDecimal totalInvested,
            BigDecimal realizedPnl
    ) {
        this.symbol = symbol;
        this.holdings = holdings;
        this.totalInvested = totalInvested;
        this.realizedPnl = realizedPnl;
    }
}
