package com.pms.analytics.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "analysis_outbox")
public class AnalysisOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "AnalysisOutbox_id")
    private UUID AnalysisOutboxId;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Lob
    @Column(name = "payload", nullable = false)
    private byte[] payload;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
