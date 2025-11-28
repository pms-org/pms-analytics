package com.pms.analytics.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.analytics.dao.entity.PositionEntity;
import com.pms.analytics.dao.entity.PositionEntity.PositionKey;

public interface PositionDao extends JpaRepository<PositionEntity, PositionKey>{

}
