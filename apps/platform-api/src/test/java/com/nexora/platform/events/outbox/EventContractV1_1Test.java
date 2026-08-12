package com.nexora.platform.events.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.tenant.TenantContext;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventContractV1_1Test {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void matchesTheFrozenWorkflowDigestVectors() throws Exception {
        String payload = """
                {"resourceId":"30000000-0000-4000-8000-000000000001","resourceType":"page",
                "organizationId":"10000000-0000-4000-8000-000000000001",
                "subjectId":"90000000-0000-4000-8000-000000000001",
                "actorId":"80000000-0000-4000-8000-000000000001","eventVersion":1,
                "correlationId":"10000000000000000000000000000003",
                "traceId":"00000000000000000000000000000003",
                "receiptId":"a3000000-0000-4000-8000-000000000003","schemaVersion":"1.1.0",
                "safeDisplay":{"label":"WORKFLOW_TRANSITIONED","status":"IN_REVIEW","variant":"warning"}}
                """.replaceAll("\\s+", "");

        assertThat(EventContractV1_1.payloadDigest(JSON.readTree(payload)))
                .isEqualTo("sha256:13f3fdd664c416caf6f8f1bc3f49c9b865e7798e5283cb02eaf454c296fe57cb");
        assertThat(EventContractV1_1.idempotencyKeyDigest(
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "tenant:10000000-0000-4000-8000-000000000001:workflow",
                UUID.fromString("30000000-0000-4000-8000-000000000001"),
                "fixture-workflow-key-v1"))
                .isEqualTo("sha256:59fecb1023e1a92dfc232cbe65fbd339cc775664f8b46d577d876c8e447d5726");

        OutboxEvent fixture = new OutboxEvent(
                UUID.fromString("70000000-0000-4000-8000-000000000003"),
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                UUID.fromString("90000000-0000-4000-8000-000000000001"),
                UUID.fromString("80000000-0000-4000-8000-000000000001"), "page",
                UUID.fromString("30000000-0000-4000-8000-000000000001"), 1,
                "tenant:10000000-0000-4000-8000-000000000001:workflow", "WORKFLOW_TRANSITIONED", "1.1.0",
                "sha256:59fecb1023e1a92dfc232cbe65fbd339cc775664f8b46d577d876c8e447d5726",
                "sha256:13f3fdd664c416caf6f8f1bc3f49c9b865e7798e5283cb02eaf454c296fe57cb", payload,
                "00000000000000000000000000000003", Instant.parse("2026-08-10T00:10:00Z"), 0);
        EventContractV1_1.verifyForPublication(fixture);
    }

    @Test
    void archivesWithOnlyTheFixedV1_1SafePayloadCatalog() throws Exception {
        TenantContext actor = new TenantContext(
                UUID.fromString("90000000-0000-4000-8000-000000000001"),
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                UUID.fromString("91000000-0000-4000-8000-000000000001"), 1, "OWNER");
        EventContractV1_1.PreparedWorkflowEvent event = EventContractV1_1.archivedPage(
                actor, UUID.fromString("30000000-0000-4000-8000-000000000001"), 1);

        assertThat(event.traceId()).matches("[a-f0-9]{32}");
        Set<String> fields = new HashSet<>();
        JSON.readTree(event.safePayloadJson()).fieldNames().forEachRemaining(fields::add);
        assertThat(fields)
                .containsExactlyInAnyOrder(
                        "resourceId", "resourceType", "organizationId", "subjectId", "actorId", "eventVersion",
                        "traceId", "schemaVersion", "safeDisplay");
        assertThat(JSON.readTree(event.safePayloadJson()).path("safeDisplay").toString())
                .isEqualTo("{\"label\":\"WORKFLOW_TRANSITIONED\",\"status\":\"ARCHIVED\",\"variant\":\"neutral\"}");
        assertThat(event.payloadDigest()).isEqualTo(EventContractV1_1.payloadDigest(JSON.readTree(event.safePayloadJson())));
        EventContractV1_1.PreparedWorkflowEvent replay = EventContractV1_1.archivedPage(
                actor, UUID.fromString("30000000-0000-4000-8000-000000000001"), 1);
        assertThat(replay.traceId()).isEqualTo(event.traceId());
        assertThat(replay.payloadDigest()).isEqualTo(event.payloadDigest());
        assertThat(replay.idempotencyKeyDigest()).isEqualTo(event.idempotencyKeyDigest());
    }

    @Test
    void rejectsVersionsOutsideTheJcsSafeIntegerRange() {
        TenantContext actor = new TenantContext(
                UUID.fromString("90000000-0000-4000-8000-000000000001"),
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                UUID.fromString("91000000-0000-4000-8000-000000000001"), 1, "OWNER");

        assertThatIllegalArgumentException().isThrownBy(() -> EventContractV1_1.archivedPage(
                actor, UUID.fromString("30000000-0000-4000-8000-000000000001"), 9_007_199_254_740_992L));
    }
}
