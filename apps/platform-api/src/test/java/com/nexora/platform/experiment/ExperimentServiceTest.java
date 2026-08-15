package com.nexora.platform.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.nexora.platform.experiment.ExperimentService.VariantAssignment;
import com.nexora.platform.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for deterministic experiment variant assignment. */
@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock
    private ExperimentRepository repository;

    @InjectMocks
    private ExperimentService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final TenantContext context = new TenantContext(subjectId, orgId, null, 0L, "owner");

    @Test
    void missingExperimentReturnsControl() {
        when(repository.findByTenantIdAndExperimentKey(orgId, "unknown"))
                .thenReturn(Optional.empty());

        VariantAssignment result = service.assign(context, subjectId, "unknown");

        assertEquals("control", result.variant());
        assertFalse(result.inExperiment());
    }

    @Test
    void inactiveExperimentReturnsControl() {
        Experiment exp = new Experiment(UUID.randomUUID(), orgId, "exp-1");
        exp.setActive(false);
        when(repository.findByTenantIdAndExperimentKey(orgId, "exp-1"))
                .thenReturn(Optional.of(exp));

        VariantAssignment result = service.assign(context, subjectId, "exp-1");

        assertEquals("control", result.variant());
        assertFalse(result.inExperiment());
    }

    @Test
    void activeExperimentReturnsStableVariant() {
        Experiment exp = new Experiment(UUID.randomUUID(), orgId, "exp-1");
        exp.setActive(true);
        exp.setTreatmentPercentage(50);
        when(repository.findByTenantIdAndExperimentKey(orgId, "exp-1"))
                .thenReturn(Optional.of(exp));

        VariantAssignment first = service.assign(context, subjectId, "exp-1");
        VariantAssignment second = service.assign(context, subjectId, "exp-1");

        assertEquals(first.variant(), second.variant(), "Same subject must be stable");
        assertTrue(first.inExperiment());
    }

    @Test
    void fullTreatmentPercentageAssignsAllToTreatment() {
        Experiment exp = new Experiment(UUID.randomUUID(), orgId, "full-rollout");
        exp.setActive(true);
        exp.setTreatmentPercentage(100);
        when(repository.findByTenantIdAndExperimentKey(orgId, "full-rollout"))
                .thenReturn(Optional.of(exp));

        VariantAssignment result = service.assign(context, subjectId, "full-rollout");

        assertEquals("treatment", result.variant());
    }
}
