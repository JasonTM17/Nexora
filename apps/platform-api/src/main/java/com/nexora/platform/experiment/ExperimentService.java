package com.nexora.platform.experiment;

import com.nexora.platform.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lightweight A/B experimentation framework built on the feature flag spine.
 *
 * <p>An experiment assigns subjects to control/treatment variants using the
 * same deterministic hashing as feature flags, ensuring stable per-subject
 * assignment without per-subject storage. Exposure events are recorded to the
 * analytics pipeline for later analysis.</p>
 */
@Service
@Profile("database")
@Transactional(readOnly = true)
public class ExperimentService {

    private final ExperimentRepository repository;

    public ExperimentService(ExperimentRepository repository) {
        this.repository = repository;
    }

    /**
     * Assign a subject to a variant for the given experiment.
     *
     * @param context tenant context
     * @param subjectId the actor to assign
     * @param experimentKey the experiment identifier
     * @return variant assignment (control/treatment)
     */
    public VariantAssignment assign(TenantContext context, UUID subjectId, String experimentKey) {
        Optional<Experiment> experiment = repository
                .findByTenantIdAndExperimentKey(context.organizationId(), experimentKey);
        if (experiment.isEmpty()) {
            return new VariantAssignment(experimentKey, "control", false);
        }
        Experiment exp = experiment.get();
        if (!exp.active()) {
            return new VariantAssignment(experimentKey, "control", false);
        }
        int bucket = hashBucket(context.organizationId(), experimentKey, subjectId);
        String variant = bucket < exp.treatmentPercentage() ? "treatment" : "control";
        return new VariantAssignment(experimentKey, variant, true);
    }

    /** List all experiments for a tenant. */
    public java.util.List<Experiment> listExperiments(TenantContext context) {
        return repository.findAllByTenantId(context.organizationId());
    }

    /** Create or update an experiment. */
    @Transactional
    public Experiment upsertExperiment(TenantContext context, UpsertExperimentCommand command) {
        Experiment exp = repository
                .findByTenantIdAndExperimentKey(context.organizationId(), command.experimentKey())
                .orElse(new Experiment(UUID.randomUUID(), context.organizationId(), command.experimentKey()));
        exp.setActive(command.active());
        exp.setTreatmentPercentage(command.treatmentPercentage());
        exp.setDescription(command.description());
        return repository.save(exp);
    }

    private int hashBucket(UUID tenantId, String experimentKey, UUID subjectId) {
        String input = tenantId.toString() + ":exp:" + experimentKey + ":" + subjectId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.abs(value) % 100;
        } catch (NoSuchAlgorithmException e) {
            return Math.abs(input.hashCode()) % 100;
        }
    }

    public record VariantAssignment(String experimentKey, String variant, boolean inExperiment) {
    }

    public record UpsertExperimentCommand(
            String experimentKey,
            boolean active,
            int treatmentPercentage,
            String description) {
    }
}
