package com.nexora.platform.cms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
class CmsPageIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_cms_runtime_login";
    private static final String RUNTIME_PASSWORD = "test-cms-runtime-login";
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("nexora_cms")
            .withUsername("postgres")
            .withPassword("postgres");
    private static Path migrationDirectory;

    @Autowired
    private CmsPageService pages;

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
    }

    @AfterAll
    static void stopFixtures() throws Exception {
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

    private static void prepareMigrations() {
        try {
            migrationDirectory = Files.createTempDirectory("nexora-cms-flyway-");
            Path source = Path.of("..", "..", "database", "migrations").toAbsolutePath().normalize();
            for (int version = 1; version <= 11; version++) {
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

    private record CmsFixture(UUID organizationId, UUID ownerMembershipId, UUID ownerSubjectId,
                              UUID siteId, UUID themeVersionId) {
        TenantContext ownerContext() {
            return new TenantContext(ownerSubjectId, organizationId, ownerMembershipId, 1, "OWNER");
        }
    }
}
