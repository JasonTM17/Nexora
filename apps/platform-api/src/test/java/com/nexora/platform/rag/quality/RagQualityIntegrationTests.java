package com.nexora.platform.rag.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.auth.LocalJwtIssuer;
import com.nexora.platform.rag.query.SecureRagService;
import com.nexora.platform.rag.vector.VectorService;
import com.nexora.platform.tenant.TenantContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("database")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RagQualityIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_rag_quality_runtime";
    private static final String RUNTIME_PASSWORD = "test-rag-quality-runtime-pw";
    private static final LocalJwtIssuer ISSUER = new LocalJwtIssuer();
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("pgvector/pgvector:0.8.1-pg17")
            .withDatabaseName("nexora_rag_quality")
            .withUsername("postgres")
            .withPassword("postgres");
    private static Path migrationDirectory;

    @Autowired
    private RagQualityService quality;

    @Autowired
    private SecureRagService rag;

    @Autowired
    private VectorService vectors;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.start();
        prepareRuntimeRole();
        prepareMigrations();
        registry.add("NEXORA_RUNTIME_DATABASE_URL", DATABASE::getJdbcUrl);
        registry.add("NEXORA_RUNTIME_DATABASE_USERNAME", () -> RUNTIME_LOGIN);
        registry.add("NEXORA_RUNTIME_DATABASE_PASSWORD", () -> RUNTIME_PASSWORD);
        registry.add("NEXORA_MIGRATION_DATABASE_URL", DATABASE::getJdbcUrl);
        registry.add("NEXORA_MIGRATION_DATABASE_USERNAME", DATABASE::getUsername);
        registry.add("NEXORA_MIGRATION_DATABASE_PASSWORD", DATABASE::getPassword);
        registry.add("NEXORA_MIGRATIONS_LOCATION", () -> migrationDirectory.toString());
        registry.add("NEXORA_AUTH_ISSUER", ISSUER::issuer);
        registry.add("NEXORA_AUTH_JWKS_URI", ISSUER::jwksUri);
        registry.add("nexora.outbox.publisher.enabled", () -> "false");
        registry.add("nexora.events.consumer.enabled", () -> "false");
        registry.add("nexora.realtime.descriptor.jwt-secret", () -> "test-rag-quality-realtime-secret");
    }

    @AfterAll
    static void stopFixtures() throws Exception {
        ISSUER.close();
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
    void recordsRetrievalRunAndDeniesCrossTenantTraceInspection() throws Exception {
        TenantFixture tenantA = seedTenant("QualityA");
        TenantFixture tenantB = seedTenant("QualityB");

        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        seedDocumentAndChunk(tenantA, docId, chunkId, "Luminous Grid design tokens enforce accessible cobalt actions.");
        vectors.embedAndStore(tenantA.ownerContext(), chunkId);

        SecureRagService.RagAnswer answer = rag.ask(tenantA.ownerContext(), "Luminous Grid cobalt actions");
        assertThat(answer.outcome()).isEqualTo("ANSWERED");

        List<RagTrace> tracesA = quality.listTraces(tenantA.ownerContext(), 10);
        assertThat(tracesA).isNotEmpty();
        RagTrace recorded = tracesA.get(0);
        assertThat(recorded.organizationId()).isEqualTo(tenantA.organizationId());
        assertThat(recorded.queryHash()).startsWith("sha256:");
        assertThat(recorded.outcome()).isEqualTo("ANSWERED");

        // Cross-tenant trace access must throw NOT_FOUND / denied
        assertThatThrownBy(() -> quality.getTrace(tenantB.ownerContext(), recorded.id()))
                .isInstanceOf(DomainAccessException.class);

        // Feedback submission and deletion
        RagFeedback feedback = quality.submitFeedback(tenantA.ownerContext(), recorded.id(), "UP", "Accurate source citation.");
        assertThat(feedback.rating()).isEqualTo("UP");
        assertThat(feedback.comment()).isEqualTo("Accurate source citation.");

        List<RagFeedback> feedbacks = quality.listFeedback(tenantA.ownerContext());
        assertThat(feedbacks).hasSize(1);

        quality.deleteFeedback(tenantA.ownerContext(), feedback.id());
        assertThat(quality.listFeedback(tenantA.ownerContext())).isEmpty();

        // Evaluation report
        RagEvaluationReport report = quality.evaluate(tenantA.ownerContext());
        assertThat(report.totalQueries()).isGreaterThanOrEqualTo(1);
        assertThat(report.recallAtK()).isGreaterThan(0.0);
    }

    private void seedDocumentAndChunk(TenantFixture tenant, UUID documentId, UUID chunkId, String text) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, tenant.ownerSubjectId(), tenant.organizationId(), tenant.ownerMembershipId());
            statement.execute("INSERT INTO nexora.knowledge_bases (id, organization_id, name, description, state, created_by_subject_id) VALUES ('"
                    + tenant.knowledgeBaseId() + "', '" + tenant.organizationId() + "', 'Quality KB', '', 'ACTIVE', '"
                    + tenant.ownerSubjectId() + "') ON CONFLICT (id) DO NOTHING");
            statement.execute("INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES ('"
                    + documentId + "', '" + tenant.knowledgeBaseId() + "', '" + tenant.organizationId()
                    + "', 'quality-doc.txt', 'k/quality-doc', 'text/plain', 128, '"
                    + "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc" + "', 'READY', '"
                    + tenant.ownerSubjectId() + "') ON CONFLICT (id) DO NOTHING");
            statement.execute("INSERT INTO nexora.chunks (id, document_id, organization_id, knowledge_base_id, chunk_index, text, token_count, sha256, chunk_strategy_version, state) VALUES ('"
                    + chunkId + "', '" + documentId + "', '" + tenant.organizationId() + "', '"
                    + tenant.knowledgeBaseId() + "', " + (chunkId.hashCode() & 0x7fffffff) + ", '"
                    + text.replace("'", "''") + "', " + (text.length() / 4 + 1) + ", '"
                    + "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd" + "', 'nexora-chunk-v1', 'ACTIVE')");
            connection.commit();
        }
    }

    private TenantFixture seedTenant(String label) throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID ownerMembershipId = UUID.randomUUID();
        UUID ownerSubjectId = UUID.randomUUID();
        UUID knowledgeBaseId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET CONSTRAINTS ALL DEFERRED");
            statement.execute("INSERT INTO nexora.organizations (id, slug, name, owner_membership_id) VALUES ('"
                    + organizationId + "', 'org-" + organizationId.toString().substring(0, 8)
                    + "', 'Rag Quality " + label + "', '" + ownerMembershipId + "')");
            setContext(statement, ownerSubjectId, organizationId, ownerMembershipId);
            statement.execute("INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role) VALUES ('"
                    + ownerMembershipId + "', '" + organizationId + "', '" + ownerSubjectId + "', 'ACTIVE', 'OWNER')");
            connection.commit();
        }
        return new TenantFixture(organizationId, ownerMembershipId, ownerSubjectId, knowledgeBaseId);
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
            throw new IllegalStateException("Unable to prepare the rag quality runtime role", exception);
        }
    }

    private static void prepareMigrations() {
        try {
            migrationDirectory = Files.createTempDirectory("nexora-rag-quality-flyway-");
            Path source = Path.of("..", "..", "database", "migrations").toAbsolutePath().normalize();
            for (int version = 1; version <= 24; version++) {
                String prefix = "V%03d__".formatted(version);
                try (Stream<Path> candidates = Files.list(source)) {
                    Path migration = candidates.filter(path -> path.getFileName().toString().startsWith(prefix))
                            .findFirst().orElseThrow();
                    Files.copy(migration, migrationDirectory.resolve(migration.getFileName()));
                }
            }
            try (Connection connection = DriverManager.getConnection(
                    DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare the rag quality migrations", exception);
        }
    }

    private static void setContext(Statement statement, UUID subjectId, UUID organizationId, UUID membershipId)
            throws Exception {
        statement.execute("SELECT set_config('nexora.subject_id', '" + subjectId + "', true)");
        statement.execute("SELECT set_config('nexora.organization_id', '" + organizationId + "', true)");
        statement.execute("SELECT set_config('nexora.membership_id', '" + membershipId + "', true)");
    }

    record TenantFixture(
            UUID organizationId, UUID ownerMembershipId, UUID ownerSubjectId, UUID knowledgeBaseId) {
        TenantContext ownerContext() {
            return new TenantContext(ownerSubjectId, organizationId, ownerMembershipId, 1, "OWNER");
        }

        UUID subjectId() {
            return ownerSubjectId;
        }
    }
}
