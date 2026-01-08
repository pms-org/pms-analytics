// package com.pms.analytics.publisher;

// import java.time.LocalDateTime;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.ExecutionException;

// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.support.TransactionTemplate;

// import com.google.protobuf.InvalidProtocolBufferException;
// import com.pms.analytics.dao.AnalysisOutboxDao;
// import com.pms.analytics.dao.entity.AnalysisOutbox;
// import com.pms.analytics.dto.RiskEventOuterClass;
// import com.pms.analytics.service.AdvisoryLockService;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class EventPublisher {

//     private final KafkaTemplate<String, RiskEventOuterClass.RiskEvent> kafkaTemplate;
//     private final AnalysisOutboxDao outboxDao;
//     private final AdvisoryLockService advisoryLockService;
//     private final TransactionTemplate transactionTemplate;

//     private static final String TOPIC = "portfolio-risk-metrics";
//     private static final int BATCH_SIZE = 50;

//     @Scheduled(fixedRate = 5000)
//     public void publishPendingEvents() {

//         Pageable pageable = PageRequest.of(0, BATCH_SIZE);

//         Set<String> lockedPortfolios = new HashSet<>();
//         Set<String> rejectedPortfolios = new HashSet<>();
//         List<AnalysisOutbox> events;

//         try {
//             events = transactionTemplate.execute(status
//                     -> outboxDao.fetchPendingOutboxForProcessing("PENDING", pageable)
//             );

//             log.info("Fetched pending events {}.",events);

//         } catch (RuntimeException ex) {
//             System.out.println("Failed while trying to fetch and lock the outbox");
//             return;
//         }

//         if (events.isEmpty()) {
//             return;
//         }

//         try {
//             for (AnalysisOutbox outbox : events) {
//                 try {
//                     if (rejectedPortfolios.contains(outbox.getPortfolioId().toString())) {
//                         log.info("Portfolio {} is already in rejected list.", outbox.getPortfolioId().toString());
//                         continue;
//                     }

//                     if (!lockedPortfolios.contains(outbox.getPortfolioId().toString())) {
//                         boolean locked = advisoryLockService.acquireLock(outbox.getPortfolioId().toString());

//                         if (!locked) {
//                             rejectedPortfolios.add(outbox.getPortfolioId().toString());
//                             log.info("Portfolio already locked by some other instance rejected the portfolio {}.",outbox.getPortfolioId().toString());
//                             continue;
//                         } else {
//                             lockedPortfolios.add(outbox.getPortfolioId().toString());
//                             log.info("Successfully locked the portfolio {}.",outbox.getPortfolioId().toString());
//                         }
//                     }

//                     RiskEventOuterClass.RiskEvent event = RiskEventOuterClass.RiskEvent.parseFrom(outbox.getPayload());

//                     kafkaTemplate.send(TOPIC, event.getPortfolioId(), event).get();

//                     transactionTemplate.execute(status -> {
//                         outbox.setStatus("SENT");
//                         outbox.setUpdatedAt(LocalDateTime.now());
//                         outboxDao.save(outbox);
//                         return null;
//                     });

//                     log.info("Event {} sent to kafka successfully.",event);

//                 } catch (InvalidProtocolBufferException e) {
//                     log.error("Invalid protobuf payload for outbox {}", outbox.getAnalysisOutboxId(), e);

//                     transactionTemplate.execute(status -> {
//                         outbox.setStatus("FAILED");
//                         outbox.setUpdatedAt(LocalDateTime.now());
//                         outboxDao.save(outbox);
//                         return null;
//                     });
//                 } catch (InterruptedException e) {
//                     log.error("Interrupted exception while sending to kafka: ",e);

//                     Thread.currentThread().interrupt();
//                     rejectedPortfolios.add(outbox.getPortfolioId().toString());
//                     return;

//                 } catch (ExecutionException e) {

//                     log.error("Kafka send failed", e);
//                     rejectedPortfolios.add(outbox.getPortfolioId().toString());
//                 } catch (RuntimeException ex) {
//                     log.error("Failed while sending the message: " + ex.getMessage() + " with portfolio id: " + outbox.getPortfolioId());

//                     rejectedPortfolios.add(outbox.getPortfolioId().toString());
//                 }

//             }
//         } finally {
//             lockedPortfolios.forEach(portfolioId -> {
//                 advisoryLockService.releaseLock(portfolioId);
//                 log.info("Released the lock for portfolio {}.", portfolioId);
//             });
//         }
//     }
// }
