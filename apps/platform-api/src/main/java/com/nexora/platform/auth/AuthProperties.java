package com.nexora.platform.auth;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("nexora.auth")
public record AuthProperties(URI issuer, @NotBlank String audience, URI jwkSetUri) {
}
