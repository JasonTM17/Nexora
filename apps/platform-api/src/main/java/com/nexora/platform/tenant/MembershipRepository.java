package com.nexora.platform.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("database")
class MembershipRepository {
    List<TenantContext> findActive(JdbcTemplate jdbc, UUID subjectId) {
        return jdbc.query("""
                SELECT id, organization_id, version, tenant_role::text
                FROM nexora.memberships
                WHERE subject_id = ? AND status = 'ACTIVE'
                ORDER BY organization_id
                """, (result, row) -> new TenantContext(
                        subjectId,
                        result.getObject("organization_id", UUID.class),
                        result.getObject("id", UUID.class),
                        result.getLong("version"),
                        result.getString("tenant_role")), subjectId);
    }

    Optional<TenantContext> findActiveForAuthorization(JdbcTemplate jdbc, TenantContext expected) {
        return jdbc.query("""
                SELECT id, organization_id, version, tenant_role::text
                FROM nexora.memberships
                WHERE id = ? AND organization_id = ? AND subject_id = ?
                  AND status = 'ACTIVE'
                """, (result, row) -> new TenantContext(
                        expected.subjectId(),
                        result.getObject("organization_id", UUID.class),
                        result.getObject("id", UUID.class),
                        result.getLong("version"),
                        result.getString("tenant_role")),
                expected.membershipId(), expected.organizationId(), expected.subjectId()).stream().findFirst();
    }
}
