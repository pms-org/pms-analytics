package com.pms.analytics.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.DltOutboxDao;
import com.pms.analytics.dto.BatchResult;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BatchProcessingService {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnalysisDao analysisDao;

    @Autowired
    private DltOutboxDao dltOutboxDao;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processBatch(List<Transaction> messages) {
        System.out.println("Processing batch of " + messages.size() + " transactions.");

        BatchResult result = transactionService.processBatchInTransaction(messages);

        // Mark all processed transaction IDs
        result.processedTransactionIds().forEach((transactionId) -> {
            idempotencyService.markProcessed(transactionId);
        });

        try {
            
            //send updated positions to web socket
            log.info("Sending updated positions {} to web socket.", result.batchedAnalysisEntities());
            messagingTemplate.convertAndSend("/topic/position-update", result.batchedAnalysisEntities());

        } catch (RuntimeException ex) {
            System.out.println("Failed while sending through websocket");
        }

    }
}