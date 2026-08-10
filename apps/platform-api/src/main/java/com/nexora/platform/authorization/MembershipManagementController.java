package com.nexora.platform.authorization;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/authorization/memberships", produces = MediaType.APPLICATION_JSON_VALUE)
public class MembershipManagementController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final MembershipManagementService memberships;

    public MembershipManagementController(
            TenantContextService tenantContexts, MembershipManagementService memberships) {
        this.tenantContexts = tenantContexts;
        this.memberships = memberships;
    }

    @PatchMapping(path = "/{membershipId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    MembershipManagementService.MembershipView update(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody MembershipMutationRequest request) {
        TenantContext actor = tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
        if (request.role() != null && request.status() != null) {
            throw validationFailure();
        }
        if (request.role() != null) {
            return memberships.assignRole(actor, membershipId, request.expectedVersion(), request.role());
        }
        if (request.status() != null) {
            try {
                return memberships.changeStatus(
                        actor,
                        membershipId,
                        request.expectedVersion(),
                        MembershipManagementService.MembershipStatus.valueOf(request.status()));
            } catch (IllegalArgumentException exception) {
                throw validationFailure();
            }
        }
        throw validationFailure();
    }

    private DomainAccessException validationFailure() {
        return new DomainAccessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.");
    }

    record MembershipMutationRequest(
            @NotNull @Min(1) Long expectedVersion,
            String role,
            String status) {
    }
}
