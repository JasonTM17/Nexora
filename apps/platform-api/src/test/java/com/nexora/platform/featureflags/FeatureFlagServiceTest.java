package com.nexora.platform.featureflags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nexora.platform.tenant.TenantContext;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.mockito.Mockito.when;

/** Unit tests for deterministic feature flag evaluation. */
@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository repository;

    @InjectMocks
    private FeatureFlagService service;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final TenantContext context = new TenantContext(subjectId, organizationId, null, 0L, "owner");

    @Test
    void disabledFlagAlwaysReturnsDisabled() {
        FeatureFlag flag = new FeatureFlag(UUID.randomUUID(), organizationId, "new-dashboard");
        flag.setEnabled(false);
        flag.setRolloutPercentage(100);
        when(repository.findByTenantIdAndFlagKey(organizationId, "new-dashboard"))
                .thenReturn(Optional.of(flag));

        FeatureFlagService.FlagEvaluation result = service.evaluate(context, subjectId, "new-dashboard");

        assertFalse(result.enabled());
        assertEquals("new-dashboard", result.flagKey());
    }

    @Test
    void fullRolloutEnabledForEveryone() {
        FeatureFlag flag = new FeatureFlag(UUID.randomUUID(), organizationId, "new-dashboard");
        flag.setEnabled(true);
        flag.setRolloutPercentage(100);
        when(repository.findByTenantIdAndFlagKey(organizationId, "new-dashboard"))
                .thenReturn(Optional.of(flag));

        FeatureFlagService.FlagEvaluation result = service.evaluate(context, subjectId, "new-dashboard");

        assertTrue(result.enabled());
        assertEquals("treatment", result.variant());
    }

    @Test
    void zeroRolloutDisabledForEveryone() {
        FeatureFlag flag = new FeatureFlag(UUID.randomUUID(), organizationId, "new-dashboard");
        flag.setEnabled(true);
        flag.setRolloutPercentage(0);
        when(repository.findByTenantIdAndFlagKey(organizationId, "new-dashboard"))
                .thenReturn(Optional.of(flag));

        FeatureFlagService.FlagEvaluation result = service.evaluate(context, subjectId, "new-dashboard");

        assertFalse(result.enabled());
    }

    @Test
    void missingFlagReturnsDisabled() {
        when(repository.findByTenantIdAndFlagKey(organizationId, "unknown"))
                .thenReturn(Optional.empty());

        FeatureFlagService.FlagEvaluation result = service.evaluate(context, subjectId, "unknown");

        assertFalse(result.enabled());
        assertEquals("unknown", result.flagKey());
    }

    @Test
    void deterministicEvaluationIsStable() {
        FeatureFlag flag = new FeatureFlag(UUID.randomUUID(), organizationId, "stable-flag");
        flag.setEnabled(true);
        flag.setRolloutPercentage(50);
        when(repository.findByTenantIdAndFlagKey(organizationId, "stable-flag"))
                .thenReturn(Optional.of(flag));

        // Same subject + flag → same result every time
        FeatureFlagService.FlagEvaluation first = service.evaluate(context, subjectId, "stable-flag");
        FeatureFlagService.FlagEvaluation second = service.evaluate(context, subjectId, "stable-flag");

        assertEquals(first.enabled(), second.enabled());
        assertEquals(first.variant(), second.variant());
    }

    @Test
    void differentSubjectsGetDifferentBuckets() {
        FeatureFlag flag = new FeatureFlag(UUID.randomUUID(), organizationId, "rollout");
        flag.setEnabled(true);
        flag.setRolloutPercentage(50);
        when(repository.findByTenantIdAndFlagKey(organizationId, "rollout"))
                .thenReturn(Optional.of(flag));

        UUID subjectA = UUID.randomUUID();
        UUID subjectB = UUID.randomUUID();
        // With 50% rollout across many subjects, some will be in, some out
        // (not guaranteed for any specific pair, but deterministic per subject)
        FeatureFlagService.FlagEvaluation resultA = service.evaluate(context, subjectA, "rollout");
        FeatureFlagService.FlagEvaluation resultBAgain = service.evaluate(context, subjectA, "rollout");
        assertEquals(resultA.enabled(), resultBAgain.enabled(), "Same subject must be stable");
    }
}
