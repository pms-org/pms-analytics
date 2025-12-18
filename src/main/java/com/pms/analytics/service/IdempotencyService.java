package com.pms.analytics.service;

import org.springframework.stereotype.Service;

import com.pms.analytics.externalRedis.RedisTransactionCache;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final RedisTransactionCache transactionCache;

    public boolean isDuplicate(String transactionId) {
        return transactionCache.isDuplicate(transactionId);
    }
   
    public void markProcessed(String transactionId) {
        transactionCache.markProcessed(transactionId);
    }
}
