package com.nexora.platform.auth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class LocalJwtIssuer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    private final AtomicInteger jwksRequests = new AtomicInteger();
    private volatile RSAKey signingKey;

    LocalJwtIssuer() {
        try {
            signingKey = newKey();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
                jwksRequests.incrementAndGet();
                byte[] body = new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("Cache-Control", "public, max-age=600");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            executor = Executors.newSingleThreadExecutor();
            server.setExecutor(executor);
            server.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to start deterministic JWT issuer", exception);
        }
    }

    String issuer() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/auth/v1";
    }

    String jwksUri() {
        return issuer() + "/.well-known/jwks.json";
    }

    int jwksRequests() {
        return jwksRequests.get();
    }

    void rotate() {
        signingKey = newKey();
    }

    String token(UUID subjectId, Instant expiresAt) {
        return token(subjectId, expiresAt, Map.of());
    }

    String token(UUID subjectId, Instant expiresAt, Map<String, Object> extraClaims) {
        return token(JWSAlgorithm.RS256, subjectId, expiresAt,
                claims -> extraClaims.forEach(claims::claim));
    }

    String token(Consumer<JWTClaimsSet.Builder> customize) {
        return token(JWSAlgorithm.RS256, UUID.randomUUID(), Instant.now().plusSeconds(60), customize);
    }

    String token(JWSAlgorithm algorithm, Consumer<JWTClaimsSet.Builder> customize) {
        return token(algorithm, UUID.randomUUID(), Instant.now().plusSeconds(60), customize);
    }

    private String token(
            JWSAlgorithm algorithm,
            UUID subjectId,
            Instant expiresAt,
            Consumer<JWTClaimsSet.Builder> customize) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(issuer())
                    .audience("authenticated")
                    .subject(subjectId.toString())
                    .issueTime(Date.from(now.minusSeconds(1)))
                    .expirationTime(Date.from(expiresAt))
                    .claim("session_id", UUID.randomUUID().toString())
                    .claim("role", "authenticated")
                    .claim("aal", "aal1")
                    .claim("is_anonymous", false);
            customize.accept(claims);
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(algorithm)
                            .type(JOSEObjectType.JWT)
                            .keyID(signingKey.getKeyID())
                            .build(),
                    claims.build());
            token.sign(new RSASSASigner(signingKey));
            return token.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to mint deterministic JWT", exception);
        }
    }

    private static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate deterministic test key", exception);
        }
    }

    @Override
    public void close() throws IOException {
        server.stop(0);
        executor.shutdownNow();
    }
}
