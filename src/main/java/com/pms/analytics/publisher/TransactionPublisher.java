package com.pms.analytics.publisher;

import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;
import com.pms.analytics.mapper.TransactionMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionPublisher {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    public void sendTransaction(TransactionDto dto) {
        Transaction protoMsg = TransactionMapper.toProto(dto);

        String key = dto.getPortfolioId().toString();
        kafkaTemplate.send("transactions",key, protoMsg);
        System.out.println("Published Transaction with key=" + key + ": " + protoMsg);
    }
}
