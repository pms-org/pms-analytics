package com.pms.analytics.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.analytics.dao.entity.TradesEntity;

public interface TradesDao extends JpaRepository<TradesEntity, UUID>{
    
}
