package com.pms.analytics.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.analytics.dao.entity.StockEntity;
import java.util.List;


public interface StockDao extends JpaRepository<StockEntity, Long>{
    List<StockEntity> findBySectorName(String sectorName);

    List<StockEntity> findAll();
}
