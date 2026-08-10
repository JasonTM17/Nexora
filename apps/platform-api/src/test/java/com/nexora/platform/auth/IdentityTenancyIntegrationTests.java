package com.nexora.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("database")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdentityTenancyIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_identity_runtime_login";
    private static final String RUNTIME_PASSWORD = "test-identity-runtime-login";
    private static final LocalJwtIssuer ISSUER = new LocalJwtIssuer();
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("nexora_identity")
            .withUsername("postgres")
            .withPassword("postgres");
    private static Path migrationDirectory;

    @LocalServerPort
    private int port;

    @Autowired
    private TenantContextService tenantContexts;

    @Autowired
    private DataSource dataSource;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

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
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
    }

    @AfterAll
    static void stopFixtures() throws Exception {
        DATABASE.stop();
        ISSUER.close();
        if (migrationDirectory != null) {
            try (Stream<Path> paths = Files.walk(migrationDirectory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Test
    void rejectsInvalidAndExpiredJwtAtTheHttpBoundary() throws Exception {
        UUID subject = UUID.randomUUID();

        assertThat(get("/api/v1/identity/access-context", "not-a-jwt", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/v1/identity/access-context",
                ISSUER.token(subject, Instant.now().minusSeconds(120)), null).statusCode()).isEqualTo(401);
    }

    @Test
    void ignoresForgedMetadataAndRequiresAnAuthoritativeMembership() throws Exception {
        UUID subject = UUID.randomUUID();
        UUID forgedOrganization = UUID.randomUUID();
        String token = ISSUER.token(subject, Instant.now().plusSeconds(120), Map.of(
                "organization_id", forgedOrganization.toString(),
                "permissions", java.util.List.of("user.manage"),
                "app_metadata", Map.of("organization_id", forgedOrganization.toString()),
                "user_metadata", Map.of("role", "OWNER")));

        HttpResponse<String> access = get("/api/v1/identity/access-context", token, null);
        HttpResponse<String> selected = get("/api/v1/tenant-context", token, forgedOrganization);

        assertThat(access.statusCode()).isEqualTo(200);
        assertThat(json.readTree(access.body()).path("memberships")).isEmpty();
        assertThat(selected.statusCode()).isEqualTo(403);
        assertThat(json.readTree(selected.body()).path("code").asText()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void derivesPermissionsFromFreshMembershipAndDeniesRemovedOrCrossTenantSelection() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        OrganizationFixture alpha = seedOrganization(owner);
        OrganizationFixture beta = seedOrganization(UUID.randomUUID());
        UUID activeMembership = seedMembership(alpha, subject, "USER", "ACTIVE");
        seedMembership(beta, subject, "OWNER", "REMOVED");
        String token = ISSUER.token(subject, Instant.now().plusSeconds(120), Map.of(
                "role", "authenticated", "permissions", java.util.List.of("user.manage")));

        HttpResponse<String> allowed = get(
                "/api/v1/authorization/permission-matrix", token, alpha.organizationId());
        HttpResponse<String> removed = get("/api/v1/tenant-context", token, beta.organizationId());
        HttpResponse<String> unknown = get("/api/v1/tenant-context", token, UUID.randomUUID());
        removeMembership(alpha, activeMembership);
        HttpResponse<String> removedOnNextRequest = get(
                "/api/v1/authorization/permission-matrix", token, alpha.organizationId());

        assertThat(allowed.statusCode()).isEqualTo(200);
        JsonNode allowedBody = json.readTree(allowed.body());
        assertThat(allowedBody.at("/context/role").asText()).isEqualTo("USER");
        assertThat(allowedBody.path("permissions")).hasSize(4);
        assertThat(removed.statusCode()).isEqualTo(403);
        assertThat(unknown.statusCode()).isEqualTo(403);
        assertThat(removedOnNextRequest.statusCode()).isEqualTo(403);
        assertThat(json.readTree(removed.body()).path("code").asText()).isEqualTo("PERMISSION_DENIED");
        assertThat(json.readTree(unknown.body()).path("code").asText()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void allowsOnlyProfileFieldsAndRejectsAStaleVersion() throws Exception {
        UUID subject = UUID.randomUUID();
        String token = ISSUER.token(subject, Instant.now().plusSeconds(120));

        HttpResponse<String> created = putProfile(token,
                "{\"displayName\":\"Ada\",\"locale\":\"en-US\","
                        + "\"reducedMotion\":false,\"highContrast\":true,\"expectedVersion\":0}");
        HttpResponse<String> updated = putProfile(token,
                "{\"displayName\":\"Ada L\",\"locale\":\"en-US\","
                        + "\"reducedMotion\":true,\"highContrast\":true,\"expectedVersion\":1}");
        HttpResponse<String> stale = putProfile(token,
                "{\"displayName\":\"Stale\",\"locale\":\"en-US\","
                        + "\"reducedMotion\":false,\"highContrast\":false,\"expectedVersion\":1}");
        HttpResponse<String> forgedField = putProfile(token,
                "{\"displayName\":\"Ada\",\"locale\":\"en-US\","
                        + "\"reducedMotion\":false,\"highContrast\":false,"
                        + "\"expectedVersion\":2,\"tenantRole\":\"OWNER\"}");

        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(json.readTree(created.body()).path("version").asLong()).isEqualTo(1);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(json.readTree(updated.body()).path("version").asLong()).isEqualTo(2);
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(json.readTree(stale.body()).path("code").asText()).isEqualTo("VERSION_CONFLICT");
        assertThat(forgedField.statusCode()).isEqualTo(400);
    }

    @Test
    void clearsAllTransactionLocalSettingsAfterAnExceptionOnThePooledConnection() throws Exception {
        OrganizationFixture tenant = seedOrganization(UUID.randomUUID());
        TenantContext context = tenant.ownerContext();

        assertThatThrownBy(() -> tenantContexts.withFreshTenant(context, (authoritative, jdbc) -> {
            throw new IllegalStateException("forced rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("forced rollback");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet settings = statement.executeQuery("""
                     SELECT current_setting('nexora.subject_id', true),
                            current_setting('nexora.organization_id', true),
                            current_setting('nexora.membership_id', true)
                     """)) {
            assertThat(settings.next()).isTrue();
            assertThat(settings.getString(1)).isEmpty();
            assertThat(settings.getString(2)).isEmpty();
            assertThat(settings.getString(3)).isEmpty();
        }
    }

    @Test
    void resetsAfterCommitAndPreventsTenantContextOrDataLeakOnRealPooledReuse() throws Exception {
        UUID tenantASubject = UUID.randomUUID();
        UUID tenantBSubject = UUID.randomUUID();
        OrganizationFixture tenantA = seedOrganization(tenantASubject);
        OrganizationFixture tenantB = seedOrganization(tenantBSubject);
        TenantContext contextA = tenantA.ownerContext();
        TenantContext contextB = tenantB.ownerContext();

        Integer backendA = tenantContexts.withFreshTenant(contextA, (authoritative, jdbc) -> {
            assertCurrentContext(jdbc, contextA);
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM nexora.organizations WHERE id = ?", Integer.class,
                    tenantA.organizationId())).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM nexora.organizations WHERE id = ?", Integer.class,
                    tenantB.organizationId())).isZero();
            return jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
        });

        assertPooledSettingsAreEmpty();

        Integer backendB = tenantContexts.withFreshTenant(contextB, (authoritative, jdbc) -> {
            assertCurrentContext(jdbc, contextB);
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM nexora.organizations WHERE id = ?", Integer.class,
                    tenantA.organizationId())).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM nexora.organizations WHERE id = ?", Integer.class,
                    tenantB.organizationId())).isEqualTo(1);
            return jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
        });

        assertThat(backendB).isEqualTo(backendA);
        assertPooledSettingsAreEmpty();
    }

    @Test
    void deniesChangedMembershipVersionAndRoleBeforePromotingOrRunningTenantWork() throws Exception {
        UUID subject = UUID.randomUUID();
        OrganizationFixture tenant = seedOrganization(UUID.randomUUID());
        UUID membershipId = seedMembership(tenant, subject, "USER", "ACTIVE");
        TenantContext resolved = tenantContexts.resolve(subject, tenant.organizationId());
        AtomicBoolean tenantWorkRan = new AtomicBoolean();

        changeMembershipRole(tenant, membershipId, "REVIEWER");

        DomainAccessException failure = catchThrowableOfType(
                () -> tenantContexts.withFreshTenant(resolved, (context, jdbc) -> {
            tenantWorkRan.set(true);
            return null;
        }), DomainAccessException.class);

        assertThat(failure.code()).isEqualTo("PERMISSION_DENIED");
        assertThat(failure.internalCode()).isEqualTo("DENY_STALE_MEMBERSHIP_CONTEXT");
        assertThat(failure.getMessage()).contains("stale");
        assertThat(tenantWorkRan).isFalse();
        assertPooledSettingsAreEmpty();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(com.nexora.platform.observability.TraceIdFilter.ATTRIBUTE, "stale-test-trace");
        var response = new IdentityApiExceptionHandler().domainFailure(failure, request);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PERMISSION_DENIED");
        assertThat(response.getBody().message()).isEqualTo("Permission denied.");
        assertThat(response.getBody().message())
                .doesNotContain("DENY_STALE_MEMBERSHIP_CONTEXT")
                .doesNotContainIgnoringCase("stale", "membership", "version", "role");
    }

    private HttpResponse<String> get(String path, String token, UUID organizationId) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET();
        if (organizationId != null) {
            request.header("X-Nexora-Organization-Id", organizationId.toString());
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertCurrentContext(org.springframework.jdbc.core.JdbcTemplate jdbc, TenantContext context) {
        Map<String, Object> settings = jdbc.queryForMap("""
                SELECT current_setting('nexora.subject_id', true) AS subject_id,
                       current_setting('nexora.organization_id', true) AS organization_id,
                       current_setting('nexora.membership_id', true) AS membership_id
                """);
        assertThat(settings.get("subject_id")).isEqualTo(context.subjectId().toString());
        assertThat(settings.get("organization_id")).isEqualTo(context.organizationId().toString());
        assertThat(settings.get("membership_id")).isEqualTo(context.membershipId().toString());
    }

    private void assertPooledSettingsAreEmpty() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet settings = statement.executeQuery("""
                     SELECT current_setting('nexora.subject_id', true),
                            current_setting('nexora.organization_id', true),
                            current_setting('nexora.membership_id', true)
                     """)) {
            assertThat(settings.next()).isTrue();
            assertThat(settings.getString(1)).isEmpty();
            assertThat(settings.getString(2)).isEmpty();
            assertThat(settings.getString(3)).isEmpty();
        }
    }

    private HttpResponse<String> putProfile(String token, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/profile"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void prepareRuntimeRole() {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE ROLE nexora_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                    NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
            statement.execute("CREATE ROLE " + RUNTIME_LOGIN + " LOGIN PASSWORD '" + RUNTIME_PASSWORD + "'");
            statement.execute("GRANT nexora_runtime TO " + RUNTIME_LOGIN);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare runtime role", exception);
        }
    }

    private static void prepareMigrations() {
        try {
            migrationDirectory = Files.createTempDirectory("nexora-m2-flyway-");
            Path source = Path.of("..", "..", "database", "migrations").toAbsolutePath().normalize();
            for (int version = 1; version <= 4; version++) {
                String prefix = "V%03d__".formatted(version);
                try (Stream<Path> candidates = Files.list(source)) {
                    Path migration = candidates
                            .filter(path -> path.getFileName().toString().startsWith(prefix))
                            .findFirst()
                            .orElseThrow();
                    Files.copy(migration, migrationDirectory.resolve(migration.getFileName()));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare M2 migrations", exception);
        }
    }

    private OrganizationFixture seedOrganization(UUID ownerSubject) throws Exception {
        UUID organization = UUID.randomUUID();
        UUID ownerMembership = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET CONSTRAINTS ALL DEFERRED");
            setContext(statement, ownerSubject, organization, ownerMembership);
            statement.execute("INSERT INTO nexora.organizations (id, slug, name, owner_membership_id) VALUES ('"
                    + organization + "', 'org-" + organization.toString().substring(0, 8)
                    + "', 'Fixture', '" + ownerMembership + "')");
            statement.execute("INSERT INTO nexora.memberships "
                    + "(id, organization_id, subject_id, status, tenant_role) VALUES ('"
                    + ownerMembership + "', '" + organization + "', '" + ownerSubject + "', 'ACTIVE', 'OWNER')");
            connection.commit();
        }
        return new OrganizationFixture(organization, ownerMembership, ownerSubject);
    }

    private UUID seedMembership(OrganizationFixture organization, UUID subjectId, String role, String status)
            throws Exception {
        UUID membershipId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, organization.ownerSubjectId(),
                    organization.organizationId(), organization.ownerMembershipId());
            statement.execute("INSERT INTO nexora.memberships "
                    + "(id, organization_id, subject_id, status, tenant_role) VALUES ('"
                    + membershipId + "', '" + organization.organizationId() + "', '" + subjectId + "', '"
                    + status + "', '" + role + "')");
            connection.commit();
        }
        return membershipId;
    }

    private void removeMembership(OrganizationFixture organization, UUID membershipId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, organization.ownerSubjectId(),
                    organization.organizationId(), organization.ownerMembershipId());
            statement.execute("UPDATE nexora.memberships SET status = 'REMOVED' WHERE id = '" + membershipId + "'");
            connection.commit();
        }
    }

    private void changeMembershipRole(
            OrganizationFixture organization, UUID membershipId, String tenantRole) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            setContext(statement, organization.ownerSubjectId(),
                    organization.organizationId(), organization.ownerMembershipId());
            statement.execute("UPDATE nexora.memberships SET tenant_role = '" + tenantRole
                    + "' WHERE id = '" + membershipId + "'");
            connection.commit();
        }
    }

    private static void setContext(
            Statement statement, UUID subjectId, UUID organizationId, UUID membershipId) throws Exception {
        statement.execute("SELECT set_config('nexora.subject_id', '" + subjectId + "', true)");
        statement.execute("SELECT set_config('nexora.organization_id', '" + organizationId + "', true)");
        statement.execute("SELECT set_config('nexora.membership_id', '" + membershipId + "', true)");
    }

    private record OrganizationFixture(UUID organizationId, UUID ownerMembershipId, UUID ownerSubjectId) {
        TenantContext ownerContext() {
            return new TenantContext(ownerSubjectId, organizationId, ownerMembershipId, 1, "OWNER");
        }
    }
}
