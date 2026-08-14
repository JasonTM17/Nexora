package com.nexora.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.auth.LocalJwtIssuer;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;
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
import org.testcontainers.utility.MountableFile;

@ActiveProfiles("database")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_knowledge_runtime_login";
    private static final String RUNTIME_PASSWORD = "test-knowledge-runtime-login";
    private static final LocalJwtIssuer ISSUER = new LocalJwtIssuer();
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("pgvector/pgvector:0.8.1-pg17")
            .withDatabaseName("nexora_knowledge")
            .withUsername("postgres")
            .withPassword("postgres");
    private static Path migrationDirectory;

    @Autowired
    private KnowledgeService knowledge;

    @Autowired
    private TenantContextService tenantContexts;

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
        registry.add("nexora.realtime.descriptor.jwt-secret", () -> "test-knowledge-realtime-secret-for-m4-t01");
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
    void managesKnowledgeBasesAndDocumentsAcrossTwoTenants() throws Exception {
        TenantFixture alpha = seedTenant("Alpha");
        TenantFixture beta = seedTenant("Beta");

        KnowledgeService.KnowledgeBaseView kb = knowledge.createKnowledgeBase(
                alpha.ownerContext(), new KnowledgeService.CreateKnowledgeBaseCommand("Acme KB", "Docs"));
        KnowledgeService.DocumentView doc = knowledge.registerDocument(
                alpha.ownerContext(),
                new KnowledgeService.RegisterDocumentCommand(
                        kb.id(), "syllabus.txt", "text/plain", 512,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        KnowledgeService.DocumentView queued = knowledge.queueDocument(alpha.ownerContext(), doc.id());

        assertThat(kb.state()).isEqualTo("ACTIVE");
        assertThat(doc.state()).isEqualTo("UPLOADED");
        assertThat(queued.state()).isEqualTo("QUEUED");

        KnowledgeService.KnowledgeBaseList listed = knowledge.listKnowledgeBases(alpha.ownerContext(), null, 25);
        assertThat(listed.items()).extracting(KnowledgeService.KnowledgeBaseView::id).contains(kb.id());
        assertThat(listed.items()).extracting(KnowledgeService.KnowledgeBaseView::id).doesNotContain(
                knowledge.createKnowledgeBase(beta.ownerContext(),
                        new KnowledgeService.CreateKnowledgeBaseCommand("Beta KB", null)).id());

        KnowledgeService.DocumentList docs = knowledge.listDocuments(alpha.ownerContext(), kb.id(), null, 25);
        assertThat(docs.items()).extracting(KnowledgeService.DocumentView::id).containsExactly(doc.id());
    }

    @Test
    void deniesCrossTenantDocumentAndKnowledgeBaseAccess() throws Exception {
        TenantFixture alpha = seedTenant("Alpha2");
        TenantFixture beta = seedTenant("Beta2");

        KnowledgeService.KnowledgeBaseView kb = knowledge.createKnowledgeBase(
                alpha.ownerContext(), new KnowledgeService.CreateKnowledgeBaseCommand("Alpha KB", null));
        KnowledgeService.DocumentView doc = knowledge.registerDocument(
                alpha.ownerContext(),
                new KnowledgeService.RegisterDocumentCommand(
                        kb.id(), "secret.txt", "text/plain", 64,
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));

        assertThatThrownBy(() -> knowledge.deleteDocument(beta.ownerContext(), doc.id()))
                .isInstanceOf(DomainAccessException.class);
        assertThatThrownBy(() -> knowledge.deleteKnowledgeBase(beta.ownerContext(), kb.id()))
                .isInstanceOf(DomainAccessException.class);
        assertThat(knowledge.listDocuments(beta.ownerContext(), kb.id(), null, 25).items()).isEmpty();
    }

    @Test
    void softDeletesDocumentsAndBlocksReadOnlyReupload() throws Exception {
        TenantFixture tenant = seedTenant("SoftDelete");

        KnowledgeService.KnowledgeBaseView kb = knowledge.createKnowledgeBase(
                tenant.ownerContext(), new KnowledgeService.CreateKnowledgeBaseCommand("KB", null));
        KnowledgeService.DocumentView doc = knowledge.registerDocument(
                tenant.ownerContext(),
                new KnowledgeService.RegisterDocumentCommand(
                        kb.id(), "delete-me.txt", "text/plain", 32,
                        "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"));
        KnowledgeService.DocumentView deleted = knowledge.deleteDocument(tenant.ownerContext(), doc.id());
        assertThat(deleted.state()).isEqualTo("DELETED");

        KnowledgeService.DocumentView reuploaded = knowledge.registerDocument(
                tenant.ownerContext(),
                new KnowledgeService.RegisterDocumentCommand(
                        kb.id(), "delete-me.txt", "text/plain", 32,
                        "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"));
        assertThat(reuploaded.state()).isEqualTo("UPLOADED");
        assertThat(reuploaded.id()).isNotEqualTo(doc.id());
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
                    + "', 'Knowledge " + label + "', '" + ownerMembershipId + "')");
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
            throw new IllegalStateException("Unable to prepare the knowledge runtime role", exception);
        }
    }

    private static void prepareMigrations() {
        try {
            migrationDirectory = Files.createTempDirectory("nexora-knowledge-flyway-");
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
            throw new IllegalStateException("Unable to prepare knowledge migrations", exception);
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
