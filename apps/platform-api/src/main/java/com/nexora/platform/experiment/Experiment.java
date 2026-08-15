package com.nexora.platform.experiment;

import java.util.UUID;

/** Tenant-scoped A/B experiment configuration. Plain carrier (no JPA). */
public class Experiment {

    private final UUID id;
    private final UUID tenantId;
    private final String experimentKey;
    private boolean active;
    private int treatmentPercentage;
    private String description;

    public Experiment(UUID id, UUID tenantId, String experimentKey) {
        this.id = id;
        this.tenantId = tenantId;
        this.experimentKey = experimentKey;
        this.active = false;
        this.treatmentPercentage = 50;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String experimentKey() { return experimentKey; }
    public boolean active() { return active; }
    public int treatmentPercentage() { return treatmentPercentage; }
    public String description() { return description; }

    public void setActive(boolean active) { this.active = active; }
    public void setTreatmentPercentage(int treatmentPercentage) { this.treatmentPercentage = treatmentPercentage; }
    public void setDescription(String description) { this.description = description; }
}
