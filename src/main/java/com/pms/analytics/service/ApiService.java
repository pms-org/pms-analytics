package com.pms.analytics.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.StockDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dto.SectorMetricsDto;

@Service
public class ApiService {
    @Autowired
    AnalysisDao analysisDao;

    @Autowired
    UnrealizedPnlCalculator unrealizedPnl;

    @Autowired
    StockDao stockDao;

    public List<AnalysisEntity> getAllAnalysis(){
        return analysisDao.findAll();
    }

    public void getUnrealizedPnl(){
        unrealizedPnl.computeUnRealisedPnlAndBroadcast();
    }

}
