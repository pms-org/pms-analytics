package com.pms.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pms.analytics.utilities.TradeSide;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private UUID transactionId; 
    private UUID tradeId;
    private UUID portfolioId;
    private String symbol;
    private TradeSide side;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private long quantity;
    private long remainingQuantity;
    private Instant timestamp;
}
