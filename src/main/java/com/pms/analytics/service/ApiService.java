package com.pms.analytics.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.PortfolioValueHistoryDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.PortfolioValueHistoryEntity;

@Service
public class ApiService {
    @Autowired
    AnalysisDao analysisDao;

    @Autowired
    UnrealizedPnlCalculator unrealizedPnl;

    @Autowired
    PortfolioValueHistoryDao historyDao;

    public List<AnalysisEntity> getAllAnalysis(){
        return analysisDao.findAll();
    }

    public void getUnrealizedPnl(){
        unrealizedPnl.computeUnRealisedPnlAndBroadcast();
    }

    public List<PortfolioValueHistoryEntity> getAllHistoryById(UUID portfolioId){
        return historyDao.findTop30ByPortfolioIdOrderByDateDesc(portfolioId);
    }

}
