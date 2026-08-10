package com.nexora.platform.events.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Disabled by default so M3-T02 introduces no background network activity.
 * M3-R01 owns environment wiring; local integration tests opt in explicitly.
 */
@ConfigurationProperties("nexora.outbox.publisher")
public record OutboxPublisherProperties(
        boolean enabled,
        String natsUrl,
        String subject,
        String claimOwner,
        Duration claimLease,
        int batchSize) {

    public OutboxPublisherProperties {
        natsUrl = natsUrl == null || natsUrl.isBlank() ? "nats://127.0.0.1:14222" : natsUrl;
        subject = subject == null || subject.isBlank() ? "nexora.events.workflow" : subject;
        claimOwner = claimOwner == null || claimOwner.isBlank() ? "platform-api-outbox" : claimOwner;
        claimLease = claimLease == null ? Duration.ofSeconds(30) : claimLease;
        batchSize = batchSize == 0 ? 25 : batchSize;
    }
}
