package com.nexora.platform.tenant;

import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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

    public <T> T forTenant(TenantContext context, Function<JdbcTemplate, T> work) {
        return execute(context.subjectId(), context.organizationId(), context.membershipId(), work);
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
        jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class,
                setting, value == null ? "" : value.toString());
    }
}
