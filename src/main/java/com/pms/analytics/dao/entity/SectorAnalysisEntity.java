package com.pms.analytics.dao.entity;

import java.io.Serializable;
import java.time.Instant;
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

import java.math.BigDecimal;


@Entity
@Table(name="sector_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorAnalysisEntity {

    // @Column(name = "portfolio_id")
    // private UUID portfolioId;

    // @Column(name = "sector_name")
    // private String sectorName;

    @EmbeddedId
    private SectorAnalysisKey id;

    @Column(name = "invested_price")
    private BigDecimal investedPrice;

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

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorAnalysisKey implements Serializable {
        @Column(name = "portfolio_id")
        private UUID portfolioId;

        @Column(name = "sector_name")
        private String sectorName;
    }

}
