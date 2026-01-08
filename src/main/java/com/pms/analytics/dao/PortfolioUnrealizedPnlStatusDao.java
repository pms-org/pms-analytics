package com.pms.analytics.dao;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PortfolioUnrealizedPnlStatusDao {
    private final JdbcTemplate jdbcTemplate;

    public boolean computedRecently(UUID portfolioId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM analytics_portfolio_unrealizedpnl_status
            WHERE portfolio_id = ?
              AND last_computed_at > now() - interval '1 minute'
        """, Integer.class, portfolioId);

        return count != null && count > 0;
    }

    public boolean tryAdvisoryLock(UUID portfolioId) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        "SELECT pg_try_advisory_xact_lock(hashtext('UNREALIZED_PNL:' || ?))::boolean",
                        Boolean.class,
                        portfolioId.toString()
                )
        );
    }

    public void updateLastComputed(UUID portfolioId) {
        jdbcTemplate.update("""
            INSERT INTO analytics_portfolio_unrealizedpnl_status(portfolio_id, last_computed_at)
            VALUES (?, now())
            ON CONFLICT (portfolio_id)
            DO UPDATE SET last_computed_at = now()
        """, portfolioId);
    }
}
