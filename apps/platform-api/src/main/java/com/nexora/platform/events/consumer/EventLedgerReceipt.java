package com.nexora.platform.events.consumer;

import java.util.UUID;

/** Database-owned durable receipt for one accepted event envelope. */
public record EventLedgerReceipt(UUID eventId, boolean duplicate) {
}
