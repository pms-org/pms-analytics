package com.pms.analytics.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnrealizedPnlDto {
    private Map<String, BigDecimal> symbol; // ex: {AAPL: 345677, GOOG: 35267}
    private BigDecimal overallUnrealised_Pnl;
    private String portfolio_id;
}
