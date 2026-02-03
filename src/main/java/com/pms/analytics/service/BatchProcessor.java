package com.pms.analytics.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.pms.analytics.dto.TransactionBatch;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BatchProcessor implements SmartLifecycle {

    @Autowired
    private BatchProcessingService batchProcessingService;

    @Autowired
    private LinkedBlockingDeque<TransactionBatch> buffer;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("batchExecutor")
    private ThreadPoolTaskExecutor batchProcessorExecutor;

    @Autowired
    @Qualifier("dbRecoveryScheduler")
    private ThreadPoolTaskScheduler dbRecoveryScheduler;

    @Autowired
    @Qualifier("batchFlushScheduler")
    private ThreadPoolTaskScheduler batchFlushScheduler;

    @Autowired
    @Qualifier("redisRecoveryScheduler")
    private ThreadPoolTaskScheduler redisRecoveryScheduler;

    @Value("${app.buffer.size}")
    private int totalBufferCapacity;

    @Value("${app.buffer.flush-interval-ms}")
    private long FLUSH_INTERVAL_MS;

    @Value("${app.kafka.consumer-id}")
    private String CONSUMER_ID;

    private boolean isRunning = false;
    private volatile boolean isRecovering = false;

    private volatile ScheduledFuture<?> dbRecoveryTask;
    private volatile ScheduledFuture<?> redisRecoveryTask;
    private volatile ScheduledFuture<?> flushTask;

    @Override
    public void start() {
        log.info("BatchProcessor starting: Initializing time-based flush");
        startFlushScheduler();
        this.isRunning = true;
    }

    public void submitBatchForProcessing() {
        pauseConsumerAndStartHealthCheck(false, false);
        batchProcessorExecutor.execute(this::flushBatch);
        log.info("Submitted the transaction batches to task executor.");
    }

    private synchronized void flushBatch() {
        log.info("Flush batch thread started.");
        if (buffer.isEmpty()) {
            log.info("Buffer is empty.");
            return;
        }
        List<TransactionBatch> pollsInTheBatch = new ArrayList<>();
        List<Transaction> batchTransactions = new ArrayList<>();

        while (!buffer.isEmpty()) {
            TransactionBatch poll = buffer.poll();

            pollsInTheBatch.add(poll);
            batchTransactions.addAll(poll.getTransactionProtos());
        }

        try {
            log.info("Processing {} Transactions from the buffer.", batchTransactions.size());
            batchProcessingService.processBatch(batchTransactions);

            pollsInTheBatch.forEach(poll -> poll.getAck().acknowledge());
            log.info("Ack sent to kafka for all transactions");

            if (isRecovering && buffer.size() <= 0.5 * totalBufferCapacity) {
                resumeConsumer();
            }
        } catch (CannotCreateTransactionException | DataAccessException ex) {

            log.error("Error occured while processing the transactions batch.");

            log.info("Buffer size before re-storing: {}", buffer.size());

            for (int i = pollsInTheBatch.size() - 1; i >= 0; i--) {
                buffer.offerFirst(pollsInTheBatch.get(i));
            }

            log.info("Re-Stored the failed batches again in buffer, Buffer Size: {}.", buffer.size());

            pauseFlushScheduler();

            Throwable root = ex.getMostSpecificCause();

            if (root instanceof io.lettuce.core.RedisException || root instanceof java.nio.channels.ClosedChannelException) {

                log.error("Redis DOWN -> retrying later");

                log.error("Root exception class: {}", root.getClass().getName());
                log.error("Root exception message: {}", root.getMessage());

                pauseConsumerAndStartHealthCheck(false, true);

            } else {
                log.error("DB DOWN → Pausing Kafka consumption");

                log.error("Root exception class: {}", root.getClass().getName());
                log.error("Root exception message: {}", root.getMessage());

                pauseConsumerAndStartHealthCheck(true, false);

            }

        }
    }

    public void pauseConsumerAndStartHealthCheck(boolean startDBHealthCheck, boolean startRedisHealthCheck) {
        synchronized (this) {
            if (isRecovering) {
                return;
            }
            isRecovering = true;
        }

        MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
        if (container != null && !container.isContainerPaused()) {
            container.pause();
            log.warn("Kafka Consumer paused.");
        }
        if (startDBHealthCheck) {
            log.warn(" Starting background DB health checker...");
            startDBHealthCheck();
        } else if (startRedisHealthCheck) {
            log.warn(" Starting background Redis health checker...");
            startRedisHealthCheck();
        }

    }

    private void startDBHealthCheck() {
        dbRecoveryTask = dbRecoveryScheduler.scheduleWithFixedDelay(() -> {
            try {
                jdbcTemplate.execute("SELECT 1");
                log.info("Database is up! Resuming consumer and stopping daemon.");

                MessageListenerContainer container = kafkaListenerEndpointRegistry
                        .getListenerContainer(CONSUMER_ID);
                if (container != null && container.isContainerPaused()) {
                    container.resume();
                }

                synchronized (this) {
                    isRecovering = false;
                    if (dbRecoveryTask != null) {
                        dbRecoveryTask.cancel(false);
                        dbRecoveryTask = null;
                    }
                }

                startFlushScheduler();
            } catch (Exception e) {
                log.warn("Daemon: Database still down. Retrying in 10s...");
            }
        }, Duration.ofMillis(10000));
    }

    private void startRedisHealthCheck() {
        redisRecoveryTask = redisRecoveryScheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!isRedisWritable()) {
                    throw new IllegalStateException("Redis not writable");
                }

                log.info("Redis PRIMARY is UP (Writable)");

                MessageListenerContainer container = kafkaListenerEndpointRegistry
                        .getListenerContainer(CONSUMER_ID);

                if (container != null && container.isContainerPaused()) {
                    container.resume();
                }

                synchronized (this) {
                    isRecovering = false;
                    if (redisRecoveryTask != null) {
                        redisRecoveryTask.cancel(false);
                        redisRecoveryTask = null;
                    }
                }

                startFlushScheduler();
            } catch (Exception e) {
                log.warn("Daemon: Redis still down. Retrying in 10s...");
            }
        }, Duration.ofMillis(10000));
    }

    private boolean isRedisWritable() {

        try {

            Properties replicationInfo = redisTemplate.execute(
                    (RedisCallback<Properties>) connection
                    -> connection.serverCommands().info("replication")
            );

            if (replicationInfo == null) {
                return false;
            }

            String role = replicationInfo.getProperty("role");

            return "master".equalsIgnoreCase(role);

        } catch (Exception e) {
            return false;
        }
    }

    public void startFlushScheduler() {
        if (flushTask == null || flushTask.isCancelled()) {

            flushTask = batchFlushScheduler.scheduleWithFixedDelay(
                    this::flushBatch,
                    Duration.ofMillis(FLUSH_INTERVAL_MS)
            );

            log.info("Flush scheduler started.");
        }
    }

    public void pauseFlushScheduler() {
        if (flushTask != null && !flushTask.isCancelled()) {
            flushTask.cancel(false);
            log.info("Flush scheduler paused");
        }
    }

    private void resumeConsumer() {
        MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
        if (container != null && container.isContainerPaused()) {
            container.resume();
            log.info("Buffer cleared to 50% ({} batches). Resuming consumer.", buffer.size());
            synchronized (this) {
                isRecovering = false;
            }
        }
    }

    @Override
    public void stop(Runnable callback) {
        log.info("BatchProcessor stopping: Performing final flush");
        batchFlushScheduler.shutdown();
        dbRecoveryScheduler.shutdown();
        redisRecoveryScheduler.shutdown();

        if (!buffer.isEmpty()) {
            flushBatch();
        }

        this.isRunning = false;
        callback.run();
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

}
