package com.pms.analytics.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pms.analytics.dao.entity.PortfolioValueHistoryEntity;

public interface PortfolioValueHistoryDao extends JpaRepository<PortfolioValueHistoryEntity, UUID> {

    List<PortfolioValueHistoryEntity> findTop30ByPortfolioIdOrderByDateDesc(UUID portfolioId);

    List<PortfolioValueHistoryEntity> findTop29ByPortfolioIdOrderByDateDesc(UUID portfolioId);

    List<PortfolioValueHistoryEntity> findByPortfolioIdAndDateBetween(UUID portfolioId, LocalDate start, LocalDate end);
}
