package com.nexora.platform.experiment;

import com.nexora.platform.experiment.ExperimentService.UpsertExperimentCommand;
import com.nexora.platform.experiment.ExperimentService.VariantAssignment;
import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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

/** HTTP projection for tenant-scoped A/B experiment management. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/experiments", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentController {

    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final ExperimentService service;

    public ExperimentController(TenantContextService tenantContexts, ExperimentService service) {
        this.tenantContexts = tenantContexts;
        this.service = service;
    }

    /** Assign the authenticated subject to a variant for an experiment. */
    @GetMapping(path = "/{experimentKey}/assign")
    public VariantAssignment assign(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable @NotBlank String experimentKey) {
        return service.assign(context(jwt, organizationId), IdentityPrincipal.from(jwt).subjectId(), experimentKey);
    }

    /** List all experiments for the tenant. */
    @GetMapping
    public List<ExperimentView> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId) {
        return service.listExperiments(context(jwt, organizationId)).stream()
                .map(ExperimentView::from)
                .toList();
    }

    /** Create or update an experiment. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExperimentView upsert(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody UpsertRequest request) {
        var command = new UpsertExperimentCommand(
                request.experimentKey(), request.active(), request.treatmentPercentage(), request.description());
        return ExperimentView.from(service.upsertExperiment(context(jwt, organizationId), command));
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 128) String experimentKey,
            boolean active,
            @Min(0) @Max(100) Integer treatmentPercentage,
            @Size(max = 512) String description) {
    }

    public record ExperimentView(
            UUID id,
            String experimentKey,
            boolean active,
            int treatmentPercentage,
            String description) {
        static ExperimentView from(Experiment exp) {
            return new ExperimentView(exp.id(), exp.experimentKey(), exp.active(),
                    exp.treatmentPercentage(), exp.description());
        }
    }
}
