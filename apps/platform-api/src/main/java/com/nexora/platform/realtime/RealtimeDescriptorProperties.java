package com.nexora.platform.realtime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Validated
@Profile("database")
@ConfigurationProperties("nexora.realtime.descriptor")
public record RealtimeDescriptorProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32) String jwtSecret,
        @Min(30) @Max(300) long ttlSeconds) {

    public RealtimeDescriptorProperties {
        issuer = issuer == null || issuer.isBlank() ? "nexora-platform-realtime" : issuer;
        ttlSeconds = ttlSeconds == 0 ? 120 : ttlSeconds;
    }

    Duration ttl() {
        return Duration.ofSeconds(ttlSeconds);
    }
}
