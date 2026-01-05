package com.pms.analytics.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.pms.analytics.dto.TransactionOuterClass.Transaction;

@Service
public class KafkaTransactionListener {

    @Autowired
    private BatchProcessingService batchProcessingService;

    @KafkaListener(
            topics = "transactions",
            groupId = "demo-group",
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void consume(List<Transaction> messages, Acknowledgment ack) {
        System.out.println("Received Transaction messages: " + messages);
        
        batchProcessingService.processBatch(messages);

        ack.acknowledge();

    }

}
