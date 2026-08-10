package com.nexora.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtValidationTests {

    @Test
    void validatesRequiredClaimsAndRejectsExpiredOrAnonymousTokens() throws Exception {
        try (LocalJwtIssuer issuer = new LocalJwtIssuer()) {
            JwtDecoder decoder = decoder(issuer);
            UUID subject = UUID.randomUUID();

            assertThat(decoder.decode(issuer.token(subject, Instant.now().plusSeconds(60))).getSubject())
                    .isEqualTo(subject.toString());
            assertThatThrownBy(() -> decoder.decode(issuer.token(subject, Instant.now().minusSeconds(120))))
                    .isInstanceOf(JwtException.class);
            assertThatThrownBy(() -> decoder.decode(issuer.token(
                    subject, Instant.now().plusSeconds(60), Map.of("is_anonymous", true))))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Test
    void refreshesOnceForKeyRotationAndCachesEachObservedJwks() throws Exception {
        try (LocalJwtIssuer issuer = new LocalJwtIssuer()) {
            JwtDecoder decoder = decoder(issuer);
            String first = issuer.token(UUID.randomUUID(), Instant.now().plusSeconds(60));

            decoder.decode(first);
            decoder.decode(first);
            assertThat(issuer.jwksRequests()).isEqualTo(1);

            issuer.rotate();
            String rotated = issuer.token(UUID.randomUUID(), Instant.now().plusSeconds(60));
            decoder.decode(rotated);
            decoder.decode(rotated);

            assertThat(issuer.jwksRequests()).isEqualTo(2);
        }
    }

    @Test
    void rejectsInvalidSignatureAndForgedIssuerOrAudience() throws Exception {
        try (LocalJwtIssuer trusted = new LocalJwtIssuer(); LocalJwtIssuer attacker = new LocalJwtIssuer()) {
            JwtDecoder decoder = decoder(trusted);
            UUID subject = UUID.randomUUID();

            assertThatThrownBy(() -> decoder.decode(attacker.token(subject, Instant.now().plusSeconds(60))))
                    .isInstanceOf(JwtException.class);
            assertThatThrownBy(() -> decoder.decode(trusted.token(
                    subject, Instant.now().plusSeconds(60), Map.of("aud", List.of("forged")))))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Test
    void rejectsEachRequiredClaimAtItsIntendedPredicateWithTheTrustedKey() throws Exception {
        try (LocalJwtIssuer issuer = new LocalJwtIssuer()) {
            JwtDecoder decoder = decoder(issuer);
            decoder.decode(issuer.token(UUID.randomUUID(), Instant.now().plusSeconds(60)));

            assertValidationFailure(decoder,
                    issuer.token(claims -> claims.issuer("http://127.0.0.1/wrong-issuer")), "iss");
            assertValidationFailure(decoder, issuer.token(claims -> claims.subject(null)), "invalid_identity_claims");
            assertValidationFailure(decoder, issuer.token(claims -> claims.subject("not-a-uuid")),
                    "invalid_identity_claims");
            assertValidationFailure(decoder, issuer.token(claims -> claims.claim("session_id", null)),
                    "invalid_identity_claims");
            assertValidationFailure(decoder, issuer.token(claims -> claims.claim("session_id", "not-a-uuid")),
                    "invalid_identity_claims");
            assertValidationFailure(decoder, issuer.token(claims -> claims.claim("role", "service_role")),
                    "invalid_execution_role");
            assertValidationFailure(decoder, issuer.token(claims -> claims.claim("aal", null)), "invalid_aal");
            assertValidationFailure(decoder, issuer.token(claims -> claims.claim("aal", "aal3")), "invalid_aal");
            assertValidationFailure(decoder, issuer.token(claims -> claims.expirationTime(null)),
                    "missing_expiration");

            assertThatThrownBy(() -> decoder.decode(issuer.token(JWSAlgorithm.PS256, claims -> { })))
                    .isInstanceOfSatisfying(JwtException.class,
                            exception -> assertThat(exception.getMessage().toLowerCase()).contains("algorithm"));
        }
    }

    private void assertValidationFailure(JwtDecoder decoder, String token, String expectedPredicate) {
        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOfSatisfying(JwtValidationException.class, exception -> assertThat(exception.getErrors())
                        .anySatisfy(error -> assertThat(error.getDescription()).contains(expectedPredicate)));
    }

    private JwtDecoder decoder(LocalJwtIssuer issuer) {
        return new SecurityConfiguration().jwtDecoder(
                new AuthProperties(URI.create(issuer.issuer()), "authenticated", URI.create(issuer.jwksUri())));
    }
}
