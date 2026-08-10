package com.nexora.platform.events.outbox;

/** A transient transport failure; the database state machine decides retry and dead-lettering. */
public class OutboxTransportException extends RuntimeException {
    public OutboxTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
