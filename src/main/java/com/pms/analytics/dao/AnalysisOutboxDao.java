package com.pms.analytics.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.pms.analytics.dao.entity.AnalysisOutbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface AnalysisOutboxDao extends JpaRepository<AnalysisOutbox, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM AnalysisOutbox o WHERE o.status = :status ORDER BY o.analysisOutboxId ASC")
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    })
    List<AnalysisOutbox> fetchPendingOutboxForProcessing(String status, Pageable pageable);
    

}
