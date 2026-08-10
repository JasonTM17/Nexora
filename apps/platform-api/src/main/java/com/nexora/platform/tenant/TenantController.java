package com.nexora.platform.tenant;

import com.nexora.platform.identity.IdentityPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("database")
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class TenantController {
    static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenants;

    public TenantController(TenantContextService tenants) {
        this.tenants = tenants;
    }

    @PostMapping(path = "/tenant-context/resolve", consumes = MediaType.APPLICATION_JSON_VALUE)
    TenantContextResponse resolve(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ResolveTenantRequest request) {
        return TenantContextResponse.from(tenants.resolve(subject(jwt), request.organizationId()));
    }

    @GetMapping("/tenant-context")
    TenantContextResponse current(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = ORGANIZATION_HEADER, required = false) UUID organizationId) {
        return TenantContextResponse.from(tenants.resolve(subject(jwt), organizationId));
    }

    @GetMapping("/authorization/permission-matrix")
    PermissionMatrixResponse permissions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = ORGANIZATION_HEADER, required = false) UUID organizationId) {
        return tenants.withFreshTenant(subject(jwt), organizationId, (context, jdbc) -> {
            List<String> permissions = jdbc.queryForList("""
                    SELECT permission::text
                    FROM nexora.tenant_role_permissions
                    WHERE tenant_role = ?::nexora.tenant_role
                    ORDER BY permission::text
                    """, String.class, context.role());
            return new PermissionMatrixResponse(TenantContextResponse.from(context), permissions);
        });
    }

    private UUID subject(Jwt jwt) {
        return IdentityPrincipal.from(jwt).subjectId();
    }

    record ResolveTenantRequest(@NotNull UUID organizationId) {
    }

    record TenantContextResponse(
            UUID organizationId, UUID membershipId, long membershipVersion, String role) {
        static TenantContextResponse from(TenantContext context) {
            return new TenantContextResponse(context.organizationId(), context.membershipId(),
                    context.membershipVersion(), context.role());
        }
    }

    record PermissionMatrixResponse(TenantContextResponse context, List<String> permissions) {
    }
}
