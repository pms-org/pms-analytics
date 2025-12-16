package com.pms.analytics.dao.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisEntity {

    @EmbeddedId
    private AnalysisKey id;

    @Column(name = "holdings")
    private Long holdings;

    @Column(name = "total_invested")
    private BigDecimal totalInvested;

    @Column(name = "realized_pnl")
    private BigDecimal realizedPnl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisKey implements Serializable {

        @Column(name = "portfolio_id")
        private UUID portfolioId;

        @Column(name = "symbol")
        private String symbol;

    }
}