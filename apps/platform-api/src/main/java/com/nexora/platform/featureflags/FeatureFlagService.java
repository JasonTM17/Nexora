package com.nexora.platform.featureflags;

import com.nexora.platform.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-scoped feature flag evaluation with deterministic rollout.
 *
 * <p>A flag is enabled for a subject when the flag is globally enabled AND the
 * subject's stable hash falls within the rollout percentage. This gives
 * consistent per-subject experience across requests without storing per-subject
 * assignments.</p>
 */
@Service
@Profile("database")
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    /**
     * Evaluate a flag for the given subject within a tenant context.
     *
     * @param context tenant context carrying the organization/tenant
     * @param subjectId the actor to evaluate for (stable hash input)
     * @param flagKey the flag to evaluate
     * @return evaluation result with enabled state and variant
     */
    public FlagEvaluation evaluate(TenantContext context, UUID subjectId, String flagKey) {
        Optional<FeatureFlag> flag = repository
                .findByTenantIdAndFlagKey(context.organizationId(), flagKey);
        if (flag.isEmpty()) {
            return FlagEvaluation.disabled(flagKey);
        }
        FeatureFlag f = flag.get();
        if (!f.enabled()) {
            return FlagEvaluation.disabled(flagKey);
        }
        if (f.rolloutPercentage() >= 100) {
            return FlagEvaluation.enabled(flagKey, "treatment");
        }
        if (f.rolloutPercentage() <= 0) {
            return FlagEvaluation.disabled(flagKey);
        }
        // Deterministic: hash(tenantId:flagKey:subjectId) % 100 < rolloutPercentage
        int bucket = hashBucket(context.organizationId(), flagKey, subjectId);
        boolean inTreatment = bucket < f.rolloutPercentage();
        return inTreatment
                ? FlagEvaluation.enabled(flagKey, "treatment")
                : FlagEvaluation.disabled(flagKey, "control");
    }

    /** List all flags for a tenant with their current configuration. */
    public List<FeatureFlag> listFlags(TenantContext context) {
        return repository.findAllByTenantId(context.organizationId());
    }

    /** Create or update a flag configuration. */
    @Transactional
    public FeatureFlag upsertFlag(TenantContext context, UpsertFlagCommand command) {
        FeatureFlag flag = repository
                .findByTenantIdAndFlagKey(context.organizationId(), command.flagKey())
                .orElse(new FeatureFlag(UUID.randomUUID(), context.organizationId(), command.flagKey()));
        flag.setEnabled(command.enabled());
        flag.setRolloutPercentage(command.rolloutPercentage());
        flag.setRules(command.rules());
        flag.setDescription(command.description());
        return repository.save(flag);
    }

    private int hashBucket(UUID tenantId, String flagKey, UUID subjectId) {
        String input = tenantId.toString() + ":" + flagKey + ":" + subjectId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Use first 4 bytes as unsigned int, mod 100 for percentage bucket
            int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.abs(value) % 100;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present; fall back to hashCode
            return Math.abs(input.hashCode()) % 100;
        }
    }

    /** Result of a flag evaluation. */
    public record FlagEvaluation(String flagKey, boolean enabled, String variant) {
        static FlagEvaluation disabled(String flagKey) {
            return new FlagEvaluation(flagKey, false, "control");
        }
        static FlagEvaluation disabled(String flagKey, String variant) {
            return new FlagEvaluation(flagKey, false, variant);
        }
        static FlagEvaluation enabled(String flagKey, String variant) {
            return new FlagEvaluation(flagKey, true, variant);
        }
    }

    /** Command to create or update a flag. */
    public record UpsertFlagCommand(
            String flagKey,
            boolean enabled,
            int rolloutPercentage,
            Map<String, Object> rules,
            String description) {
    }
}
