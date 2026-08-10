package com.nexora.platform.tenant;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("database")
public class TransactionLocalDatabaseContext {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public TransactionLocalDatabaseContext(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public <T> T forSubject(UUID subjectId, Function<JdbcTemplate, T> work) {
        return execute(subjectId, null, null, work);
    }

    void promoteToTenant(TenantContext context) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Tenant context promotion requires an active transaction");
        }
        Map<String, Object> current = jdbc.queryForMap("""
                SELECT current_setting('nexora.subject_id', true) AS subject_id,
                       current_setting('nexora.organization_id', true) AS organization_id,
                       current_setting('nexora.membership_id', true) AS membership_id
                """);
        if (!context.subjectId().toString().equals(current.get("subject_id"))
                || !"".equals(current.get("organization_id"))
                || !"".equals(current.get("membership_id"))) {
            throw new IllegalStateException("Tenant context may only promote a matching subject-only transaction");
        }
        setLocal("nexora.organization_id", context.organizationId());
        setLocal("nexora.membership_id", context.membershipId());
    }

    void setTargetMembership(UUID membershipId, long version) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Target membership context requires an active transaction");
        }
        if (membershipId == null || version < 1) {
            throw new IllegalArgumentException("A target membership id and positive version are required");
        }
        setLocal("nexora.target_membership_id", membershipId);
        setLocal("nexora.target_membership_version", Long.toString(version));
        setLocal("nexora.target_membership_mutation", "");
    }

    <T> T withTargetMembershipMutation(Supplier<T> work) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Target membership mutation requires an active transaction");
        }
        setLocal("nexora.target_membership_mutation", "true");
        try {
            return work.get();
        } finally {
            setLocal("nexora.target_membership_mutation", "");
        }
    }

    private <T> T execute(
            UUID subjectId, UUID organizationId, UUID membershipId, Function<JdbcTemplate, T> work) {
        return transactions.execute(status -> {
            setLocal("nexora.subject_id", subjectId);
            setLocal("nexora.organization_id", organizationId);
            setLocal("nexora.membership_id", membershipId);
            return work.apply(jdbc);
        });
    }

    private void setLocal(String setting, UUID value) {
        setLocal(setting, value == null ? "" : value.toString());
    }

    private void setLocal(String setting, String value) {
        jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class, setting, value);
    }
}
