package com.pms.analytics.publisher;

import com.pms.analytics.dao.AnalysisOutboxDao;
import com.pms.analytics.dao.entity.AnalysisOutbox;
import com.pms.analytics.dto.RiskEventOuterClass;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, RiskEventOuterClass.RiskEvent> kafkaTemplate;
    private final AnalysisOutboxDao outboxDao;

    private static final String TOPIC = "portfolio-risk-metrics";
    private static final int BATCH_SIZE = 200;


    @Scheduled(fixedRate = 5000)
    @Transactional
    public void publishPendingEvents() {

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        List<AnalysisOutbox> events = outboxDao.findTopPendingForUpdate(pageable);
        if (events.isEmpty()) return;

        for (AnalysisOutbox outbox : events) {

            try {
                RiskEventOuterClass.RiskEvent event = RiskEventOuterClass.RiskEvent.parseFrom(outbox.getPayload());

                kafkaTemplate.send(TOPIC, outbox.getPortfolioId().toString(), event)
                        .whenComplete((SendResult<String, RiskEventOuterClass.RiskEvent> result, Throwable ex) -> {
                            if (ex == null) {
                                outbox.setStatus("SENT");
                                outboxDao.save(outbox);
                                System.out.println("Published: " + outbox.getPortfolioId());
                            } else {
                                // Keep as PENDING for automatic retry
                                System.err.println("Failed to publish, will retry: " + outbox.getPortfolioId()
                                        + " | Reason: " + ex.getMessage());
                            }
                        });

            } catch (Exception e) {

                System.err.println("Failed to parse proto, will retry: " + outbox.getPortfolioId()
                        + " | Reason: " + e.getMessage());
            }
        }
    }
}
