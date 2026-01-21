// package com.pms.analytics.service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;
// import java.util.concurrent.TimeUnit;

// import org.apache.kafka.common.errors.SerializationException;
// import org.apache.kafka.common.errors.TimeoutException;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// import com.google.protobuf.InvalidProtocolBufferException;
// import com.pms.analytics.dao.AnalysisOutboxDao;
// import com.pms.analytics.dao.entity.AnalysisOutbox;

// import com.pms.analytics.dto.RiskEventOuterClass;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class OutboxEventProcessor {

//     private final AnalysisOutboxDao outboxDao;

//     private final AdaptiveBatchSizer batchSizer;

//     private final KafkaTemplate<String, RiskEventOuterClass.RiskEvent> kafkaTemplate;

//     @Value("${app.producer-topic}")
//     private final String TOPIC;

//     @Transactional
//     public ProcessingResult dispatchOnce() {

//         int limit = batchSizer.getCurrentSize();
//         log.info("Limit of this batch {}.", limit);

//         List<AnalysisOutbox> batch = outboxDao.findPendingWithPortfolioXactLock(limit);

//         log.info("Fetched {} from outbox.", batch.size());

//         if (batch.isEmpty()) {
//             batchSizer.reset();
//             return ProcessingResult.success(List.of());
//         }

//         // START timing
//         long start = System.currentTimeMillis();

//         ProcessingResult result = process(batch);

//         // END timing
//         long duration = System.currentTimeMillis() - start;

//         // Adaptive feedback loop
//         // if (!result.systemFailure()) {
//         // batchSizer.adjust(duration, batch.size());
//         // }

//         if (!result.successfulIds().isEmpty()) {
//             outboxDao.markAsSent(result.successfulIds());
//             log.info("Updated {} outbox events to SENT",result.successfulIds());
//         }

//         if (!result.systemFailure() && result.poisonPill() == null) {
//             batchSizer.adjust(duration, batch.size());
//         }

//         if (result.poisonPill() != null) {

//             AnalysisOutbox poison = result.poisonPill();

//             // InvalidTradesEntity invalid = new InvalidTradesEntity();
//             // invalid.setAggregateId(poison.getAggregateId());
//             // invalid.setPayload(poison.getPayload());
//             // invalid.setErrorMessage("Poison pill – processing failed");

//             // invalidTradesDao.save(invalid);
            
//             outboxDao.markAsFailed(poison.getAnalysisOutboxId());
//         }

//         return result;
//     }

//     public ProcessingResult process(List<AnalysisOutbox> events) {

//         List<UUID> successfulIds = new ArrayList<>();

//         for (AnalysisOutbox outbox : events) {
//             try {
//                 RiskEventOuterClass.RiskEvent event = RiskEventOuterClass.RiskEvent.parseFrom(outbox.getPayload());

//                 kafkaTemplate.send(TOPIC, event.getPortfolioId(), event).get();

//                 log.info("Event {} sent to kakfa successfully.",event);

//                 successfulIds.add(outbox.getAnalysisOutboxId());

//             } catch (InvalidProtocolBufferException e) {
//                 return ProcessingResult.poisonPill(successfulIds, outbox);
//             } catch (Exception e) {
//                 return ProcessingResult.systemFailure(successfulIds);
//             }
//         }

//         return ProcessingResult.success(successfulIds);
//     }
// }

package com.pms.analytics.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pms.analytics.dao.AnalysisOutboxDao;
import com.pms.analytics.dao.entity.AnalysisOutbox;
import com.pms.analytics.dto.RiskEventOuterClass.RiskEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OutboxEventProcessor {

    private final AnalysisOutboxDao outboxDao;
    private final AdaptiveBatchSizer batchSizer;
    private final KafkaTemplate<String, RiskEvent> kafkaTemplate;
    private final String topic;

    public OutboxEventProcessor(AnalysisOutboxDao outboxDao,
                                AdaptiveBatchSizer batchSizer,
                                KafkaTemplate<String, RiskEvent> kafkaTemplate,
                                @org.springframework.beans.factory.annotation.Value("${app.kafka.producer-topic}") String topic) {
        this.outboxDao = outboxDao;
        this.batchSizer = batchSizer;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Transactional
    public ProcessingResult dispatchOnce() {

        int limit = batchSizer.getCurrentSize();
        log.info("Limit of this batch {}.", limit);

        List<AnalysisOutbox> batch = outboxDao.findPendingWithPortfolioXactLock(limit);

        log.info("Fetched {} from outbox.", batch.size());

        if (batch.isEmpty()) {
            batchSizer.reset();
            return ProcessingResult.success(List.of());
        }

        long start = System.currentTimeMillis();
        ProcessingResult result = process(batch);
        long duration = System.currentTimeMillis() - start;

        if (!result.successfulIds().isEmpty()) {
            outboxDao.markAsSent(result.successfulIds());
            log.info("Updated {} outbox events to SENT", result.successfulIds());
        }

        if (!result.systemFailure() && result.poisonPill() == null) {
            batchSizer.adjust(duration, batch.size());
        }

        if (result.poisonPill() != null) {
            AnalysisOutbox poison = result.poisonPill();
            outboxDao.markAsFailed(poison.getAnalysisOutboxId());
        }

        return result;
    }

    public ProcessingResult process(List<AnalysisOutbox> events) {

        List<UUID> successfulIds = new ArrayList<>();

        for (AnalysisOutbox outbox : events) {
            try {
                RiskEvent event =
                        RiskEvent.parseFrom(outbox.getPayload());

                kafkaTemplate.send(topic, event.getPortfolioId(), event).get();

                log.info("Event {} sent to Kafka successfully.", event);

                successfulIds.add(outbox.getAnalysisOutboxId());

            } catch (InvalidProtocolBufferException e) {
                return ProcessingResult.poisonPill(successfulIds, outbox);
            } catch (Exception e) {
                return ProcessingResult.systemFailure(successfulIds);
            }
        }

        return ProcessingResult.success(successfulIds);
    }
}
