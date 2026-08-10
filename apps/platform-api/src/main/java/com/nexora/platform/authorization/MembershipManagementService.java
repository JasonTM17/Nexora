package com.nexora.platform.authorization;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
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
        return tenantContexts.withFreshTenantTarget(
                actor, targetMembershipId, expectedVersion, true,
                (authoritative, jdbc) -> {
                    permissions.requireAssignment(jdbc, authoritative, targetRole);
                    return updateRole(jdbc, targetMembershipId, expectedVersion, targetRole);
                });
    }

    public MembershipView changeStatus(
            TenantContext actor, UUID targetMembershipId, long expectedVersion, MembershipStatus status) {
        if (status == MembershipStatus.ACTIVE) {
            throw denied("Direct activation is not supported by the v1 assignment boundary.");
        }
        return tenantContexts.withFreshTenantTarget(
                actor, targetMembershipId, expectedVersion, true,
                (authoritative, jdbc) -> {
                    permissions.require(jdbc, authoritative, "user.manage");
                    return updateStatus(jdbc, targetMembershipId, expectedVersion, status);
                });
    }

    private MembershipView updateRole(
            JdbcTemplate jdbc, UUID targetMembershipId, long expectedVersion, String targetRole) {
        try {
            return jdbc.query("""
                    UPDATE nexora.memberships
                    SET tenant_role = ?::nexora.tenant_role
                    WHERE id = ? AND version = ?
                    RETURNING id, organization_id, subject_id, status::text, tenant_role::text, version
                    """, this::map, targetRole, targetMembershipId, expectedVersion)
                    .stream().findFirst().orElseThrow(this::versionConflict);
        } catch (DataAccessException exception) {
            throw translateDatabaseDenial(exception);
        }
    }

    private MembershipView updateStatus(
            JdbcTemplate jdbc, UUID targetMembershipId, long expectedVersion, MembershipStatus status) {
        try {
            return jdbc.query("""
                    UPDATE nexora.memberships
                    SET status = ?::nexora.membership_status
                    WHERE id = ? AND version = ?
                    RETURNING id, organization_id, subject_id, status::text, tenant_role::text, version
                    """, this::map, status.name(), targetMembershipId, expectedVersion)
                    .stream().findFirst().orElseThrow(this::versionConflict);
        } catch (DataAccessException exception) {
            throw translateDatabaseDenial(exception);
        }
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
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("REJECT_LAST_OWNER_INVARIANT")) {
            return new DomainAccessException(
                    HttpStatus.CONFLICT, "REJECT_LAST_OWNER_INVARIANT", "The organization must retain an active owner.");
        }
        return denied("The membership mutation was denied.");
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
