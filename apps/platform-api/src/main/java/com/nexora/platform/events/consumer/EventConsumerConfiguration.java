package com.nexora.platform.events.consumer;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Opt-in NATS connection factory for the durable event-ledger subscription.
 * The connection is created only when the consumer is enabled; the default
 * local and database runtimes keep no NATS dependency.
 */
@Configuration
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.events.consumer", name = "enabled", havingValue = "true")
public class EventConsumerConfiguration {

    @Bean(destroyMethod = "close")
    Connection eventLedgerConnection(EventConsumerProperties properties) throws Exception {
        return Nats.connect(properties.natsUrl());
    }
}
