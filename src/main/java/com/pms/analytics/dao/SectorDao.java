package com.pms.analytics.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.analytics.dao.entity.SectorEntity;
import java.util.List;


public interface SectorDao extends JpaRepository<SectorEntity, String>{
        List<String> findSymbolBySector(String sector);
}
