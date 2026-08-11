package com.nexora.platform.realtime;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/realtime/descriptors", produces = MediaType.APPLICATION_JSON_VALUE)
public class RealtimeDescriptorController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";

    private final TenantContextService tenantContexts;
    private final RealtimeDescriptorService descriptors;

    public RealtimeDescriptorController(TenantContextService tenantContexts, RealtimeDescriptorService descriptors) {
        this.tenantContexts = tenantContexts;
        this.descriptors = descriptors;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    RealtimeDescriptorService.Descriptor issue(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody DescriptorRequest request) {
        TenantContext context = tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
        return descriptors.issue(context, request.eventType(), request.resourceId());
    }

    record DescriptorRequest(@NotNull RealtimeChannel eventType, UUID resourceId) {
    }
}
