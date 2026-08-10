package com.nexora.platform.realtime;

import java.util.UUID;

enum RealtimeChannel {
    PUBLICATION_INVALIDATED("tenant", "publication", "broadcast"),
    WORKFLOW_TRANSITIONED("tenant", "workflow", "broadcast"),
    JOB_PROGRESS_CHANGED("resource", "job-progress", "broadcast"),
    PRESENCE_CHANGED("resource", "presence", "presence");

    private final String scope;
    private final String purpose;
    private final String delivery;

    RealtimeChannel(String scope, String purpose, String delivery) {
        this.scope = scope;
        this.purpose = purpose;
        this.delivery = delivery;
    }

    String eventType() {
        return name();
    }

    long eventVersion() {
        return 1;
    }

    String delivery() {
        return delivery;
    }

    boolean requiresResource() {
        return "resource".equals(scope);
    }

    String topic(UUID organizationId, UUID resourceId) {
        UUID owner = requiresResource() ? resourceId : organizationId;
        if (owner == null) {
            throw new IllegalArgumentException("A resource id is required for " + name());
        }
        return scope + ":" + owner + ":" + purpose;
    }
}
