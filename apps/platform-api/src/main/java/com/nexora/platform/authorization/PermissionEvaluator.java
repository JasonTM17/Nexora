package com.nexora.platform.authorization;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Central database-backed permission evaluator. Role names never authorize an
 * operation directly; every decision is derived from the frozen permission
 * matrix and the fresh tenant context supplied by TenantContextService.
 */
@Component
@Profile("database")
public class PermissionEvaluator {
    private static final Set<String> TENANT_ROLES = Set.of(
            "OWNER", "ADMIN", "EDITOR", "CONTENT_CREATOR", "REVIEWER", "USER");

    public void require(JdbcTemplate jdbc, TenantContext actor, String permission) {
        if (!has(jdbc, actor, permission)) {
            throw denied("Permission is not granted for this operation.");
        }
    }

    public boolean has(JdbcTemplate jdbc, TenantContext actor, String permission) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM nexora.tenant_role_permissions
                    WHERE tenant_role = ?::nexora.tenant_role
                      AND permission = ?::nexora.tenant_permission
                )
                """, Boolean.class, actor.role(), permission));
    }

    public void requireAssignment(JdbcTemplate jdbc, TenantContext actor, String targetRole) {
        if (!TENANT_ROLES.contains(targetRole)) {
            throw denied("The requested tenant role is not supported.");
        }
        require(jdbc, actor, "user.manage");
        require(jdbc, actor, "role.manage");
        boolean canGrantAll = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT NOT EXISTS (
                    SELECT permission
                    FROM nexora.tenant_role_permissions
                    WHERE tenant_role = ?::nexora.tenant_role
                    EXCEPT
                    SELECT permission
                    FROM nexora.tenant_role_permissions
                    WHERE tenant_role = ?::nexora.tenant_role
                )
                """, Boolean.class, targetRole, actor.role()));
        if (!canGrantAll) {
            throw denied("The actor cannot grant the requested role.");
        }
        if ("OWNER".equals(targetRole) && !"OWNER".equals(actor.role())) {
            throw denied("Only an owner may assign the owner role.");
        }
    }

    private DomainAccessException denied(String message) {
        return new DomainAccessException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", message);
    }
}
