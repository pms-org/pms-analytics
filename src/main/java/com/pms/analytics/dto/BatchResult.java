package com.pms.analytics.dto;

import java.util.List;
import java.util.Set;

import com.pms.analytics.dao.entity.AnalysisEntity;

public record BatchResult(
    List<AnalysisEntity> batchedAnalysisEntities,
    Set<String> processedTransactionIds
) {}
