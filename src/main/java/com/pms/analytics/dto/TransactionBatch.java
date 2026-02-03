package com.pms.analytics.dto;

import java.util.List;

import org.springframework.kafka.support.Acknowledgment;

import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionBatch {
    List<Transaction> transactionProtos;
    Acknowledgment ack;
}
