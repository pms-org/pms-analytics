package com.pms.analytics.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.PortfolioValueHistoryEntity;
import com.pms.analytics.service.ApiService;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    ApiService apiService;

    @GetMapping("/analysis/all")
    public ResponseEntity<List<AnalysisEntity>> getAllAnalysis(){
        return ResponseEntity.ok(apiService.getAllAnalysis());
    }

    @GetMapping("/unrealized")
    public void getUnrealizedPnl(){
        apiService.getUnrealizedPnl();
    }

    @GetMapping("/portfolio_value/history")
    public ResponseEntity<List<PortfolioValueHistoryEntity>> getPortfolioValueHistoryById(UUID portfolioId){
        return ResponseEntity.ok(apiService.getAllHistoryById(portfolioId));
    }
}
