package com.pms.analytics.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskEventDto {
    private UUID portfolioId;
    private double avgRateOfReturn;   // changed to double
    private double sharpeRatio;       // changed to double
    private double sortinoRatio;      // changed to double
}
