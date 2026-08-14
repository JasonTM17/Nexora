package com.nexora.platform.events.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Bounded outbox observability required by Prompt Phase 14: backlog, oldest
 * pending age, failures and attempts. Counters aggregate over publisher passes;
 * gauges are refreshed on demand from the authoritative database so a metric
 * scrape never falls back to a stale in-memory claim about durable state.
 */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxMetrics {
    static final String PREFIX = "nexora_outbox_";
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong retrying = new AtomicLong();
    private final AtomicLong deadLettered = new AtomicLong();
    private final AtomicLong acknowledgementUncertain = new AtomicLong();
    private final OutboxEventRepository events;

    public OutboxMetrics(MeterRegistry registry, OutboxEventRepository events) {
        this.events = events;
        registry.counter(PREFIX + "published_total");
        registry.counter(PREFIX + "retrying_total");
        registry.counter(PREFIX + "dead_lettered_total");
        registry.counter(PREFIX + "acknowledgement_uncertain_total");
        Gauge.builder(PREFIX + "backlog", events, OutboxMetrics::backlog).register(registry);
        Gauge.builder(PREFIX + "oldest_pending_age_seconds", events, OutboxMetrics::oldestPendingAgeSeconds)
                .register(registry);
    }

    public void record(OutboxPublisher.PublishResult result) {
        published.addAndGet(result.published());
        retrying.addAndGet(result.retrying());
        deadLettered.addAndGet(result.deadLettered());
        acknowledgementUncertain.addAndGet(result.acknowledgementUncertain());
    }

    private static double backlog(OutboxEventRepository events) {
        return events.countPendingAndClaimed();
    }

    private static double oldestPendingAgeSeconds(OutboxEventRepository events) {
        return events.oldestPendingAgeSeconds();
    }
}
