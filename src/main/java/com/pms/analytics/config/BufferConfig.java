package com.pms.analytics.config;

import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pms.analytics.dto.TransactionBatch;



@Configuration
public class BufferConfig {

    @Value("${app.buffer.size}")
    private int bufferSize;

    @Bean
    public LinkedBlockingDeque<TransactionBatch> protoBuffer() {
        return new LinkedBlockingDeque<TransactionBatch>(bufferSize);
    }

}
