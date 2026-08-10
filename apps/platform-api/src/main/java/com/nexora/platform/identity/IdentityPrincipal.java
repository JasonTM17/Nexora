package com.nexora.platform.identity;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public record IdentityPrincipal(UUID subjectId, UUID sessionId, String assuranceLevel) {

    public static IdentityPrincipal from(Jwt jwt) {
        return new IdentityPrincipal(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("session_id")),
                jwt.getClaimAsString("aal"));
    }
}
