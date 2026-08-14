package com.nexora.platform.events.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Opt-in durable subscription wiring for the M3-T05 persistence consumer.
 * Disabled by default so no background network activity exists without an
 * explicit deployment decision. The event route vocabulary remains owned by
 * the event contract; this boundary only consumes the frozen envelopes.
 */
@ConfigurationProperties("nexora.events.consumer")
public record EventConsumerProperties(
        boolean enabled,
        String natsUrl,
        String stream,
        String subject,
        String durable,
        String description) {

    public EventConsumerProperties {
        natsUrl = natsUrl == null || natsUrl.isBlank() ? "nats://127.0.0.1:14222" : natsUrl;
        stream = stream == null || stream.isBlank() ? "NEXORA_EVENTS" : stream;
        subject = subject == null || subject.isBlank() ? "nexora.events.workflow" : subject;
        durable = durable == null || durable.isBlank() ? "platform-api-event-ledger" : durable;
        description = description == null || description.isBlank() ? "Nexora platform event ledger" : description;
    }
}
