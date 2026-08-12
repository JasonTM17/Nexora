package com.nexora.platform.events.outbox;

/** A non-transient contract violation that must remain visible in the outbox failure ledger. */
public final class OutboxContractViolationException extends RuntimeException {
    public OutboxContractViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
