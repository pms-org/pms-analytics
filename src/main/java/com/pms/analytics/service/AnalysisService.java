package com.pms.analytics.service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {
    @Autowired
    AnalysisDao analysisDao;

    public List<AnalysisEntity> getAllAnalysis(){
        return analysisDao.findAll();
    }
}
