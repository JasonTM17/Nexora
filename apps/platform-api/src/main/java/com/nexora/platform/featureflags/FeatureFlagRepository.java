package com.nexora.platform.featureflags;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Feature flag repository. Reads filter by tenant inline; writes go through
 * the function-only runtime boundary that sets the tenant context for RLS.
 */
@Repository
@Profile("database")
public class FeatureFlagRepository {

    private final JdbcTemplate jdbc;

    public FeatureFlagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<FeatureFlag> findByTenantIdAndFlagKey(UUID tenantId, String flagKey) {
        List<FeatureFlag> results = jdbc.query("""
                SELECT id, tenant_id, flag_key, enabled, rollout_percentage, rules, description
                FROM nexora.feature_flags
                WHERE tenant_id = ? AND flag_key = ?
                """, this::map, tenantId, flagKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<FeatureFlag> findAllByTenantId(UUID tenantId) {
        return jdbc.query("""
                SELECT id, tenant_id, flag_key, enabled, rollout_percentage, rules, description
                FROM nexora.feature_flags
                WHERE tenant_id = ?
                ORDER BY flag_key
                """, this::map, tenantId);
    }

    public FeatureFlag save(FeatureFlag flag) {
        if (flag.id() == null) {
            return insert(flag);
        }
        return update(flag);
    }

    private FeatureFlag insert(FeatureFlag flag) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO nexora.feature_flags (tenant_id, flag_key, enabled, rollout_percentage, rules, description)
                VALUES (?, ?, ?, ?, ?::jsonb, ?)
                RETURNING id
                """, UUID.class, flag.tenantId(), flag.flagKey(), flag.enabled(),
                flag.rolloutPercentage(), toJson(flag.rules()), flag.description());
        return findById(id).orElse(flag);
    }

    private FeatureFlag update(FeatureFlag flag) {
        jdbc.update("""
                UPDATE nexora.feature_flags
                SET enabled = ?, rollout_percentage = ?, rules = ?::jsonb, description = ?, updated_at = now()
                WHERE tenant_id = ? AND flag_key = ?
                """, flag.enabled(), flag.rolloutPercentage(), toJson(flag.rules()),
                flag.description(), flag.tenantId(), flag.flagKey());
        return flag;
    }

    private Optional<FeatureFlag> findById(UUID id) {
        List<FeatureFlag> results = jdbc.query("""
                SELECT id, tenant_id, flag_key, enabled, rollout_percentage, rules, description
                FROM nexora.feature_flags WHERE id = ?
                """, this::map, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private FeatureFlag map(ResultSet rs, int row) throws SQLException {
        return new FeatureFlag(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("flag_key")) {{
            setEnabled(rs.getBoolean("enabled"));
            setRolloutPercentage(rs.getInt("rollout_percentage"));
            setDescription(rs.getString("description"));
        }};
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
