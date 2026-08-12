package com.nexora.platform.events.admission;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.authorization.PermissionEvaluator;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Authorizes the one currently-owned Go-ingress candidate without trusting its
 * tenant or resource metadata. The caller supplies only candidates; this
 * service returns the values derived from the active, transaction-local
 * membership and the forced-RLS page lookup.
 *
 * <p>This is deliberately not an event producer and it does not mint a
 * service token. The Go ingress must forward the existing bearer principal,
 * exact-compare this decision before publishing, and obtain a new decision
 * after the short validity window.</p>
 */
@Service
@Profile("database")
public class EventAdmissionService {
    static final String EVENT_TYPE = "PUBLICATION_INVALIDATED";
    static final String RESOURCE_TYPE = "page";
    static final String SCHEMA_VERSION = "1.1.0";
    private static final long MAX_VALIDITY_SECONDS = 30;

    private final TenantContextService tenantContexts;
    private final PermissionEvaluator permissions;

    public EventAdmissionService(TenantContextService tenantContexts, PermissionEvaluator permissions) {
        this.tenantContexts = tenantContexts;
        this.permissions = permissions;
    }

    public AdmissionDecision authorizePublicationInvalidation(
            UUID subjectId,
            Instant bearerExpiresAt,
            UUID organizationCandidate,
            CandidateEnvelope candidate) {
        if (bearerExpiresAt == null || !bearerExpiresAt.isAfter(Instant.now())) {
            throw denied();
        }
        requireCanonicalPublicationInvalidation(candidate);
        TenantContext selected = tenantContexts.resolve(subjectId, organizationCandidate);
        return tenantContexts.withFreshTenant(selected, (actor, jdbc) -> {
            permissions.require(jdbc, actor, "page.publish");
            PageResource page = findPublishedPage(jdbc, candidate.resourceId(), actor.organizationId());
            if (candidate.eventVersion() != page.eventVersion()) {
                throw denied();
            }
            Instant validUntil = validity(bearerExpiresAt);
            return new AdmissionDecision(
                    actor.organizationId(), actor.subjectId(), actor.subjectId(), RESOURCE_TYPE, page.id(),
                    EVENT_TYPE, page.eventVersion(), SCHEMA_VERSION,
                    "tenant:%s:publication".formatted(actor.organizationId()), validUntil);
        });
    }

    private PageResource findPublishedPage(JdbcTemplate jdbc, UUID pageId, UUID organizationId) {
        return jdbc.query("""
                SELECT id, draft_version
                FROM nexora.pages
                WHERE id = ? AND organization_id = ? AND state = 'PUBLISHED'
                """, (result, row) -> new PageResource(
                        result.getObject("id", UUID.class), result.getLong("draft_version")),
                pageId, organizationId).stream().findFirst().orElseThrow(this::denied);
    }

    private Instant validity(Instant bearerExpiresAt) {
        Instant shortDeadline = Instant.now().plusSeconds(MAX_VALIDITY_SECONDS);
        return bearerExpiresAt.isBefore(shortDeadline) ? bearerExpiresAt : shortDeadline;
    }

    private void requireCanonicalPublicationInvalidation(CandidateEnvelope candidate) {
        if (candidate == null
                || !EVENT_TYPE.equals(candidate.eventType())
                || !RESOURCE_TYPE.equals(candidate.resourceType())
                || candidate.resourceId() == null
                || candidate.eventVersion() < 1
                || candidate.eventVersion() > 9_007_199_254_740_991L
                || !SCHEMA_VERSION.equals(candidate.schemaVersion())) {
            throw denied();
        }
    }

    private DomainAccessException denied() {
        return new DomainAccessException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Permission denied.");
    }

    record CandidateEnvelope(
            String eventType, String resourceType, UUID resourceId, long eventVersion, String schemaVersion) { }

    private record PageResource(UUID id, long eventVersion) { }

    public record AdmissionDecision(
            UUID organizationId,
            UUID subjectId,
            UUID actorId,
            String resourceType,
            UUID resourceId,
            String eventType,
            long eventVersion,
            String schemaVersion,
            String topic,
            Instant validUntil) { }
}
