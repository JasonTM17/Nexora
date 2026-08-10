package com.nexora.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

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

    private JwtDecoder decoder(LocalJwtIssuer issuer) {
        return new SecurityConfiguration().jwtDecoder(
                new AuthProperties(URI.create(issuer.issuer()), "authenticated", URI.create(issuer.jwksUri())));
    }
}
