package com.pms.analytics.controller;

import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.publisher.TransactionPublisher;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionPublisher publisherService;

    @PostMapping
    public ResponseEntity<String> publishTransaction(@RequestBody TransactionDto transactionDto) {
        publisherService.sendTransaction(transactionDto);
        return ResponseEntity.ok("Transaction sent to Kafka");
    }
}
