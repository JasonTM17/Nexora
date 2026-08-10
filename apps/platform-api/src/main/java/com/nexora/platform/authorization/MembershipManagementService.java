package com.nexora.platform.authorization;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Membership role/status mutations with one exact target ID + version. */
@Service
@Profile("database")
public class MembershipManagementService {
    private final TenantContextService tenantContexts;
    private final PermissionEvaluator permissions;

    public MembershipManagementService(
            TenantContextService tenantContexts, PermissionEvaluator permissions) {
        this.tenantContexts = tenantContexts;
        this.permissions = permissions;
    }

    public MembershipView assignRole(
            TenantContext actor, UUID targetMembershipId, long expectedVersion, String targetRole) {
        try {
            return tenantContexts.withFreshTenantTarget(
                    actor, targetMembershipId, expectedVersion,
                    (authoritative, jdbc) -> {
                        permissions.requireAssignment(jdbc, authoritative, targetRole);
                        return updateRole(jdbc, targetMembershipId, expectedVersion, targetRole);
                    });
        } catch (RuntimeException exception) {
            throw translateCommitFailure(exception);
        }
    }

    /** Lists the selected tenant only after the same fresh actor and permission checks as mutations. */
    public List<MembershipView> list(TenantContext actor) {
        return tenantContexts.withFreshTenant(actor, (authoritative, jdbc) -> {
            permissions.require(jdbc, authoritative, "user.manage");
            permissions.require(jdbc, authoritative, "role.manage");
            return jdbc.query("""
                    SELECT id, organization_id, subject_id, status::text, tenant_role::text, version
                    FROM nexora.memberships
                    WHERE organization_id = ?
                    ORDER BY id
                    """, this::map, authoritative.organizationId());
        });
    }

    public MembershipView changeStatus(
            TenantContext actor, UUID targetMembershipId, long expectedVersion, MembershipStatus status) {
        if (status == MembershipStatus.ACTIVE) {
            throw denied("Direct activation is not supported by the v1 assignment boundary.");
        }
        try {
            return tenantContexts.withFreshTenantTarget(
                    actor, targetMembershipId, expectedVersion,
                    (authoritative, jdbc) -> {
                        permissions.require(jdbc, authoritative, "user.manage");
                        permissions.require(jdbc, authoritative, "role.manage");
                        return updateStatus(jdbc, targetMembershipId, expectedVersion, status);
                    });
        } catch (RuntimeException exception) {
            throw translateCommitFailure(exception);
        }
    }

    private MembershipView updateRole(
            JdbcTemplate jdbc, UUID targetMembershipId, long expectedVersion, String targetRole) {
        try {
            MembershipView target = expectedTarget(jdbc, targetMembershipId, expectedVersion);
            int updated = tenantContexts.withTargetMembershipMutation(() -> jdbc.update("""
                    UPDATE nexora.memberships
                    SET tenant_role = ?::nexora.tenant_role
                    WHERE id = ? AND version = ?
                    """, targetRole, targetMembershipId, expectedVersion));
            if (updated != 1) {
                throw versionConflict();
            }
            return new MembershipView(
                    target.membershipId(), target.organizationId(), target.subjectId(), target.status(),
                    targetRole, expectedVersion + 1);
        } catch (DataAccessException exception) {
            throw translateDatabaseDenial(exception);
        }
    }

    private MembershipView updateStatus(
            JdbcTemplate jdbc, UUID targetMembershipId, long expectedVersion, MembershipStatus status) {
        try {
            MembershipView target = expectedTarget(jdbc, targetMembershipId, expectedVersion);
            int updated = tenantContexts.withTargetMembershipMutation(() -> jdbc.update("""
                    UPDATE nexora.memberships
                    SET status = ?::nexora.membership_status
                    WHERE id = ? AND version = ?
                    """, status.name(), targetMembershipId, expectedVersion));
            if (updated != 1) {
                throw versionConflict();
            }
            return new MembershipView(
                    target.membershipId(), target.organizationId(), target.subjectId(), status.name(),
                    target.role(), expectedVersion + 1);
        } catch (DataAccessException exception) {
            throw translateDatabaseDenial(exception);
        }
    }

    private MembershipView expectedTarget(JdbcTemplate jdbc, UUID targetMembershipId, long expectedVersion) {
        return jdbc.query("""
                SELECT id, organization_id, subject_id, status::text, tenant_role::text, version
                FROM nexora.memberships
                WHERE id = ? AND version = ?
                """, this::map, targetMembershipId, expectedVersion)
                .stream().findFirst().orElseThrow(this::versionConflict);
    }

    private MembershipView map(java.sql.ResultSet result, int row) throws java.sql.SQLException {
        return new MembershipView(
                result.getObject("id", UUID.class),
                result.getObject("organization_id", UUID.class),
                result.getObject("subject_id", UUID.class),
                result.getString("status"),
                result.getString("tenant_role"),
                result.getLong("version"));
    }

    private DomainAccessException translateDatabaseDenial(DataAccessException exception) {
        DomainAccessException translated = lastOwnerFailure(exception);
        if (translated != null) {
            return translated;
        }
        return denied("The membership mutation was denied.");
    }

    private RuntimeException translateCommitFailure(RuntimeException exception) {
        if (exception instanceof DomainAccessException) {
            return exception;
        }
        DomainAccessException translated = lastOwnerFailure(exception);
        return translated == null ? exception : translated;
    }

    private DomainAccessException lastOwnerFailure(Throwable exception) {
        for (Throwable cursor = exception; cursor != null; cursor = cursor.getCause()) {
            String message = cursor.getMessage();
            if (message != null && (message.contains("REJECT_LAST_OWNER_INVARIANT")
                    || message.contains("organizations_active_owner_fk"))) {
                return new DomainAccessException(
                        HttpStatus.CONFLICT,
                        "REJECT_LAST_OWNER_INVARIANT",
                        "The organization must retain an active owner.");
            }
        }
        return null;
    }

    private DomainAccessException versionConflict() {
        return new DomainAccessException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "The membership version is stale.");
    }

    private DomainAccessException denied(String message) {
        return new DomainAccessException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", message);
    }

    public enum MembershipStatus {
        INVITED, ACTIVE, SUSPENDED, REMOVED
    }

    public record MembershipView(
            UUID membershipId,
            UUID organizationId,
            UUID subjectId,
            String status,
            String role,
            long version) {
    }
}
