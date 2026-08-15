package com.nexora.platform.featureflags;

import com.nexora.platform.featureflags.FeatureFlagService.FlagEvaluation;
import com.nexora.platform.featureflags.FeatureFlagService.UpsertFlagCommand;
import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP projection for tenant-scoped feature flag evaluation and management. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/feature-flags", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureFlagController {

    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final FeatureFlagService service;

    public FeatureFlagController(TenantContextService tenantContexts, FeatureFlagService service) {
        this.tenantContexts = tenantContexts;
        this.service = service;
    }

    /** Evaluate a single flag for the authenticated subject. */
    @GetMapping(path = "/{flagKey}/evaluate")
    public FlagEvaluation evaluate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable @NotBlank String flagKey) {
        TenantContext context = context(jwt, organizationId);
        return service.evaluate(context, IdentityPrincipal.from(jwt).subjectId(), flagKey);
    }

    /** List all flags for the tenant. */
    @GetMapping
    public java.util.List<FeatureFlagView> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId) {
        return service.listFlags(context(jwt, organizationId)).stream()
                .map(FeatureFlagView::from)
                .toList();
    }

    /** Create or update a flag configuration. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public FeatureFlagView upsert(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody UpsertRequest request) {
        var command = new UpsertFlagCommand(
                request.flagKey(), request.enabled(), request.rolloutPercentage(),
                request.rules() != null ? request.rules() : Map.of(),
                request.description());
        return FeatureFlagView.from(service.upsertFlag(context(jwt, organizationId), command));
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 128) String flagKey,
            boolean enabled,
            @Min(0) @Max(100) Integer rolloutPercentage,
            Map<String, Object> rules,
            @Size(max = 512) String description) {
    }

    public record FeatureFlagView(
            UUID id,
            String flagKey,
            boolean enabled,
            int rolloutPercentage,
            Map<String, Object> rules,
            String description) {
        static FeatureFlagView from(FeatureFlag flag) {
            return new FeatureFlagView(flag.id(), flag.flagKey(), flag.enabled(),
                    flag.rolloutPercentage(), flag.rules(), flag.description());
        }
    }
}
