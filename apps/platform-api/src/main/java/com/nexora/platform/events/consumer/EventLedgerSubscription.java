package com.nexora.platform.events.consumer;

import io.nats.client.Connection;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Opt-in durable subscription that drives the persist-before-ack handler for
 * the frozen event stream. A failed delivery is nacked with a bounded delay by
 * the handler; malformed envelopes are terminated there. This component owns
 * subscription lifecycle only and never changes the event vocabulary.
 */
@Component
@Profile("database")
@ConditionalOnProperty(prefix = "nexora.events.consumer", name = "enabled", havingValue = "true")
public class EventLedgerSubscription implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(EventLedgerSubscription.class);
    private static final Duration ACK_WAIT = Duration.ofSeconds(3);
    private static final int MAX_DELIVER = 5;

    private final EventConsumerProperties properties;
    private final NatsJetStreamEventHandler handler;
    private final JetStreamSubscription subscription;
    private final ExecutorService dispatcher;

    public EventLedgerSubscription(
            EventConsumerProperties properties,
            NatsJetStreamEventHandler handler,
            Connection connection) throws Exception {
        this.properties = properties;
        this.handler = handler;
        PushSubscribeOptions options = ConsumerConfiguration.builder()
                .durable(properties.durable())
                .deliverPolicy(DeliverPolicy.New)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(ACK_WAIT)
                .maxDeliver(MAX_DELIVER)
                .buildPushSubscribeOptions();
        this.subscription = connection.jetStream().subscribe(properties.subject(), options);
        this.dispatcher = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "nexora-event-ledger-consumer");
            thread.setDaemon(true);
            return thread;
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        dispatcher.submit(this::dispatch);
        LOG.info("event ledger consumer subscribed to {} on stream {}",
                properties.subject(), properties.stream());
    }

    private void dispatch() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message message = subscription.nextMessage(Duration.ofSeconds(5));
                if (message == null) {
                    continue;
                }
                try {
                    handler.consumeAndAck(message);
                } catch (RuntimeException failure) {
                    LOG.warn("event ledger delivery failed; redelivery or terminal handling applies",
                            failure);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException failure) {
                LOG.error("event ledger subscription loop failed", failure);
                break;
            }
        }
    }

    @PreDestroy
    @Override
    public void close() {
        dispatcher.shutdownNow();
        try {
            dispatcher.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        subscription.unsubscribe();
    }
}
