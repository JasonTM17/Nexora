package com.nexora.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("database")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseProfileIntegrationTests {
    private static final String RUNTIME_LOGIN = "nexora_runtime_login";
    private static final String RUNTIME_PASSWORD = "test-runtime-login";
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("nexora")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("runtime-login.sql");

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource runtimeDataSource;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.start();
        prepareRuntimeLogin();
        registry.add("NEXORA_RUNTIME_DATABASE_URL", DATABASE::getJdbcUrl);
        registry.add("NEXORA_RUNTIME_DATABASE_USERNAME", () -> RUNTIME_LOGIN);
        registry.add("NEXORA_RUNTIME_DATABASE_PASSWORD", () -> RUNTIME_PASSWORD);
        registry.add("NEXORA_MIGRATION_DATABASE_URL", DATABASE::getJdbcUrl);
        registry.add("NEXORA_MIGRATION_DATABASE_USERNAME", DATABASE::getUsername);
        registry.add("NEXORA_MIGRATION_DATABASE_PASSWORD", DATABASE::getPassword);
        registry.add("NEXORA_MIGRATIONS_LOCATION", () -> Path.of("..", "..", "database", "migrations")
                .toAbsolutePath().normalize().toString());
    }

    private static void prepareRuntimeLogin() {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE ROLE nexora_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                    NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
            statement.execute("GRANT nexora_runtime TO " + RUNTIME_LOGIN);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare the disposable runtime login", exception);
        }
    }

    @AfterAll
    static void stopDatabase() {
        DATABASE.stop();
    }

    @Test
    void migratesAndUsesTheNonOwnerRuntimeRoleBeforeReportingReady() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'nexora')
                         AND EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version = '001')
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBoolean(1)).isTrue();
        }

        try (Connection connection = runtimeDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT session_user, current_user")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("session_user")).isEqualTo(RUNTIME_LOGIN);
            assertThat(resultSet.getString("current_user")).isEqualTo("nexora_runtime");
        }

        HttpResponse<String> readiness = getReadiness();
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).contains("\"status\":\"UP\"");

        DATABASE.stop();

        HttpResponse<String> unavailableReadiness = getReadiness();
        assertThat(unavailableReadiness.statusCode()).isEqualTo(503);
        assertThat(unavailableReadiness.body()).contains("\"status\":\"DOWN\"");
    }

    private HttpResponse<String> getReadiness() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health/readiness"))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
