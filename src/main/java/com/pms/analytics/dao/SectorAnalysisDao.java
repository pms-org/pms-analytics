package com.pms.analytics.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.analytics.dao.entity.SectorAnalysisEntity;
import com.pms.analytics.dao.entity.SectorAnalysisEntity.SectorAnalysisKey;

public interface SectorAnalysisDao extends JpaRepository<SectorAnalysisEntity, SectorAnalysisKey>{

}
