package com.nexora.platform.featureflags;

import java.util.Map;
import java.util.UUID;

/**
 * Tenant-scoped feature flag configuration. Plain carrier — persistence goes
 * through {@link FeatureFlagRepository} via JdbcTemplate (no JPA).
 */
public class FeatureFlag {

    private final UUID id;
    private final UUID tenantId;
    private final String flagKey;
    private boolean enabled;
    private int rolloutPercentage;
    private Map<String, Object> rules;
    private String description;

    public FeatureFlag(UUID id, UUID tenantId, String flagKey) {
        this.id = id;
        this.tenantId = tenantId;
        this.flagKey = flagKey;
        this.enabled = false;
        this.rolloutPercentage = 0;
        this.rules = Map.of();
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String flagKey() { return flagKey; }
    public boolean enabled() { return enabled; }
    public int rolloutPercentage() { return rolloutPercentage; }
    public Map<String, Object> rules() { return rules; }
    public String description() { return description; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRolloutPercentage(int rolloutPercentage) { this.rolloutPercentage = rolloutPercentage; }
    public void setRules(Map<String, Object> rules) { this.rules = rules; }
    public void setDescription(String description) { this.description = description; }
}
