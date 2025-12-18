package com.pms.analytics.service;

import java.util.List;

import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
public class KafkaTransactionListener {

    @Autowired
    private BatchProcessingService batchProcessingService;
   

    @KafkaListener(
            topics = "transactions",
            groupId = "demo-group",
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void consume(List<Transaction> messages) {
        System.out.println("Received Transaction messages: " + messages);
        
        batchProcessingService.processBatch(messages);

    }

}
