package com.pms.analytics.service;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.pms.analytics.dto.TransactionBatch;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaTransactionListener {
    
    @Autowired
    private BlockingQueue<TransactionBatch> buffer;

    @Autowired
    private BatchProcessor batchProcessor;

    @Value("${app.buffer.size}")
    private int totalBufferCapacity;
    
    // @KafkaListener(
    //         id = "transactionsListener",
    //         topics = "transactions",
    //         groupId = "demo-group",
    //         containerFactory = "protobufKafkaListenerContainerFactory"
    // )
    @KafkaListener(
            id = "${app.kafka.consumer-id}",
            topics = "${app.kafka.consumer-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void consume(List<Transaction> messages, Acknowledgment ack) {

        log.info("Received {} Transaction messages: " + messages, messages.size());
        
        buffer.offer(new TransactionBatch(messages,ack));
        log.info("Transactions Successfully added to buffer.");

        if(buffer.size() >= (0.8 * totalBufferCapacity))
        {
            log.info("Buffer is 80% full flushing the buffer.");
            batchProcessor.submitBatchForProcessing();
        }

        log.info("Kafka consumer thread free...");

    }

}
