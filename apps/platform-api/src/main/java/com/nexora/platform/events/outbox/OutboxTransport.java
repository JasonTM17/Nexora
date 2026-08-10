package com.nexora.platform.events.outbox;

/** Transport acknowledgement must complete before the database event is marked PUBLISHED. */
public interface OutboxTransport {
    void publish(OutboxEvent event);
}
