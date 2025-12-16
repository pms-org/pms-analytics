package com.pms.analytics.service;

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
    private TransactionService transactionService;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 3000, multiplier = 2, maxDelay = 10000)
    )
    @KafkaListener(
            topics = "transactions",
            groupId = "demo-group",
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void consume(Transaction message) {
        System.out.println("Received Transaction message: " + message);

        transactionService.processTransaction(message);
    }

    @DltHandler
    public void listenDLT(Transaction message) {
        System.out.println("DLT reached for transaction: " + message);

    }
}
