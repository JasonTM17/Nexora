package com.nexora.platform.events.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Opt-in publisher trigger. It exists only when the deployment explicitly
 * enables the publisher; local and default profiles never create this bean.
 */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxPublisherScheduler {
    private final OutboxPublisher publisher;
    private final OutboxMetrics metrics;

    public OutboxPublisherScheduler(OutboxPublisher publisher, OutboxMetrics metrics) {
        this.publisher = publisher;
        this.metrics = metrics;
    }

    @Scheduled(
            initialDelayString = "${nexora.outbox.publisher.initial-delay-millis:1000}",
            fixedDelayString = "${nexora.outbox.publisher.poll-delay-millis:5000}")
    void publishAvailable() {
        metrics.record(publisher.publishAvailable());
    }
}
