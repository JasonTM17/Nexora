package com.nexora.platform.events.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexora.platform.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Narrow producer-side implementation of the frozen M3 event contract v1.1.
 *
 * <p>This packet owns only the {@code WORKFLOW_TRANSITIONED/page} producer. The
 * immutable database boundary independently validates routing and the safe
 * payload; this class makes those bytes deterministic before the same
 * transaction calls {@code record_outbox_event}.</p>
 */
public final class EventContractV1_1 {
    public static final String SCHEMA_VERSION = "1.1.0";
    public static final String EVENT_TYPE = "WORKFLOW_TRANSITIONED";
    public static final String RESOURCE_TYPE = "page";
    public static final String OPERATION = "workflow.transition";
    private static final String PAYLOAD_PREFIX = "nexora:event-payload:1.1\n";
    private static final String IDEMPOTENCY_PREFIX = "nexora:event-idempotency:1.1\n";
    private static final Pattern TRACE_ID = Pattern.compile("^[a-f0-9]{32}$");
    private static final Pattern DIGEST = Pattern.compile("^sha256:[a-f0-9]{64}$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private EventContractV1_1() {
    }

    public static PreparedWorkflowEvent archivedPage(
            TenantContext actor, UUID pageId, long pageVersion) {
        if (pageVersion <= 0) {
            throw new IllegalArgumentException("A positive workflow event version is required.");
        }
        String topic = "tenant:%s:workflow".formatted(actor.organizationId());
        String traceId = serverGeneratedTraceId();
        ObjectNode payload = JSON.createObjectNode();
        payload.put("resourceId", pageId.toString());
        payload.put("resourceType", RESOURCE_TYPE);
        payload.put("organizationId", actor.organizationId().toString());
        payload.put("subjectId", actor.subjectId().toString());
        payload.put("actorId", actor.subjectId().toString());
        payload.put("eventVersion", pageVersion);
        payload.put("traceId", traceId);
        payload.put("schemaVersion", SCHEMA_VERSION);
        ObjectNode display = payload.putObject("safeDisplay");
        display.put("label", EVENT_TYPE);
        display.put("status", "ARCHIVED");
        display.put("variant", "neutral");

        String opaqueIdempotencyKey = "cms-page-archive:%s:%d".formatted(pageId, pageVersion);
        String payloadJson = canonicalJson(payload);
        return new PreparedWorkflowEvent(
                topic,
                traceId,
                payloadJson,
                idempotencyKeyDigest(actor.organizationId(), topic, pageId, opaqueIdempotencyKey),
                payloadDigest(payload));
    }

    public static String payloadDigest(JsonNode safePayload) {
        return sha256(PAYLOAD_PREFIX + canonicalJson(safePayload));
    }

    public static String idempotencyKeyDigest(
            UUID organizationId, String topic, UUID resourceId, String opaqueIdempotencyKey) {
        ObjectNode key = JSON.createObjectNode();
        key.put("operation", OPERATION);
        key.put("organizationId", organizationId.toString());
        key.put("topic", topic);
        key.put("eventType", EVENT_TYPE);
        key.put("resourceType", RESOURCE_TYPE);
        key.put("resourceId", resourceId.toString());
        key.put("opaqueIdempotencyKey", opaqueIdempotencyKey);
        return sha256(IDEMPOTENCY_PREFIX + canonicalJson(key));
    }

    /** Rejects malformed stored data before it can cross the JetStream boundary. */
    public static void verifyForPublication(OutboxEvent event) {
        if (!SCHEMA_VERSION.equals(event.schemaVersion())
                || !EVENT_TYPE.equals(event.eventType())
                || !RESOURCE_TYPE.equals(event.resourceType())
                || !TRACE_ID.matcher(event.traceId()).matches()
                || !DIGEST.matcher(event.idempotencyKeyDigest()).matches()
                || !DIGEST.matcher(event.payloadDigest()).matches()
                || !event.topic().equals("tenant:%s:workflow".formatted(event.organizationId()))) {
            throw new IllegalArgumentException("The outbox event does not satisfy contract v1.1.");
        }
        try {
            JsonNode payload = JSON.readTree(event.safePayloadJson());
            if (!payload.isObject()
                    || !event.payloadDigest().equals(payloadDigest(payload))
                    || !payloadMatchesEnvelope(payload, event)) {
                throw new IllegalArgumentException("The outbox event does not satisfy contract v1.1.");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("The outbox event does not satisfy contract v1.1.", exception);
        }
    }

    static String canonicalJson(JsonNode value) {
        StringBuilder output = new StringBuilder();
        appendCanonical(value, output);
        return output.toString();
    }

    private static boolean payloadMatchesEnvelope(JsonNode payload, OutboxEvent event) {
        List<String> fields = new ArrayList<>();
        payload.fieldNames().forEachRemaining(fields::add);
        fields.sort(String::compareTo);
        List<String> expected = new ArrayList<>(List.of(
                "resourceId", "resourceType", "organizationId", "subjectId", "actorId", "eventVersion",
                "traceId", "schemaVersion", "safeDisplay"));
        expected.sort(String::compareTo);
        if (!fields.equals(expected)) {
            return false;
        }
        JsonNode display = payload.path("safeDisplay");
        return payload.path("resourceId").asText().equals(event.resourceId().toString())
                && payload.path("resourceType").asText().equals(event.resourceType())
                && payload.path("organizationId").asText().equals(event.organizationId().toString())
                && payload.path("subjectId").asText().equals(event.subjectId().toString())
                && payload.path("actorId").asText().equals(event.actorId().toString())
                && payload.path("eventVersion").canConvertToLong()
                && payload.path("eventVersion").asLong() == event.eventVersion()
                && payload.path("traceId").asText().equals(event.traceId())
                && payload.path("schemaVersion").asText().equals(event.schemaVersion())
                && display.isObject()
                && display.size() == 3
                && EVENT_TYPE.equals(display.path("label").asText())
                && "ARCHIVED".equals(display.path("status").asText())
                && "neutral".equals(display.path("variant").asText());
    }

    private static void appendCanonical(JsonNode value, StringBuilder output) {
        if (value.isObject()) {
            output.append('{');
            List<String> names = new ArrayList<>();
            value.fieldNames().forEachRemaining(names::add);
            names.sort(Comparator.naturalOrder());
            for (int index = 0; index < names.size(); index++) {
                if (index > 0) {
                    output.append(',');
                }
                appendQuoted(names.get(index), output);
                output.append(':');
                appendCanonical(value.get(names.get(index)), output);
            }
            output.append('}');
            return;
        }
        if (value.isIntegralNumber()) {
            output.append(value.bigIntegerValue());
            return;
        }
        if (value.isTextual()) {
            appendQuoted(value.textValue(), output);
            return;
        }
        throw new IllegalArgumentException("The event contract permits only objects, strings, and integral versions.");
    }

    private static void appendQuoted(String value, StringBuilder output) {
        if (hasLoneSurrogate(value)) {
            throw new IllegalArgumentException("A valid I-JSON string is required.");
        }
        try {
            output.append(JSON.writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("A valid I-JSON string is required.", exception);
        }
    }

    private static boolean hasLoneSurrogate(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(++index))) {
                    return true;
                }
            } else if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }

    private static String serverGeneratedTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String sha256(String canonicalBytes) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalBytes.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the event contract.", exception);
        }
    }

    public record PreparedWorkflowEvent(
            String topic,
            String traceId,
            String safePayloadJson,
            String idempotencyKeyDigest,
            String payloadDigest) {
    }
}
