package com.pms.analytics.dao.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pms.analytics.utilities.TradeSide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pnl")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PnlEntity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "symbol")
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private TradeSide side;

    @Column(name = "buy_price")
    private BigDecimal buyPrice;

    @Column(name = "remaining_quantity")
    private Long remainingQuantity;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "realized_pnl")
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl")
    private BigDecimal unrealizedPnl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }
}
