package com.pms.analytics.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.mapper.TransactionMapper;

import lombok.RequiredArgsConstructor;

@Service
public class BatchProcessingService {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnalysisDao analysisDao;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processBatch(List<Transaction> messages) {
        // Batch processing logic goes here
        System.out.println("Processing batch of " + messages.size() + " transactions.");

        //create a map here 
        Map<AnalysisEntity.AnalysisKey,AnalysisEntity> cachedAnalysisMap = new HashMap<>();

        Set<String> processedTransactionIds = new HashSet<>();

        for(Transaction message : messages) {

            System.out.println("Processing Transaction message: " + message);

            if(idempotencyService.isDuplicate(message.getTransactionId().toString())) {
                System.out.println("Transaction: " + message.getTransactionId() + " already processed!");
                continue;
            }

            TransactionDto transactionDto = TransactionMapper.fromProto(message);

            // Process the transaction
            transactionService.processTransaction(transactionDto, cachedAnalysisMap);

            processedTransactionIds.add(message.getTransactionId().toString());
           
        }

        //save all as a batch to db
        List<AnalysisEntity> batchedAnalysisEntities = new ArrayList<>(cachedAnalysisMap.values());

        analysisDao.saveAll(batchedAnalysisEntities);

        messagingTemplate.convertAndSend("/topic/position-update", batchedAnalysisEntities);

        // Mark all processed transaction IDs
        processedTransactionIds.forEach((transactionId) ->  {
            idempotencyService.markProcessed(transactionId);
        });
    }
}
