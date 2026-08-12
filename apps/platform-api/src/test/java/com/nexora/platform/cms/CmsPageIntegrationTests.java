package com.nexora.platform.cms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.auth.LocalJwtIssuer;
import com.nexora.platform.events.outbox.OutboxEvent;
import com.nexora.platform.events.outbox.OutboxEventRepository;
import com.nexora.platform.events.outbox.OutboxPublisher;
import com.nexora.platform.events.outbox.OutboxPublisherProperties;
import com.nexora.platform.events.outbox.OutboxTransport;
import com.nexora.platform.events.outbox.OutboxContractViolationException;
import com.nexora.platform.events.outbox.EventContractV1_1;
import com.nexora.platform.events.outbox.CmsWorkflowOutboxRecorder;
import com.nexora.platform.events.consumer.EventEnvelopeRejectedException;
import com.nexora.platform.events.consumer.EventLedgerConsumer;
import com.nexora.platform.events.consumer.EventLedgerReceipt;
import com.nexora.platform.events.consumer.NatsJetStreamEventHandler;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.nats.client.Nats;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;

@ActiveProfiles("database")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CmsPageIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_cms_runtime_login";
    private static final String RUNTIME_PASSWORD = "test-cms-runtime-login";
    private static final String REALTIME_JWT_SECRET = "test-realtime-descriptor-secret-for-m3-t03";
    private static final LocalJwtIssuer ISSUER = new LocalJwtIssuer();
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("nexora_cms")
            .withUsername("postgres")
            .withPassword("postgres");
    private static final GenericContainer<?> NATS = new GenericContainer<>("nats:2.11.0-alpine")
            .withCommand("-js", "-sd", "/data")
            .withExposedPorts(4222);
    private static final String OUTBOX_STREAM = "NEXORA_EVENTS";
    private static final String OUTBOX_SUBJECT = "nexora.events.workflow";
    private static Path migrationDirectory;

    @Autowired
    private CmsPageService pages;

    @Autowired
    private OutboxPublisher publisher;

    @Autowired
    private OutboxEventRepository outboxEvents;

    @Autowired
    private OutboxPublisherProperties outboxProperties;

    @Autowired
    private OutboxTransport outboxTransport;

    @Autowired
    private CmsWorkflowOutboxRecorder outboxRecorder;

    @Autowired
    private EventLedgerConsumer eventLedger;

    @Autowired
    private TenantContextService tenantContexts;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.start();
        NATS.start();
        prepareRuntimeRole();
        prepareMigrations();
        prepareOutboxStream();
        registry.add("NEXORA_RUNTIME_DATABASE_URL", DATABASE::getJdbcUrl);
        registry.add("NEXORA_RUNTIME_DATABASE_USERNAME", () -> RUNTIME_LOGIN);
        registry.add("NEXORA_RUNTIME_DATABASE_PASSWORD", () -> RUNTIME_PASSWORD);
        registry.add("NEXORA_MIGRATION_DATABASE_URL", DATABASE::getJdbcUrl);
        registry.add("NEXORA_MIGRATION_DATABASE_USERNAME", DATABASE::getUsername);
        registry.add("NEXORA_MIGRATION_DATABASE_PASSWORD", DATABASE::getPassword);
        registry.add("NEXORA_MIGRATIONS_LOCATION", () -> migrationDirectory.toString());
        registry.add("NEXORA_AUTH_ISSUER", ISSUER::issuer);
        registry.add("NEXORA_AUTH_JWKS_URI", ISSUER::jwksUri);
        registry.add("nexora.outbox.publisher.enabled", () -> "true");
        registry.add("nexora.outbox.publisher.nats-url", CmsPageIntegrationTests::natsUrl);
        registry.add("nexora.outbox.publisher.subject", () -> OUTBOX_SUBJECT);
        registry.add("nexora.outbox.publisher.initial-delay-millis", () -> "600000");
        registry.add("nexora.outbox.publisher.poll-delay-millis", () -> "600000");
        registry.add("nexora.realtime.descriptor.jwt-secret", () -> REALTIME_JWT_SECRET);
        registry.add("nexora.realtime.descriptor.ttl-seconds", () -> "120");
    }

    @AfterAll
    static void stopFixtures() throws Exception {
        ISSUER.close();
        NATS.stop();
        DATABASE.stop();
        if (migrationDirectory != null) {
            try (Stream<Path> paths = Files.walk(migrationDirectory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Test
    void createsUpdatesListsAndAuditsDraftsWithoutWritingImmutablePublicationHistory() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView created = pages.create(tenant.ownerContext(), create(tenant, "welcome"), "cms-create-1");
        CmsPageService.PageView updated = pages.update(tenant.ownerContext(), created.pageId(),
                update(tenant, 1, "Welcome revised"), "cms-update-1");
        CmsPageService.PageList listed = pages.list(tenant.ownerContext(), null, 25);

        assertThat(created.state()).isEqualTo("DRAFT");
        assertThat(updated.draftVersion()).isEqualTo(2);
        assertThat(updated.title()).isEqualTo("Welcome revised");
        assertThat(listed.items()).extracting(CmsPageService.PageSummary::pageId).containsExactly(created.pageId());
        assertThat(listed.nextCursor()).isNull();
        assertThat(count("SELECT count(*) FROM nexora.cms_audit_events WHERE organization_id = '"
                + tenant.organizationId() + "' AND operation IN ('PAGE_CREATE', 'PAGE_UPDATE')")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM nexora.page_versions WHERE organization_id = '"
                + tenant.organizationId() + "'")).isZero();
        assertThat(count("SELECT count(*) FROM nexora.page_publications WHERE organization_id = '"
                + tenant.organizationId() + "'")).isZero();

        assertThatThrownBy(() -> pages.update(tenant.ownerContext(), created.pageId(),
                update(tenant, 1, "Lost update"), "cms-stale-1"))
                .isInstanceOf(DomainAccessException.class)
                .extracting(exception -> ((DomainAccessException) exception).code())
                .isEqualTo("VERSION_CONFLICT");
    }

    @Test
    void rejectsCrossTenantAccessInvalidThemeAndDraftArchiveBeforeAnyHistoryMutation() throws Exception {
        CmsFixture alpha = seedTenant();
        CmsFixture beta = seedTenant();
        CmsPageService.PageView alphaPage = pages.create(alpha.ownerContext(), create(alpha, "alpha-page"), "cms-alpha-1");

        assertThatThrownBy(() -> pages.get(beta.ownerContext(), alphaPage.pageId()))
                .isInstanceOf(DomainAccessException.class)
                .extracting(exception -> ((DomainAccessException) exception).code())
                .isEqualTo("PERMISSION_DENIED");
        assertThatThrownBy(() -> pages.create(alpha.ownerContext(), new CmsPageService.CreateCommand(
                alpha.siteId(), "bad-theme", "Bad theme", "1.0.0", digest('b'), UUID.randomUUID(), seo()), "cms-theme-1"))
                .isInstanceOf(DomainAccessException.class)
                .extracting(exception -> ((DomainAccessException) exception).code())
                .isEqualTo("THEME_REFERENCE_INVALID");
        assertThatThrownBy(() -> pages.archive(alpha.ownerContext(), alphaPage.pageId(), 1, "cms-archive-1"))
                .isInstanceOf(DomainAccessException.class)
                .extracting(exception -> ((DomainAccessException) exception).code())
                .isEqualTo("WORKFLOW_TRANSITION_DENIED");
        assertThat(count("SELECT count(*) FROM nexora.page_versions WHERE organization_id = '"
                + alpha.organizationId() + "'")).isZero();
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '"
                + alphaPage.pageId() + "'")).isZero();
    }

    @Test
    void cursorUsesTheLastReturnedPageWithoutGapsOrDuplicates() throws Exception {
        CmsFixture tenant = seedTenant();
        for (int index = 1; index <= 26; index++) {
            pages.create(tenant.ownerContext(), create(tenant, "page-" + String.format("%02d", index)),
                    "cms-page-" + index);
        }

        CmsPageService.PageList first = pages.list(tenant.ownerContext(), null, 10);
        CmsPageService.PageList second = pages.list(tenant.ownerContext(), first.nextCursor(), 10);
        CmsPageService.PageList third = pages.list(tenant.ownerContext(), second.nextCursor(), 10);
        List<UUID> seen = new java.util.ArrayList<>();
        for (CmsPageService.PageList page : List.of(first, second, third)) {
            seen.addAll(page.items().stream().map(CmsPageService.PageSummary::pageId).toList());
        }

        assertThat(first.items()).hasSize(10);
        assertThat(second.items()).hasSize(10);
        assertThat(third.items()).hasSize(6);
        assertThat(first.nextCursor()).isEqualTo(first.items().getLast().pageId().toString());
        assertThat(second.nextCursor()).isEqualTo(second.items().getLast().pageId().toString());
        assertThat(third.nextCursor()).isNull();
        assertThat(new HashSet<>(seen)).hasSize(26);
    }

    @Test
    void httpDraftResponseOmitsUnsetOptionalFields() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView draft = pages.create(tenant.ownerContext(), create(tenant, "http-draft"),
                "cms-http-draft-1");

        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + port + "/api/v1/cms/pages/" + draft.pageId()))
                .header("Authorization", "Bearer " + ISSUER.token(tenant.ownerSubjectId(), Instant.now().plusSeconds(60)))
                .header("X-Nexora-Organization-Id", tenant.organizationId().toString())
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.has("publishedVersionId")).isFalse();
        assertThat(body.path("seo").path("openGraph").has("imageAssetId")).isFalse();
        assertThat(body.path("seo").path("twitter").has("imageAssetId")).isFalse();
        assertThat(body.path("seo").path("canonicalPath").asText()).isEqualTo("/welcome");
    }

    @Test
    void httpArchiveRequiresPublishPermissionAndAuditsWorkflow() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "archive-page"),
                "cms-archive-create-1");
        publish(tenant, page.pageId());
        UUID contributor = UUID.randomUUID();
        addMembership(tenant, contributor, "CONTENT_CREATOR");

        HttpResponse<String> denied = archive(tenant.organizationId(), contributor, page.pageId(), 1);

        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(json.readTree(denied.body()).path("code").asText()).isEqualTo("PERMISSION_DENIED");
        setMembershipRole(tenant, contributor, "REVIEWER");

        HttpResponse<String> allowed = archive(tenant.organizationId(), contributor, page.pageId(), 1);

        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(json.readTree(allowed.body()).path("state").asText()).isEqualTo("ARCHIVED");
        assertThat(pages.get(tenant.ownerContext(), page.pageId()).draftVersion()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM nexora.cms_audit_events WHERE page_id = '" + page.pageId()
                + "' AND operation = 'WORKFLOW'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM nexora.cms_audit_events WHERE page_id = '" + page.pageId()
                + "' AND operation = 'PAGE_UPDATE'")).isZero();
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE organization_id = '" + tenant.organizationId()
                + "' AND resource_id = '" + page.pageId() + "' AND event_type = 'WORKFLOW_TRANSITIONED'"
                + " AND state = 'PENDING' AND attempt_count = 0")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND topic = 'tenant:" + tenant.organizationId() + ":workflow'"
                + " AND safe_payload ->> 'resourceType' = 'page'"
                + " AND safe_payload -> 'safeDisplay' ->> 'status' = 'ARCHIVED'"))
                .isEqualTo(1);
    }

    @Test
    void publishesClaimedWorkflowEventsToJetStreamBeforeAcknowledgingTheOutbox() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "outbox-page"),
                "cms-outbox-create-1");
        publish(tenant, page.pageId());
        String traceId = "cms-outbox-archive-1";
        pages.archive(tenant.ownerContext(), page.pageId(), 1, traceId);
        long messagesBefore = streamMessageCount();

        OutboxPublisher.PublishResult result = publisher.publishAvailable();

        assertThat(result.claimed()).isGreaterThanOrEqualTo(1);
        assertThat(result.published()).isGreaterThanOrEqualTo(1);
        assertThat(result.acknowledgementUncertain()).isZero();
        assertThat(streamMessageCount()).isGreaterThan(messagesBefore);
        JsonNode envelope = latestStreamMessage();
        assertThat(envelope.path("eventType").asText()).isEqualTo("WORKFLOW_TRANSITIONED");
        assertThat(envelope.path("eventVersion").asLong()).isEqualTo(1);
        assertThat(envelope.path("organizationId").asText()).isEqualTo(tenant.organizationId().toString());
        assertThat(envelope.path("subjectId").asText()).isEqualTo(tenant.ownerSubjectId().toString());
        assertThat(envelope.path("actorId").asText()).isEqualTo(tenant.ownerSubjectId().toString());
        assertThat(envelope.path("resourceType").asText()).isEqualTo("page");
        assertThat(envelope.path("resourceId").asText()).isEqualTo(page.pageId().toString());
        assertThat(envelope.path("topic").asText()).isEqualTo("tenant:%s:workflow".formatted(tenant.organizationId()));
        assertThat(envelope.path("schemaVersion").asText()).isEqualTo("1.1.0");
        assertThat(envelope.path("traceId").asText()).matches("[a-f0-9]{32}");
        assertThat(envelope.path("traceId").asText()).isNotEqualTo(traceId);
        assertThat(envelope.path("idempotencyKeyDigest").asText()).isEqualTo(EventContractV1_1.idempotencyKeyDigest(
                tenant.organizationId(), envelope.path("topic").asText(), page.pageId(),
                "cms-page-archive:%s:%d".formatted(page.pageId(), 1)));
        assertThat(envelope.path("payloadDigest").asText())
                .isEqualTo(EventContractV1_1.payloadDigest(envelope.path("safePayload")));
        assertThat(envelope.path("safePayload").path("traceId").asText()).isEqualTo(envelope.path("traceId").asText());
        assertThat(envelope.path("safePayload").path("safeDisplay").toString())
                .isEqualTo("{\"label\":\"WORKFLOW_TRANSITIONED\",\"status\":\"ARCHIVED\",\"variant\":\"neutral\"}");
        assertThat(envelope.path("safePayload").has("body")).isFalse();
        assertThat(envelope.path("safePayload").has("token")).isFalse();
        assertThat(envelope.path("occurredAt").asText()).isNotBlank();
        assertThat(fieldNames(envelope)).containsExactlyInAnyOrder(
                "eventId", "eventType", "eventVersion", "organizationId", "subjectId", "resourceType",
                "resourceId", "topic", "actorId", "traceId", "idempotencyKeyDigest", "payloadDigest",
                "safePayload", "occurredAt", "schemaVersion");
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND state = 'PUBLISHED' AND attempt_count = 1 AND published_at IS NOT NULL"))
                .isEqualTo(1);
    }

    @Test
    void persistsTheVerifiedRawJetStreamEnvelopeBeforeAckAndConvergesOnReplay() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "ledger-page"),
                "cms-ledger-create-1");
        publish(tenant, page.pageId());
        pages.archive(tenant.ownerContext(), page.pageId(), 1, "cms-ledger-archive-1");

        try (io.nats.client.Connection connection = Nats.connect(natsUrl())) {
            String durable = "ledger-" + UUID.randomUUID().toString().replace("-", "");
            PushSubscribeOptions options = ConsumerConfiguration.builder()
                    .durable(durable)
                    .deliverPolicy(DeliverPolicy.New)
                    .ackPolicy(AckPolicy.Explicit)
                    .buildPushSubscribeOptions();
            JetStreamSubscription subscription = connection.jetStream().subscribe(OUTBOX_SUBJECT, options);
            OutboxPublisher.PublishResult published = publisher.publishAvailable();
            assertThat(published.published()).isGreaterThanOrEqualTo(1);

            Message delivery = subscription.nextMessage(java.time.Duration.ofSeconds(5));
            assertThat(delivery).isNotNull();
            JsonNode publishedEnvelope = json.readTree(delivery.getData());
            assertThat(publishedEnvelope.path("resourceId").asText()).isEqualTo(page.pageId().toString());
            EventLedgerReceipt first = new NatsJetStreamEventHandler(eventLedger).consumeAndAck(delivery);
            EventLedgerReceipt replay = eventLedger.consume(delivery.getData());

            assertThat(first.duplicate()).isFalse();
            assertThat(replay.eventId()).isEqualTo(first.eventId());
            assertThat(replay.duplicate()).isTrue();
            assertThat(count("SELECT count(*) FROM nexora.event_ledger_entries WHERE event_id = '"
                    + first.eventId() + "'")).isEqualTo(1);

            String raw = new String(delivery.getData(), StandardCharsets.UTF_8);
            String eventId = publishedEnvelope.path("eventId").asText();
            String duplicateEnvelopeField = raw.replace("\"eventId\":\"" + eventId + "\"",
                    "\"eventId\":\"" + eventId + "\",\"eventId\":\"" + eventId + "\"");
            String duplicatePayloadField = raw.replace("\"label\":\"WORKFLOW_TRANSITIONED\"",
                    "\"label\":\"WORKFLOW_TRANSITIONED\",\"label\":\"WORKFLOW_TRANSITIONED\"");
            assertThatThrownBy(() -> eventLedger.consume(duplicateEnvelopeField.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(EventEnvelopeRejectedException.class);
            assertThatThrownBy(() -> eventLedger.consume(duplicatePayloadField.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(EventEnvelopeRejectedException.class);
            assertThatThrownBy(() -> eventLedger.consume((raw + "{\"body\":\"secret\"}").getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(EventEnvelopeRejectedException.class);
            assertThat(count("SELECT count(*) FROM nexora.event_ledger_entries WHERE event_id = '"
                    + first.eventId() + "'")).isEqualTo(1);

            Message malformedDelivery = mock(Message.class);
            when(malformedDelivery.isJetStream()).thenReturn(true);
            when(malformedDelivery.getData()).thenReturn(duplicatePayloadField.getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> new NatsJetStreamEventHandler(eventLedger).consumeAndAck(malformedDelivery))
                    .isInstanceOf(EventEnvelopeRejectedException.class);
            verify(malformedDelivery).term();
            verify(malformedDelivery, never()).ackSync(any(java.time.Duration.class));

            EventLedgerConsumer unavailableConsumer = new EventLedgerConsumer(new com.nexora.platform.events.consumer.EventLedgerRepository(null) {
                @Override
                public EventLedgerReceipt record(OutboxEvent ignored) {
                    throw new IllegalStateException("simulated ledger outage");
                }
            });
            Message retryDelivery = mock(Message.class);
            when(retryDelivery.isJetStream()).thenReturn(true);
            when(retryDelivery.getData()).thenReturn(delivery.getData());
            assertThatThrownBy(() -> new NatsJetStreamEventHandler(unavailableConsumer).consumeAndAck(retryDelivery))
                    .isInstanceOf(IllegalStateException.class);
            verify(retryDelivery).nakWithDelay(java.time.Duration.ofSeconds(1));
            verify(retryDelivery, never()).ackSync(any(java.time.Duration.class));

            AtomicBoolean persisted = new AtomicBoolean();
            EventLedgerConsumer acknowledgedConsumer = new EventLedgerConsumer(new com.nexora.platform.events.consumer.EventLedgerRepository(null) {
                @Override
                public EventLedgerReceipt record(OutboxEvent event) {
                    persisted.set(true);
                    return new EventLedgerReceipt(event.id(), false);
                }
            });
            Message acknowledgedDelivery = mock(Message.class);
            when(acknowledgedDelivery.isJetStream()).thenReturn(true);
            when(acknowledgedDelivery.getData()).thenReturn(delivery.getData());
            doAnswer(ignored -> {
                assertThat(persisted).isTrue();
                return null;
            }).when(acknowledgedDelivery).ackSync(any(java.time.Duration.class));
            assertThat(new NatsJetStreamEventHandler(acknowledgedConsumer).consumeAndAck(acknowledgedDelivery).duplicate())
                    .isFalse();
            verify(acknowledgedDelivery).ackSync(java.time.Duration.ofSeconds(3));
            subscription.unsubscribe();
        }
    }

    @Test
    void reusesTheOriginalOutboxReceiptForTheSameArchiveRequest() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "outbox-idempotency"),
                "cms-outbox-idempotency-create-1");
        publish(tenant, page.pageId());
        pages.archive(tenant.ownerContext(), page.pageId(), 1, "cms-outbox-idempotency-archive-1");
        CmsPageService.PageView archived = pages.get(tenant.ownerContext(), page.pageId());
        UUID[] replay = tenantContexts.withFreshTenant(tenant.ownerContext(), (authoritative, jdbc) -> new UUID[] {
            outboxRecorder.recordArchivedPage(jdbc, authoritative, page.pageId(), archived.draftVersion(), archived.updatedAt()),
            outboxRecorder.recordArchivedPage(jdbc, authoritative, page.pageId(), archived.draftVersion(), archived.updatedAt())
        });

        assertThat(replay[1]).isEqualTo(replay[0]);
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND event_type = 'WORKFLOW_TRANSITIONED'"))
                .isEqualTo(1);
    }

    @Test
    void retriesAfterTransportOutageThenRecoversThroughJetStream() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "outbox-retry"),
                "cms-outbox-retry-create-1");
        publish(tenant, page.pageId());
        pages.archive(tenant.ownerContext(), page.pageId(), 1, "cms-outbox-retry-archive-1");
        OutboxPublisher unavailable = new OutboxPublisher(outboxEvents,
                event -> { throw new IllegalStateException("simulated NATS outage"); }, outboxProperties);

        OutboxPublisher.PublishResult failed = unavailable.publishAvailable();

        assertThat(failed.retrying()).isGreaterThanOrEqualTo(1);
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND state = 'FAILED' AND attempt_count = 1")).isEqualTo(1);
        Thread.sleep(1_500);

        OutboxPublisher.PublishResult recovered = publisher.publishAvailable();

        assertThat(recovered.published()).isGreaterThanOrEqualTo(1);
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND state = 'PUBLISHED' AND attempt_count = 2")).isEqualTo(1);
    }

    @Test
    void replaysAfterAmbiguousPostPublishFailureInsteadOfFalseAcknowledgement() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "outbox-replay"),
                "cms-outbox-replay-create-1");
        publish(tenant, page.pageId());
        pages.archive(tenant.ownerContext(), page.pageId(), 1, "cms-outbox-replay-archive-1");
        long messagesBefore = streamMessageCount();
        OutboxPublisher ambiguous = new OutboxPublisher(outboxEvents, event -> {
            outboxTransport.publish(event);
            throw new IllegalStateException("simulated crash after JetStream acknowledgement");
        }, outboxProperties);

        OutboxPublisher.PublishResult failed = ambiguous.publishAvailable();

        assertThat(failed.retrying()).isGreaterThanOrEqualTo(1);
        assertThat(streamMessageCount()).isGreaterThan(messagesBefore);
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND state = 'FAILED' AND attempt_count = 1")).isEqualTo(1);
        Thread.sleep(1_500);

        OutboxPublisher.PublishResult replayed = publisher.publishAvailable();

        assertThat(replayed.published()).isGreaterThanOrEqualTo(1);
        assertThat(streamMessageCount()).isGreaterThan(messagesBefore + 1);
        assertThat(count("SELECT count(*) FROM nexora.outbox_events WHERE resource_id = '" + page.pageId()
                + "' AND state = 'PUBLISHED' AND attempt_count = 2")).isEqualTo(1);
    }

    @Test
    void rejectsMalformedStoredEnvelopeBeforeJetStreamPublication() throws Exception {
        String hostileDigest = "x\",\"body\":\"injected" + "z".repeat(32);
        OutboxEvent hostile = new OutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "page", UUID.randomUUID(), 1,
                "tenant:00000000-0000-0000-0000-000000000001:workflow", "WORKFLOW_TRANSITIONED", "1.1.0",
                hostileDigest, hostileDigest,
                "{\"traceId\":\"safe-trace-1\",\"safeDisplay\":{\"label\":\"Page workflow\",\"status\":\"ARCHIVED\"}}",
                "safe-trace-1", Instant.now(), 1);

        long messagesBefore = streamMessageCount();

        assertThatThrownBy(() -> outboxTransport.publish(hostile))
                .isInstanceOf(OutboxContractViolationException.class)
                .hasMessageContaining("does not satisfy contract v1.1");

        assertThat(streamMessageCount()).isEqualTo(messagesBefore);
    }

    @Test
    void replacesTraceHeadersThatWouldBeRejectedByTheDurableSafePayloadPolicy() throws Exception {
        CmsFixture tenant = seedTenant();
        CmsPageService.PageView page = pages.create(tenant.ownerContext(), create(tenant, "safe-trace"), "cms-trace-create-1");
        publish(tenant, page.pageId());

        HttpResponse<String> response = archive(
                tenant.organizationId(), tenant.ownerSubjectId(), page.pageId(), 1, "token-1");

        assertThat(response.statusCode()).isEqualTo(200);
        String replacementTraceId = response.headers().firstValue("X-Trace-Id").orElseThrow();
        assertThat(replacementTraceId).isNotEqualTo("token-1");
        assertThat(replacementTraceId).matches("[A-Za-z0-9._-]{1,128}");
        String eventTraceId = scalar("SELECT safe_payload ->> 'traceId' FROM nexora.outbox_events WHERE resource_id = '"
                + page.pageId() + "'");
        assertThat(eventTraceId).matches("[a-f0-9]{32}");
        assertThat(eventTraceId).isNotEqualTo(replacementTraceId);
    }

    @Test
    void issuesScopedRealtimeDescriptorsOnlyAfterServerAuthorization() throws Exception {
        CmsFixture alpha = seedTenant();
        CmsFixture beta = seedTenant();
        CmsPageService.PageView page = pages.create(alpha.ownerContext(), create(alpha, "presence-page"),
                "cms-realtime-presence-1");

        HttpResponse<String> publication = descriptor(alpha.organizationId(), alpha.ownerSubjectId(),
                "{\"eventType\":\"PUBLICATION_INVALIDATED\"}");
        HttpResponse<String> presence = descriptor(alpha.organizationId(), alpha.ownerSubjectId(),
                "{\"eventType\":\"PRESENCE_CHANGED\",\"resourceId\":\"" + page.pageId() + "\"}");
        HttpResponse<String> missingResource = descriptor(alpha.organizationId(), alpha.ownerSubjectId(),
                "{\"eventType\":\"PRESENCE_CHANGED\"}");
        HttpResponse<String> crossTenantGuess = descriptor(beta.organizationId(), alpha.ownerSubjectId(),
                "{\"eventType\":\"PUBLICATION_INVALIDATED\"}");
        SignedJWT ordinarySessionToken = SignedJWT.parse(ISSUER.token(alpha.ownerSubjectId(), Instant.now().plusSeconds(60)));

        assertDescriptor(publication, "tenant:" + alpha.organizationId() + ":publication",
                "PUBLICATION_INVALIDATED", "broadcast", alpha.ownerSubjectId());
        assertDescriptor(presence, "resource:" + page.pageId() + ":presence",
                "PRESENCE_CHANGED", "presence", alpha.ownerSubjectId());
        assertThat(missingResource.statusCode()).isEqualTo(403);
        assertThat(json.readTree(missingResource.body()).path("code").asText()).isEqualTo("REALTIME_DESCRIPTOR_DENIED");
        assertThat(crossTenantGuess.statusCode()).isEqualTo(403);
        assertThat(json.readTree(crossTenantGuess.body()).path("code").asText()).isEqualTo("PERMISSION_DENIED");
        assertThat(ordinarySessionToken.getJWTClaimsSet().getClaim("nexora_realtime_topic")).isNull();
    }

    private CmsPageService.CreateCommand create(CmsFixture tenant, String slug) {
        return new CmsPageService.CreateCommand(tenant.siteId(), slug, "Welcome", "1.0.0", digest('a'),
                tenant.themeVersionId(), seo());
    }

    private CmsPageService.UpdateCommand update(CmsFixture tenant, long expectedVersion, String title) {
        return new CmsPageService.UpdateCommand(expectedVersion, title, "1.0.0", digest('c'),
                tenant.themeVersionId(), seo());
    }

    private CmsPageService.SeoSnapshot seo() {
        return new CmsPageService.SeoSnapshot("Welcome", "Welcome description.", "en-US", "/welcome",
                "Welcome", "Welcome description.", null, "website", "summary", "Welcome",
                "Welcome description.", null, "WebPage");
    }

    private static String digest(char character) {
        return "sha256:" + String.valueOf(character).repeat(64);
    }

    private HttpResponse<String> archive(UUID organizationId, UUID subjectId, UUID pageId, long expectedDraftVersion)
            throws Exception {
        return archive(organizationId, subjectId, pageId, expectedDraftVersion, null);
    }

    private HttpResponse<String> archive(
            UUID organizationId, UUID subjectId, UUID pageId, long expectedDraftVersion, String traceId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/api/v1/cms/pages/" + pageId + "?expectedDraftVersion=" + expectedDraftVersion))
                .header("Authorization", "Bearer " + ISSUER.token(subjectId, Instant.now().plusSeconds(60)))
                .header("X-Nexora-Organization-Id", organizationId.toString());
        if (traceId != null) {
            builder.header("X-Trace-Id", traceId);
        }
        return http.send(builder.DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> descriptor(UUID organizationId, UUID subjectId, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + port + "/api/v1/realtime/descriptors"))
                .header("Authorization", "Bearer " + ISSUER.token(subjectId, Instant.now().plusSeconds(60)))
                .header("X-Nexora-Organization-Id", organizationId.toString())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertDescriptor(
            HttpResponse<String> response,
            String topic,
            String eventType,
            String delivery,
            UUID subjectId) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode descriptor = json.readTree(response.body());
        assertThat(descriptor.path("topic").asText()).isEqualTo(topic);
        assertThat(descriptor.path("eventType").asText()).isEqualTo(eventType);
        assertThat(descriptor.path("eventVersion").asLong()).isEqualTo(1);
        assertThat(descriptor.path("authorizationEpoch").asLong()).isGreaterThan(0);
        assertThat(descriptor.path("privateChannel").asBoolean()).isTrue();
        assertThat(descriptor.path("delivery").asText()).isEqualTo(delivery);
        assertThat(descriptor.path("reconnectBackoffMs").isArray()).isTrue();
        assertThat(descriptor.path("reconnectBackoffMs")).hasSize(4);
        SignedJWT token = SignedJWT.parse(descriptor.path("transportToken").asText());
        assertThat(token.verify(new MACVerifier(REALTIME_JWT_SECRET))).isTrue();
        assertThat(token.getJWTClaimsSet().getIssueTime()).isNotNull();
        assertThat(token.getJWTClaimsSet().getExpirationTime()).isNotNull();
        long descriptorLifetimeSeconds = token.getJWTClaimsSet().getExpirationTime().toInstant().getEpochSecond()
                - token.getJWTClaimsSet().getIssueTime().toInstant().getEpochSecond();
        assertThat(descriptorLifetimeSeconds).isBetween(30L, 300L);
        assertThat(token.getJWTClaimsSet().getSubject()).isEqualTo(subjectId.toString());
        assertThat(token.getJWTClaimsSet().getStringClaim("nexora_realtime_topic")).isEqualTo(topic);
        assertThat(token.getJWTClaimsSet().getStringClaim("nexora_realtime_event_type")).isEqualTo(eventType);
        assertThat(token.getJWTClaimsSet().getLongClaim("nexora_realtime_event_version")).isEqualTo(1L);
        assertThat(token.getJWTClaimsSet().getLongClaim("nexora_realtime_authorization_epoch"))
                .isEqualTo(descriptor.path("authorizationEpoch").asLong());
    }

    private void publish(CmsFixture actor, UUID pageId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, actor.ownerSubjectId(), actor.organizationId(), actor.ownerMembershipId());
            statement.execute("UPDATE nexora.pages SET state = 'IN_REVIEW' WHERE id = '" + pageId + "'");
            statement.execute("UPDATE nexora.pages SET state = 'APPROVED' WHERE id = '" + pageId + "'");
            statement.execute("UPDATE nexora.pages SET state = 'PUBLISHED' WHERE id = '" + pageId + "'");
            connection.commit();
        }
    }

    private void addMembership(CmsFixture actor, UUID subjectId, String role) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, actor.ownerSubjectId(), actor.organizationId(), actor.ownerMembershipId());
            statement.execute("INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role) VALUES ('"
                    + UUID.randomUUID() + "', '" + actor.organizationId() + "', '" + subjectId + "', 'ACTIVE', '" + role + "')");
            connection.commit();
        }
    }

    private void setMembershipRole(CmsFixture actor, UUID subjectId, String role) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, actor.ownerSubjectId(), actor.organizationId(), actor.ownerMembershipId());
            statement.execute("UPDATE nexora.memberships SET tenant_role = '" + role
                    + "', version = version + 1 WHERE organization_id = '" + actor.organizationId()
                    + "' AND subject_id = '" + subjectId + "'");
            connection.commit();
        }
    }

    private static void prepareRuntimeRole() {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE nexora_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE "
                    + "NOINHERIT NOREPLICATION NOBYPASSRLS");
            statement.execute("CREATE ROLE " + RUNTIME_LOGIN + " LOGIN PASSWORD '" + RUNTIME_PASSWORD + "'");
            statement.execute("GRANT nexora_runtime TO " + RUNTIME_LOGIN);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare the CMS runtime role", exception);
        }
    }

    private static void prepareOutboxStream() {
        try (io.nats.client.Connection connection = Nats.connect(natsUrl())) {
            connection.jetStreamManagement().addStream(StreamConfiguration.builder()
                    .name(OUTBOX_STREAM)
                    .subjects(OUTBOX_SUBJECT)
                    .storageType(StorageType.Memory)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare the disposable JetStream outbox stream", exception);
        }
    }

    private static String natsUrl() {
        return "nats://" + NATS.getHost() + ":" + NATS.getMappedPort(4222);
    }

    private static void prepareMigrations() {
        try {
            migrationDirectory = Files.createTempDirectory("nexora-cms-flyway-");
            Path source = Path.of("..", "..", "database", "migrations").toAbsolutePath().normalize();
            for (int version = 1; version <= 21; version++) {
                String prefix = "V%03d__".formatted(version);
                try (Stream<Path> candidates = Files.list(source)) {
                    Path migration = candidates.filter(path -> path.getFileName().toString().startsWith(prefix))
                            .findFirst().orElseThrow();
                    Files.copy(migration, migrationDirectory.resolve(migration.getFileName()));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare M2 CMS migrations", exception);
        }
    }

    private CmsFixture seedTenant() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID ownerMembershipId = UUID.randomUUID();
        UUID ownerSubjectId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        UUID themeId = UUID.randomUUID();
        UUID themeVersionId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET CONSTRAINTS ALL DEFERRED");
            statement.execute("INSERT INTO nexora.organizations (id, slug, name, owner_membership_id) VALUES ('"
                    + organizationId + "', 'org-" + organizationId.toString().substring(0, 8)
                    + "', 'CMS fixture', '" + ownerMembershipId + "')");
            setContext(statement, ownerSubjectId, organizationId, ownerMembershipId);
            statement.execute("INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role) VALUES ('"
                    + ownerMembershipId + "', '" + organizationId + "', '" + ownerSubjectId + "', 'ACTIVE', 'OWNER')");
            statement.execute("INSERT INTO nexora.sites (id, organization_id, slug, canonical_host) VALUES ('"
                    + siteId + "', '" + organizationId + "', 'main', '" + organizationId.toString().substring(0, 8)
                    + ".example.test')");
            statement.execute("INSERT INTO nexora.themes (id, organization_id, slug) VALUES ('"
                    + themeId + "', '" + organizationId + "', 'default')");
            statement.execute("INSERT INTO nexora.theme_versions (id, organization_id, theme_id, version, state, token_digest, token_manifest, actor_id) VALUES ('"
                    + themeVersionId + "', '" + organizationId + "', '" + themeId + "', 1, 'PUBLISHED', '"
                    + digest('d') + "', '{\"color\":\"safe\"}', '" + ownerSubjectId + "')");
            connection.commit();
        }
        return new CmsFixture(organizationId, ownerMembershipId, ownerSubjectId, siteId, themeVersionId);
    }

    private static void setContext(
            Statement statement, UUID subjectId, UUID organizationId, UUID membershipId) throws Exception {
        statement.execute("SELECT set_config('nexora.subject_id', '" + subjectId + "', true)");
        statement.execute("SELECT set_config('nexora.organization_id', '" + organizationId + "', true)");
        statement.execute("SELECT set_config('nexora.membership_id', '" + membershipId + "', true)");
    }

    private int count(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement();
             var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private String scalar(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement();
             var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getString(1);
        }
    }

    private long streamMessageCount() throws Exception {
        try (io.nats.client.Connection connection = Nats.connect(natsUrl())) {
            return connection.jetStreamManagement().getStreamInfo(OUTBOX_STREAM).getStreamState().getMsgCount();
        }
    }

    private JsonNode latestStreamMessage() throws Exception {
        try (io.nats.client.Connection connection = Nats.connect(natsUrl())) {
            var management = connection.jetStreamManagement();
            long lastSequence = management.getStreamInfo(OUTBOX_STREAM).getStreamState().getLastSequence();
            return json.readTree(new String(management.getMessage(OUTBOX_STREAM, lastSequence).getData(), StandardCharsets.UTF_8));
        }
    }

    private Set<String> fieldNames(JsonNode node) {
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private record CmsFixture(UUID organizationId, UUID ownerMembershipId, UUID ownerSubjectId,
                              UUID siteId, UUID themeVersionId) {
        TenantContext ownerContext() {
            return new TenantContext(ownerSubjectId, organizationId, ownerMembershipId, 1, "OWNER");
        }
    }
}
