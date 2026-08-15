package com.nexora.platform.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.List;

/**
 * M6-R01: Dual-algorithm JWT decoder supporting Ed25519 (primary) and
 * HS256 (legacy fallback) during migration.
 *
 * <p>Implements the three-phase Ed25519 migration protocol:
 * <ol>
 *   <li>Dual verify: both Ed25519 and HS256 accepted (LEGACY_HS256_FALLBACK=true)</li>
 *   <li>Cut signing: issuer flips to Ed25519; both still accepted</li>
 *   <li>Drop legacy: only Ed25519 accepted (LEGACY_HS256_FALLBACK=false)</li>
 * </ol>
 */
public class DualJwtDecoder implements JwtDecoder {

    private final JwtDecoder primaryDecoder;
    private final JwtDecoder legacyDecoder;
    private final boolean legacyFallbackEnabled;

    public DualJwtDecoder(JwtDecoder primaryDecoder, JwtDecoder legacyDecoder, boolean legacyFallbackEnabled) {
        this.primaryDecoder = primaryDecoder;
        this.legacyDecoder = legacyDecoder;
        this.legacyFallbackEnabled = legacyFallbackEnabled;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        // Try primary (Ed25519/RS256) first
        try {
            return primaryDecoder.decode(token);
        } catch (JwtException primaryError) {
            if (!legacyFallbackEnabled) {
                throw primaryError;
            }
            // Fallback to legacy (HS256) during migration
            try {
                return legacyDecoder.decode(token);
            } catch (JwtException legacyError) {
                // Throw the primary error (more descriptive of target state)
                throw primaryError;
            }
        }
    }

    /**
     * Factory for creating the dual decoder from a JWK Set URI.
     * Primary: Ed25519/RS256 (asymmetric). Legacy: HS256 (symmetric).
     */
    public static DualJwtDecoder fromJwkSetUri(String jwkSetUri, String legacySecret, boolean legacyFallbackEnabled) {
        NimbusJwtDecoder primary = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        // Legacy decoder uses the shared secret for HS256
        NimbusJwtDecoder legacy = NimbusJwtDecoder.withSecretKey(
                new javax.crypto.spec.SecretKeySpec(legacySecret.getBytes(), "HmacSHA256")).build();
        return new DualJwtDecoder(primary, legacy, legacyFallbackEnabled);
    }
}
