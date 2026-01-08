package com.pms.analytics.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pms.analytics.dao.entity.AnalysisOutbox;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface AnalysisOutboxDao extends JpaRepository<AnalysisOutbox, UUID> {

    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query("SELECT o FROM AnalysisOutbox o WHERE o.status = :status ORDER BY o.analysisOutboxId ASC")
    // @QueryHints({
    //     @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    // })
    // List<AnalysisOutbox> fetchPendingOutboxForProcessing(String status, Pageable pageable);

    
    @Query(value = """
                        SELECT *
                        FROM analytics_outbox e
                        WHERE e.status = 'PENDING'
                          AND pg_try_advisory_xact_lock(
                                hashtext('ANALYTICS_OUTBOX:' || e.portfolio_id::text)
                              )
                        ORDER BY e.created_at
                        LIMIT :limit;
                                    """, nativeQuery = true)
    List<AnalysisOutbox> findPendingWithPortfolioXactLock(
            @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("update AnalysisOutbox e set e.status = 'SENT' where e.analysisOutboxId in :ids")
    void markAsSent(List<UUID> ids);

    @Modifying
    @Query("""
                update AnalysisOutbox e
                set e.status = 'FAILED'
                where e.analysisOutboxId = :id
                                                """)
    void markAsFailed(@Param("id") UUID id);

}
