package com.pms.analytics.service;

import java.util.List;
import java.util.UUID;

import com.pms.analytics.dao.entity.AnalysisOutbox;

public record ProcessingResult(List<UUID> successfulIds,AnalysisOutbox poisonPill,boolean systemFailure){

    public static ProcessingResult success(List<UUID> ids){
        return new ProcessingResult(ids, null, false);
    }

    public static ProcessingResult poisonPill(List<UUID> ids, AnalysisOutbox bad){
        return new ProcessingResult(ids, bad, false);
    }

    public static ProcessingResult systemFailure(List<UUID> ids){
        return new ProcessingResult(ids, null, true);
    }
}