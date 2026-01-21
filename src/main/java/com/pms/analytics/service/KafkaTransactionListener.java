package com.pms.analytics.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaTransactionListener {

    @Autowired
    private BatchProcessingService batchProcessingService;

    @Autowired
    private DbHealthMonitor dbHealthMonitor;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    // @KafkaListener(
    //         id = "transactionsListener",
    //         topics = "transactions",
    //         groupId = "demo-group",
    //         containerFactory = "protobufKafkaListenerContainerFactory"
    // )
    
    @KafkaListener(
            id = "transactionsListener",
            topics = "${app.kafka.consumer-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void consume(List<Transaction> messages, Acknowledgment ack) {
        try {
            System.out.println("Received Transaction messages: " + messages);

            batchProcessingService.processBatch(messages);

            ack.acknowledge();
        } catch (CannotCreateTransactionException | DataAccessException ex) {
            log.error("DB DOWN → Pausing Kafka consumption");

            log.info("Pausing the kafka consumer {}.");
        
            registry.getListenerContainer("transactionsListener").stop();
            
            dbHealthMonitor.pause();

            log.info("Consumer thread running continously.");
        }

    }

}
