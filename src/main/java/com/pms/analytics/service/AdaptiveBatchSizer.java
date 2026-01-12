package com.pms.analytics.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AdaptiveBatchSizer{

    @Value("${app.outbox.target-latency-ms:200}")
    private long targetLatencyMs;

    @Value("${app.outbox.min-batch:10}")
    private int minBatchSize;

    @Value("${app.outbox.max-batch:500}")
    private int maxBatchSize;

    private final AtomicInteger currentBatchSize = new AtomicInteger(10);

    public void adjust(long timeTakenMs, int recordsProcessed){
        log.info("Adjusting outbox batch size.");
        int current = currentBatchSize.get();
        int next = current;

        // Drain phase
        if (recordsProcessed < current){
            log.info("In Drain phase of outbox batch size.");
            next = minBatchSize;
        }
        // Growth / shrink phase
        else if(timeTakenMs < targetLatencyMs){
            log.info("In Growth phase of outbox batch size.");
            next = Math.min((int)(current * 1.2), maxBatchSize);
        } 
        else{
            log.info("In Shrink phase of outbox batch size.");
            next = Math.max((int)(current * 0.7), minBatchSize);
        }

        currentBatchSize.set(next);
        log.info("Adjusted new batch size {}.",next);
    }

    public int getCurrentSize(){
        return currentBatchSize.get();
    }

    public void reset(){
        currentBatchSize.set(minBatchSize);
    }
}