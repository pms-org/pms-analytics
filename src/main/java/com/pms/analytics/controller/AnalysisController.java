package com.pms.analytics.controller;

import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    AnalysisService analysisService;

    @GetMapping("/all")
    public ResponseEntity<List<AnalysisEntity>> getAllAnalysis(){
        return ResponseEntity.ok(analysisService.getAllAnalysis());
    }
}
