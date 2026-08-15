package com.nexora.platform.experiment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Experiment repository using JdbcTemplate + RLS inline tenant filter. */
@Repository
@Profile("database")
public class ExperimentRepository {

    private final JdbcTemplate jdbc;

    public ExperimentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Experiment> findByTenantIdAndExperimentKey(UUID tenantId, String experimentKey) {
        List<Experiment> results = jdbc.query("""
                SELECT id, tenant_id, experiment_key, active, treatment_percentage, description
                FROM nexora.experiments
                WHERE tenant_id = ? AND experiment_key = ?
                """, this::map, tenantId, experimentKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Experiment> findAllByTenantId(UUID tenantId) {
        return jdbc.query("""
                SELECT id, tenant_id, experiment_key, active, treatment_percentage, description
                FROM nexora.experiments
                WHERE tenant_id = ?
                ORDER BY experiment_key
                """, this::map, tenantId);
    }

    public Experiment save(Experiment exp) {
        if (exp.id() == null) {
            return insert(exp);
        }
        return update(exp);
    }

    private Experiment insert(Experiment exp) {
        jdbc.update("""
                INSERT INTO nexora.experiments (tenant_id, experiment_key, active, treatment_percentage, description)
                VALUES (?, ?, ?, ?, ?)
                """, exp.tenantId(), exp.experimentKey(), exp.active(), exp.treatmentPercentage(), exp.description());
        return exp;
    }

    private Experiment update(Experiment exp) {
        jdbc.update("""
                UPDATE nexora.experiments
                SET active = ?, treatment_percentage = ?, description = ?
                WHERE tenant_id = ? AND experiment_key = ?
                """, exp.active(), exp.treatmentPercentage(), exp.description(),
                exp.tenantId(), exp.experimentKey());
        return exp;
    }

    private Experiment map(ResultSet rs, int row) throws SQLException {
        Experiment exp = new Experiment(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("experiment_key"));
        exp.setActive(rs.getBoolean("active"));
        exp.setTreatmentPercentage(rs.getInt("treatment_percentage"));
        exp.setDescription(rs.getString("description"));
        return exp;
    }
}
