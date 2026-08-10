package com.nexora.platform.tenant;

import java.util.UUID;

public record TenantContext(
        UUID subjectId,
        UUID organizationId,
        UUID membershipId,
        long membershipVersion,
        String role) {
}
