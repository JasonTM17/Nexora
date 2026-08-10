package com.nexora.platform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("nexora.platform")
public record PlatformProperties(
        @NotBlank String apiVersion,
        MigrationContract migrationContract) {

    public record MigrationContract(
            @NotBlank @Pattern(regexp = "V\\d{3}") String baselineVersion,
            @NotBlank String runtimeRole,
            @NotEmpty List<@NotBlank String> schemas) {
    }
}
