package com.nexora.platform.observability;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Shared trace policy for HTTP, durable audit, and outbox envelopes. The
 * forbidden-value rule intentionally matches V014 safe-payload admission.
 */
public final class TraceIdPolicy {
    private static final Pattern SAFE_CHARACTERS = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final Pattern FORBIDDEN_VALUE = Pattern.compile(
            "(?i)(authorization|bearer|token|secret|password|cookie|provider|prompt|private[ _-]?key|"
                    + "access[ _-]?token|api[ _-]?key|pii|email|phone|body|raw|html|document)");

    private TraceIdPolicy() {
    }

    public static String acceptedOrGenerated(String requestedTraceId) {
        return isSafe(requestedTraceId) ? requestedTraceId : UUID.randomUUID().toString();
    }

    public static boolean isSafe(String traceId) {
        return traceId != null
                && SAFE_CHARACTERS.matcher(traceId).matches()
                && !FORBIDDEN_VALUE.matcher(traceId).find();
    }
}
