package com.nexora.platform.identity;

import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/identity", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentityController {
    private final TenantContextService tenants;

    public IdentityController(TenantContextService tenants) {
        this.tenants = tenants;
    }

    @GetMapping("/access-context")
    AccessContextResponse accessContext(@AuthenticationPrincipal Jwt jwt) {
        IdentityPrincipal identity = IdentityPrincipal.from(jwt);
        List<MembershipResponse> memberships = tenants.accessContexts(identity.subjectId()).stream()
                .map(MembershipResponse::from)
                .toList();
        return new AccessContextResponse(identity.subjectId(), identity.sessionId(),
                identity.assuranceLevel(), memberships, memberships.size() > 1);
    }

    record AccessContextResponse(
            UUID subjectId,
            UUID sessionId,
            String assuranceLevel,
            List<MembershipResponse> memberships,
            boolean tenantSelectionRequired) {
    }

    record MembershipResponse(UUID organizationId, UUID membershipId, long membershipVersion, String role) {
        static MembershipResponse from(TenantContext context) {
            return new MembershipResponse(context.organizationId(), context.membershipId(),
                    context.membershipVersion(), context.role());
        }
    }
}
