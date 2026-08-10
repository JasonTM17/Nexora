package com.nexora.platform.tenant;

import com.nexora.platform.auth.DomainAccessException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("database")
public class TenantContextService {
    private static final String STALE_MEMBERSHIP_CONTEXT = "DENY_STALE_MEMBERSHIP_CONTEXT";
    private final TransactionLocalDatabaseContext databaseContext;
    private final MembershipRepository memberships;

    public TenantContextService(
            TransactionLocalDatabaseContext databaseContext, MembershipRepository memberships) {
        this.databaseContext = databaseContext;
        this.memberships = memberships;
    }

    public List<TenantContext> accessContexts(UUID subjectId) {
        return databaseContext.forSubject(subjectId, jdbc -> memberships.findActive(jdbc, subjectId));
    }

    public TenantContext resolve(UUID subjectId, UUID selectedOrganizationId) {
        List<TenantContext> active = accessContexts(subjectId);
        if (selectedOrganizationId != null) {
            return active.stream()
                    .filter(context -> context.organizationId().equals(selectedOrganizationId))
                    .findFirst()
                    .orElseThrow(() -> denied("PERMISSION_DENIED", "The selected organization is not available."));
        }
        if (active.isEmpty()) {
            throw denied("MEMBERSHIP_REQUIRED", "An active organization membership is required.");
        }
        if (active.size() > 1) {
            throw denied("TENANT_SELECTION_REQUIRED", "Select one active organization.");
        }
        return active.getFirst();
    }

    public <T> T withFreshTenant(
            UUID subjectId, UUID selectedOrganizationId, BiFunction<TenantContext, JdbcTemplate, T> work) {
        TenantContext resolved = resolve(subjectId, selectedOrganizationId);
        return withFreshTenant(resolved, work);
    }

    public <T> T withFreshTenant(
            TenantContext expected, BiFunction<TenantContext, JdbcTemplate, T> work) {
        return databaseContext.forSubject(expected.subjectId(), jdbc -> {
            TenantContext authoritative = memberships.findActiveForAuthorization(jdbc, expected)
                    .filter(current -> current.membershipId().equals(expected.membershipId()))
                    .filter(current -> current.membershipVersion() == expected.membershipVersion())
                    .filter(current -> current.role().equals(expected.role()))
                    .orElseThrow(this::staleMembershipContext);
            databaseContext.promoteToTenant(authoritative);
            return work.apply(authoritative, jdbc);
        });
    }

    private DomainAccessException staleMembershipContext() {
        return new DomainAccessException(
                HttpStatus.FORBIDDEN,
                "PERMISSION_DENIED",
                STALE_MEMBERSHIP_CONTEXT,
                "The active membership context is stale.");
    }

    private DomainAccessException denied(String code, String message) {
        return new DomainAccessException(HttpStatus.FORBIDDEN, code, message);
    }
}
