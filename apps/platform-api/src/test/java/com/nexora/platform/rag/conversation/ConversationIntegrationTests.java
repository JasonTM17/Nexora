package com.nexora.platform.rag.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.auth.LocalJwtIssuer;
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
class ConversationIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_chat_runtime_login";
    private static final String RUNTIME_PASSWORD = "test-chat-runtime-login";
    private static final LocalJwtIssuer ISSUER = new LocalJwtIssuer();
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("pgvector/pgvector:0.8.1-pg17")
            .withDatabaseName("nexora_chat")
            .withUsername("postgres")
            .withPassword("postgres");
    private static Path migrationDirectory;

    @Autowired
    private ConversationService conversations;

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
        registry.add("nexora.realtime.descriptor.jwt-secret", () -> "test-chat-realtime-secret-for-m4-t06b");
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
    void persistsIdempotentSendsAndRegenerationLineage() throws Exception {
        TenantFixture tenant = seedTenant("Chat");

        ConversationService.SessionView session = conversations.createSession(tenant.ownerContext(), "Publishing help");
        assertThat(session.state()).isEqualTo("ACTIVE");

        ConversationService.MessageView first = conversations.send(
                tenant.ownerContext(), session.id(), "client-msg-1", "How do I publish immutably?");
        ConversationService.MessageView duplicate = conversations.send(
                tenant.ownerContext(), session.id(), "client-msg-1", "How do I publish immutably?");
        assertThat(duplicate.id()).isEqualTo(first.id());

        ConversationService.MessageView assistant = conversations.completeAssistant(
                tenant.ownerContext(), session.id(), first.id(), "Publishing creates a new immutable version.");
        assertThat(assistant.revision()).isEqualTo(2);
        assertThat(assistant.state()).isEqualTo("COMPLETED");

        List<ConversationService.MessageView> history = conversations.history(
                tenant.ownerContext(), session.id(), null, 50);
        assertThat(history).extracting(ConversationService.MessageView::id)
                .containsExactly(first.id(), assistant.id());
    }

    @Test
    void deniesCrossSubjectAndSoftDeletesSession() throws Exception {
        TenantFixture tenant = seedTenant("ChatDelete");

        ConversationService.SessionView session = conversations.createSession(tenant.ownerContext(), "Delete me");
        conversations.send(tenant.ownerContext(), session.id(), "client-msg-2", "Message before deletion.");

        ConversationService.SessionView deleted = conversations.deleteSession(tenant.ownerContext(), session.id());
        assertThat(deleted.state()).isEqualTo("DELETED");

        List<ConversationService.MessageView> history = conversations.history(
                tenant.ownerContext(), session.id(), null, 50);
        assertThat(history).isEmpty();

        assertThatThrownBy(() -> conversations.send(
                tenant.ownerContext(), session.id(), "client-msg-3", "After deletion."))
                .isInstanceOf(DomainAccessException.class);
    }

    private TenantFixture seedTenant(String label) throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID ownerMembershipId = UUID.randomUUID();
        UUID ownerSubjectId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET CONSTRAINTS ALL DEFERRED");
            statement.execute("INSERT INTO nexora.organizations (id, slug, name, owner_membership_id) VALUES ('"
                    + organizationId + "', 'org-" + organizationId.toString().substring(0, 8)
                    + "', 'Chat " + label + "', '" + ownerMembershipId + "')");
            setContext(statement, ownerSubjectId, organizationId, ownerMembershipId);
            statement.execute("INSERT INTO nexora.memberships (id, organization_id, subject_id, status, tenant_role) VALUES ('"
                    + ownerMembershipId + "', '" + organizationId + "', '" + ownerSubjectId + "', 'ACTIVE', 'OWNER')");
            connection.commit();
        }
        return new TenantFixture(organizationId, ownerMembershipId, ownerSubjectId);
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
            throw new IllegalStateException("Unable to prepare the chat runtime role", exception);
        }
    }

    private static void prepareMigrations() {
        try {
            migrationDirectory = Files.createTempDirectory("nexora-chat-flyway-");
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
            throw new IllegalStateException("Unable to prepare chat migrations", exception);
        }
    }

    private static void setContext(
            Statement statement, UUID subjectId, UUID organizationId, UUID membershipId) throws Exception {
        statement.execute("SELECT set_config('nexora.subject_id', '" + subjectId + "', true)");
        statement.execute("SELECT set_config('nexora.organization_id', '" + organizationId + "', true)");
        statement.execute("SELECT set_config('nexora.membership_id', '" + membershipId + "', true)");
    }

    private record TenantFixture(UUID organizationId, UUID ownerMembershipId, UUID ownerSubjectId) {
        TenantContext ownerContext() {
            return new TenantContext(ownerSubjectId, organizationId, ownerMembershipId, 1, "OWNER");
        }
    }
}
