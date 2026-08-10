package com.nexora.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexora.platform.PlatformApiApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(classes = PlatformApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformApiIntegrationTests {
    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesTheMigrationContractThroughThePlatformSlice() throws Exception {
        HttpResponse<String> response = get("/api/v1/platform", null);

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValueSatisfying(value -> assertThat(value).isNotBlank());
        assertThat(body.path("apiVersion").asText()).isEqualTo("v1");
        assertThat(body.path("migrationBaseline").asText()).isEqualTo("V001");
        assertThat(body.path("schemas").get(0).asText()).isEqualTo("nexora");
        assertThat(body.path("schemas").get(1).asText()).isEqualTo("rag");
        assertThat(body.path("schemas").get(2).asText()).isEqualTo("audit");
    }

    @Test
    void exposesDeterministicLocalLivenessAndReadiness() throws Exception {
        HttpResponse<String> liveness = get("/actuator/health/liveness", null);
        HttpResponse<String> readiness = get("/actuator/health/readiness", null);

        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(liveness.body()).path("status").asText()).isEqualTo("UP");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(readiness.body()).path("status").asText()).isEqualTo("UP");
    }

    @Test
    void publishesOpenApiAndPreservesCallerTraceIds() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs", "test-trace-42");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id")).contains("test-trace-42");
        assertThat(objectMapper.readTree(response.body()).path("paths").has("/api/v1/platform")).isTrue();
    }

    @Test
    void returnsASafeTraceableProblemForInvalidRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/platform/echo"))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", "validation-trace-7")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"\"}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("X-Trace-Id")).contains("validation-trace-7");
        assertThat(body.path("code").asText()).isEqualTo("validation_failed");
        assertThat(body.path("traceId").asText()).isEqualTo("validation-trace-7");
        assertThat(body.path("details").has("message")).isTrue();
    }

    private HttpResponse<String> get(String path, String traceId) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (traceId != null) {
            request.header("X-Trace-Id", traceId);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
