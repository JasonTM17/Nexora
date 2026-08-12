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
import java.util.Map;
import java.util.Set;
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
    private static final long MAX_JCS_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final String PAYLOAD_PREFIX = "nexora:event-payload:1.1\n";
    private static final String IDEMPOTENCY_PREFIX = "nexora:event-idempotency:1.1\n";
    private static final Pattern TRACE_ID = Pattern.compile("^[a-f0-9]{32}$");
    private static final Pattern UUID_TEXT = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final Pattern DIGEST = Pattern.compile("^sha256:[a-f0-9]{64}$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private EventContractV1_1() {
    }

    public static PreparedWorkflowEvent archivedPage(
            TenantContext actor, UUID pageId, long pageVersion) {
        if (pageVersion <= 0 || pageVersion > MAX_JCS_SAFE_INTEGER) {
            throw new IllegalArgumentException("A JCS-safe positive workflow event version is required.");
        }
        String topic = "tenant:%s:workflow".formatted(actor.organizationId());
        String opaqueIdempotencyKey = "cms-page-archive:%s:%d".formatted(pageId, pageVersion);
        String idempotencyKeyDigest = idempotencyKeyDigest(actor.organizationId(), topic, pageId, opaqueIdempotencyKey);
        String traceId = serverDerivedTraceId(idempotencyKeyDigest);
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

        String payloadJson = canonicalJson(payload);
        return new PreparedWorkflowEvent(
                topic,
                traceId,
                payloadJson,
                idempotencyKeyDigest,
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
                || !isKnownEventType(event.eventType())
                || event.eventVersion() <= 0
                || event.eventVersion() > MAX_JCS_SAFE_INTEGER
                || !TRACE_ID.matcher(event.traceId()).matches()
                || !DIGEST.matcher(event.idempotencyKeyDigest()).matches()
                || !DIGEST.matcher(event.payloadDigest()).matches()
                || !routeMatches(event)) {
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
        if (event.eventType().equals("JOB_PROGRESS_CHANGED")) {
            expected.add("jobState");
            expected.add("progress");
        }
        if (payload.has("correlationId")) {
            expected.add("correlationId");
        }
        if (payload.has("receiptId")) {
            expected.add("receiptId");
        }
        expected.sort(String::compareTo);
        if (!fields.equals(expected)) {
            return false;
        }
        JsonNode display = payload.path("safeDisplay");
        boolean baseIds = payload.path("resourceId").isTextual()
                && payload.path("organizationId").isTextual()
                && payload.path("subjectId").isTextual()
                && payload.path("actorId").isTextual()
                && UUID_TEXT.matcher(payload.path("resourceId").asText()).matches()
                && UUID_TEXT.matcher(payload.path("organizationId").asText()).matches()
                && UUID_TEXT.matcher(payload.path("subjectId").asText()).matches()
                && UUID_TEXT.matcher(payload.path("actorId").asText()).matches();
        boolean jobPayload = event.eventType().equals("JOB_PROGRESS_CHANGED")
                && payload.path("jobState").isTextual()
                && Set.of("QUEUED", "RUNNING", "COMPLETED", "FAILED", "CANCELED").contains(payload.path("jobState").asText())
                && payload.path("progress").isIntegralNumber()
                && payload.path("progress").asInt() >= 0
                && payload.path("progress").asInt() <= 100;
        boolean nonJobPayload = !event.eventType().equals("JOB_PROGRESS_CHANGED")
                && !payload.has("jobState") && !payload.has("progress");
        boolean optionalPayload = (!payload.has("correlationId") || TRACE_ID.matcher(payload.path("correlationId").asText()).matches())
                && (!payload.has("receiptId") || UUID_TEXT.matcher(payload.path("receiptId").asText()).matches());
        return baseIds
                && payload.path("resourceId").asText().equals(event.resourceId().toString())
                && payload.path("resourceType").asText().equals(event.resourceType())
                && payload.path("organizationId").asText().equals(event.organizationId().toString())
                && payload.path("subjectId").asText().equals(event.subjectId().toString())
                && payload.path("actorId").asText().equals(event.actorId().toString())
                && payload.path("eventVersion").canConvertToLong()
                && payload.path("eventVersion").asLong() == event.eventVersion()
                && event.eventVersion() > 0
                && event.eventVersion() <= MAX_JCS_SAFE_INTEGER
                && payload.path("traceId").asText().equals(event.traceId())
                && payload.path("schemaVersion").asText().equals(event.schemaVersion())
                && display.isObject()
                && display.size() == 3
                && display.path("label").isTextual()
                && display.path("status").isTextual()
                && display.path("variant").isTextual()
                && event.eventType().equals(display.path("label").asText())
                && displayAllowed(event.eventType(), display.path("status").asText(), display.path("variant").asText())
                && (jobPayload || nonJobPayload)
                && optionalPayload;
    }

    private static boolean isKnownEventType(String eventType) {
        return Map.of(
                "PUBLICATION_INVALIDATED", "page",
                "WORKFLOW_TRANSITIONED", "page",
                "JOB_PROGRESS_CHANGED", "job",
                "NOTIFICATION_ENQUEUED", "notification",
                "PRESENCE_CHANGED", "collaboration_session",
                "OUTBOX_RECORDED", "outbox").containsKey(eventType);
    }

    private static boolean routeMatches(OutboxEvent event) {
        String scope;
        String purpose;
        boolean tenant;
        switch (event.eventType()) {
            case "PUBLICATION_INVALIDATED" -> { scope = "tenant"; purpose = "publication"; tenant = true; }
            case "WORKFLOW_TRANSITIONED" -> { scope = "tenant"; purpose = "workflow"; tenant = true; }
            case "JOB_PROGRESS_CHANGED" -> { scope = "resource"; purpose = "job-progress"; tenant = false; }
            case "NOTIFICATION_ENQUEUED" -> { scope = "tenant"; purpose = "notification"; tenant = true; }
            case "PRESENCE_CHANGED" -> { scope = "resource"; purpose = "presence"; tenant = false; }
            case "OUTBOX_RECORDED" -> { scope = "tenant"; purpose = "outbox"; tenant = true; }
            default -> { return false; }
        }
        String expectedResourceType = Map.of(
                "PUBLICATION_INVALIDATED", "page", "WORKFLOW_TRANSITIONED", "page",
                "JOB_PROGRESS_CHANGED", "job", "NOTIFICATION_ENQUEUED", "notification",
                "PRESENCE_CHANGED", "collaboration_session", "OUTBOX_RECORDED", "outbox")
                .get(event.eventType());
        String owner = tenant ? event.organizationId().toString() : event.resourceId().toString();
        return event.resourceType().equals(expectedResourceType)
                && event.topic().equals(scope + ":" + owner + ":" + purpose);
    }

    private static boolean displayAllowed(String eventType, String status, String variant) {
        return switch (eventType) {
            case "PUBLICATION_INVALIDATED" -> Set.of("QUEUED:warning", "PUBLISHED:success", "ARCHIVED:neutral", "INVALIDATED:danger").contains(status + ":" + variant);
            case "WORKFLOW_TRANSITIONED" -> Set.of("PENDING:info", "IN_REVIEW:warning", "PUBLISHED:success", "ARCHIVED:neutral", "FAILED:danger").contains(status + ":" + variant);
            case "JOB_PROGRESS_CHANGED" -> Set.of("QUEUED:info", "RUNNING:warning", "COMPLETED:success", "FAILED:danger", "CANCELED:neutral").contains(status + ":" + variant);
            case "NOTIFICATION_ENQUEUED" -> Set.of("QUEUED:info", "DELIVERED:success", "FAILED:danger").contains(status + ":" + variant);
            case "PRESENCE_CHANGED" -> Set.of("ACTIVE:success", "INACTIVE:neutral").contains(status + ":" + variant);
            case "OUTBOX_RECORDED" -> Set.of("PENDING:info", "CLAIMED:warning", "PUBLISHED:success", "FAILED:danger", "DEAD_LETTER:danger").contains(status + ":" + variant);
            default -> false;
        };
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
            if (value.bigIntegerValue().abs().compareTo(java.math.BigInteger.valueOf(MAX_JCS_SAFE_INTEGER)) > 0) {
                throw new IllegalArgumentException("A JCS-safe integer is required.");
            }
            output.append(value.longValue());
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

    private static String serverDerivedTraceId(String idempotencyKeyDigest) {
        return sha256("nexora:event-trace:1.1\n" + idempotencyKeyDigest).substring("sha256:".length(), 39);
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
