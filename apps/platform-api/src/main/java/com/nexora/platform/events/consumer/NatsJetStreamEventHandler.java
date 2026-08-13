package com.nexora.platform.events.consumer;

import io.nats.client.Message;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * One durable JetStream delivery. M3-R01 owns subscription and cadence
 * wiring; this boundary persists before ACK and makes malformed envelopes
 * terminal so an unsafe message cannot consume a redelivery budget.
 */
public final class NatsJetStreamEventHandler {
    private static final Duration ACK_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);
    private final EventLedgerConsumer consumer;

    public NatsJetStreamEventHandler(EventLedgerConsumer consumer) {
        this.consumer = consumer;
    }

    public EventLedgerReceipt consumeAndAck(Message message) {
        if (!message.isJetStream()) {
            throw new IllegalArgumentException("A durable JetStream delivery is required.");
        }
        try {
            EventLedgerReceipt receipt = consumer.consume(message.getData());
            message.ackSync(ACK_TIMEOUT);
            return receipt;
        } catch (EventEnvelopeRejectedException exception) {
            message.term();
            throw exception;
        } catch (TimeoutException exception) {
            message.nakWithDelay(RETRY_DELAY);
            throw new IllegalStateException("JetStream did not confirm the event acknowledgement.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            message.nakWithDelay(RETRY_DELAY);
            throw new IllegalStateException("JetStream event acknowledgement was interrupted.", exception);
        } catch (RuntimeException exception) {
            message.nakWithDelay(RETRY_DELAY);
            throw exception;
        }
    }
}
