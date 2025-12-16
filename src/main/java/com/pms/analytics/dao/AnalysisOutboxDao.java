package com.pms.analytics.dao;

import com.pms.analytics.dao.entity.AnalysisOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisOutboxDao extends JpaRepository<AnalysisOutbox, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM AnalysisOutbox o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<AnalysisOutbox> findTopPendingForUpdate(Pageable pageable);
}
