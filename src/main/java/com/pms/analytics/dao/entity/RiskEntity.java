package com.pms.analytics.dao.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="risk")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskEntity {

    @Id
    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "sharpe_ratio")
    private float sharpeRatio;

    @Column(name = "sortino_ratio")
    private float sortinoRatio;

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
