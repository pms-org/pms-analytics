package com.pms.analytics.service;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DbHealthMonitor {

    private final AnalysisDao analysisDao;
    private final KafkaListenerEndpointRegistry registry;

    private volatile boolean paused = false;
    private volatile boolean monitoring = false;

    public synchronized void pause() {
        if (!paused) {
            log.info("Pausing kafka consumer (DB down)");
            paused = true;
        }

        if (!monitoring) {
            log.info("Starting DB health monitor thread");
            monitoring = true;
            monitorAndResume();
        }
    }

    @Async
    public void monitorAndResume() {
        try {
            while (paused) {
                if (databaseIsUp()) {
                    log.info("DB is back → Resuming Kafka consumption");
                    registry.getListenerContainer("transactionsListener").start();
                    paused = false;
                    monitoring = false;
                    return;
                }

                log.info("DB still down, retrying in 5s");
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            monitoring = false;
        }
    }

    private boolean databaseIsUp() {
        try {
            analysisDao.count();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
