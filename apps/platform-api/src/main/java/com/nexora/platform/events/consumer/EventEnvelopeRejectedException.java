package com.nexora.platform.events.consumer;

/** Raised before durable recording when a raw event cannot satisfy contract v1.1. */
public final class EventEnvelopeRejectedException extends RuntimeException {
    public EventEnvelopeRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventEnvelopeRejectedException(String message) {
        super(message);
    }
}
