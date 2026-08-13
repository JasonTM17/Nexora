package com.nexora.platform.events.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.authorization.PermissionEvaluator;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class EventAdmissionServiceTest {

    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID ORGANIZATION = UUID.randomUUID();
    private static final UUID PAGE = UUID.randomUUID();
    private static final TenantContext ACTOR = new TenantContext(
            SUBJECT, ORGANIZATION, UUID.randomUUID(), 1, "OWNER");

    private TenantContextService tenants;
    private PermissionEvaluator permissions;
    private JdbcTemplate jdbc;
    private EventAdmissionService admissions;

    @BeforeEach
    void setUp() {
        tenants = mock(TenantContextService.class);
        permissions = mock(PermissionEvaluator.class);
        jdbc = mock(JdbcTemplate.class);
        admissions = new EventAdmissionService(tenants, permissions);
    }

    @Test
    void rejectsABearerWhoseExpirySlipsIntoThePastDuringAuthorization() throws Exception {
        Instant shortLivedBearer = Instant.now().plusMillis(300);
        prepareFreshTenantAndPublishedPage(500);

        assertThatThrownBy(() -> admissions.authorizePublicationInvalidation(
                SUBJECT, shortLivedBearer, ORGANIZATION, canonicalCandidate()))
                .isInstanceOf(DomainAccessException.class);
        verify(permissions).require(jdbc, ACTOR, "page.publish");
    }

    @Test
    void rejectsABearerWhoseExpiryIsExactlyNow() {
        prepareFreshTenantAndPublishedPage();

        assertThatThrownBy(() -> admissions.authorizePublicationInvalidation(
                SUBJECT, Instant.now(), ORGANIZATION, canonicalCandidate()))
                .isInstanceOf(DomainAccessException.class);
        verify(tenants, never()).resolve(SUBJECT, ORGANIZATION);
    }

    @Test
    void rejectsAnAlreadyExpiredBearerAtTheEntryBoundary() {
        assertThatThrownBy(() -> admissions.authorizePublicationInvalidation(
                SUBJECT, Instant.now().minusSeconds(1), ORGANIZATION, canonicalCandidate()))
                .isInstanceOf(DomainAccessException.class);
        verify(tenants, never()).resolve(any(), any());
    }

    @Test
    void capsDecisionValidityAtTheShortDeadlineWhenTheBearerOutlivesIt() {
        prepareFreshTenantAndPublishedPage();

        EventAdmissionService.AdmissionDecision decision = admissions.authorizePublicationInvalidation(
                SUBJECT, Instant.now().plusSeconds(600), ORGANIZATION, canonicalCandidate());

        assertThat(decision.validUntil()).isAfter(Instant.now());
        assertThat(decision.validUntil()).isBeforeOrEqualTo(Instant.now().plusSeconds(31));
    }

    private void prepareFreshTenantAndPublishedPage() {
        prepareFreshTenantAndPublishedPage(0);
    }

    private void prepareFreshTenantAndPublishedPage(long authorizationDelayMillis) {
        when(tenants.resolve(SUBJECT, ORGANIZATION)).thenReturn(ACTOR);
        when(tenants.withFreshTenant(eq(ACTOR), any())).thenAnswer(invocation -> {
            if (authorizationDelayMillis > 0) {
                Thread.sleep(authorizationDelayMillis);
            }
            @SuppressWarnings("unchecked")
            BiFunction<TenantContext, JdbcTemplate, Object> work = invocation.getArgument(1);
            return work.apply(ACTOR, jdbc);
        });
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<Object>>any(), any(), any()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet result = mock(ResultSet.class);
                    when(result.getObject("id", UUID.class)).thenReturn(PAGE);
                    when(result.getLong("draft_version")).thenReturn(1L);
                    return List.of(mapper.mapRow(result, 0));
                });
    }

    private EventAdmissionService.CandidateEnvelope canonicalCandidate() {
        return new EventAdmissionService.CandidateEnvelope(
                "PUBLICATION_INVALIDATED", "page", PAGE, 1, "1.1.0");
    }
}
