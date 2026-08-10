package com.nexora.platform.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class NexoraJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2TokenValidatorResult SUCCESS = OAuth2TokenValidatorResult.success();
    private static final List<String> ACCEPTED_AAL = List.of("aal1", "aal2");
    private final String audience;

    NexoraJwtValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (!token.getAudience().contains(audience)) {
            return failure("invalid_audience");
        }
        if (!"authenticated".equals(token.getClaimAsString("role"))) {
            return failure("invalid_execution_role");
        }
        Boolean anonymous = token.getClaimAsBoolean("is_anonymous");
        if (anonymous == null || anonymous) {
            return failure("anonymous_identity");
        }
        if (token.getExpiresAt() == null) {
            return failure("missing_expiration");
        }
        if (!ACCEPTED_AAL.contains(token.getClaimAsString("aal"))) {
            return failure("invalid_aal");
        }
        try {
            UUID.fromString(token.getSubject());
            UUID.fromString(token.getClaimAsString("session_id"));
        } catch (RuntimeException exception) {
            return failure("invalid_identity_claims");
        }
        return SUCCESS;
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
