package com.nexora.platform.api;

import com.nexora.platform.config.PlatformProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/platform", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform", description = "Nexora platform foundation")
public class PlatformController {
    private final PlatformProperties properties;

    public PlatformController(PlatformProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    @Operation(summary = "Read the platform contract baseline")
    @ApiResponse(responseCode = "200", description = "Platform baseline")
    public PlatformResponse platform() {
        PlatformProperties.MigrationContract contract = properties.migrationContract();
        return new PlatformResponse(
                properties.apiVersion(),
                contract.baselineVersion(),
                List.copyOf(contract.schemas()));
    }

    @PostMapping(path = "/echo", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate the bootstrap API error contract")
    @ApiResponse(responseCode = "200", description = "Validated request payload")
    @ApiResponse(responseCode = "400", description = "Stable validation problem")
    public EchoResponse echo(@Valid @RequestBody EchoRequest request) {
        return new EchoResponse(request.message());
    }

    public record PlatformResponse(String apiVersion, String migrationBaseline, List<String> schemas) {
    }

    public record EchoRequest(@NotBlank @Size(max = 140) String message) {
    }

    public record EchoResponse(String message) {
    }
}
