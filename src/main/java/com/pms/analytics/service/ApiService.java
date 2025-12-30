package com.pms.analytics.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.entity.AnalysisEntity;

@Service
public class ApiService {
    @Autowired
    AnalysisDao analysisDao;

    @Autowired
    UnrealizedPnlCalculator unrealizedPnl;

    public List<AnalysisEntity> getAllAnalysis(){
        return analysisDao.findAll();
    }

    public void getUnrealizedPnl(){
        unrealizedPnl.computeUnRealisedPnlAndBroadcast();
    }

}
