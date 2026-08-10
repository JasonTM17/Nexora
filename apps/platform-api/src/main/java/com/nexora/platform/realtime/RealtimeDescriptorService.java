package com.nexora.platform.realtime;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("database")
public class RealtimeDescriptorService {
    private static final List<Long> RECONNECT_BACKOFF_MS = List.of(1000L, 2000L, 5000L, 10000L);

    private final TenantContextService tenantContexts;
    private final RealtimeDescriptorProperties properties;
    private final Clock clock;

    public RealtimeDescriptorService(
            TenantContextService tenantContexts, RealtimeDescriptorProperties properties, Clock clock) {
        this.tenantContexts = tenantContexts;
        this.properties = properties;
        this.clock = clock;
    }

    public Descriptor issue(TenantContext expected, RealtimeChannel channel, UUID resourceId) {
        if (channel.requiresResource() && resourceId == null) {
            throw denied();
        }
        return tenantContexts.withFreshTenant(expected, (authoritative, jdbc) -> {
            String topic = channel.topic(authoritative.organizationId(), resourceId);
            Long epoch = jdbc.queryForObject(
                    "SELECT nexora.current_realtime_descriptor_epoch(?)",
                    Long.class,
                    topic);
            if (epoch == null) {
                throw denied();
            }
            Instant issuedAt = Instant.now(clock);
            Instant expiresAt = issuedAt.plus(properties.ttl());
            return new Descriptor(
                    topic,
                    channel.eventType(),
                    channel.eventVersion(),
                    epoch,
                    expiresAt,
                    sign(authoritative.subjectId(), topic, channel, epoch, issuedAt, expiresAt),
                    true,
                    channel.delivery(),
                    RECONNECT_BACKOFF_MS,
                    "REFETCH_DURABLE_STATE");
        });
    }

    private String sign(
            UUID subjectId,
            String topic,
            RealtimeChannel channel,
            long epoch,
            Instant issuedAt,
            Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .subject(subjectId.toString())
                    .audience("authenticated")
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim("role", "authenticated")
                    .claim("nexora_realtime_topic", topic)
                    .claim("nexora_realtime_event_type", channel.eventType())
                    .claim("nexora_realtime_event_version", channel.eventVersion())
                    .claim("nexora_realtime_authorization_epoch", epoch)
                    .build();
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                    claims);
            token.sign(new MACSigner(properties.jwtSecret()));
            return token.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to issue scoped Realtime descriptor.", exception);
        }
    }

    private DomainAccessException denied() {
        return new DomainAccessException(
                HttpStatus.FORBIDDEN,
                "REALTIME_DESCRIPTOR_DENIED",
                "Realtime descriptor is not available for this channel.");
    }

    public record Descriptor(
            String topic,
            String eventType,
            long eventVersion,
            long authorizationEpoch,
            Instant expiresAt,
            String transportToken,
            boolean privateChannel,
            String delivery,
            List<Long> reconnectBackoffMs,
            String onEvent) {
    }
}
