package com.nexora.platform.rag.quality;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing RAG traces, quality evaluation reports, and user feedback.
 */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/rag", produces = MediaType.APPLICATION_JSON_VALUE)
public class RagQualityController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final RagQualityService quality;

    public RagQualityController(TenantContextService tenantContexts, RagQualityService quality) {
        this.tenantContexts = tenantContexts;
        this.quality = quality;
    }

    @GetMapping(path = "/traces")
    List<RagTrace> listTraces(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return quality.listTraces(context(jwt, organizationId), limit);
    }

    @GetMapping(path = "/traces/{runId}")
    RagTrace getTrace(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID runId) {
        return quality.getTrace(context(jwt, organizationId), runId);
    }

    @GetMapping(path = "/evaluation")
    RagEvaluationReport evaluation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId) {
        return quality.evaluate(context(jwt, organizationId));
    }

    @PostMapping(path = "/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    RagFeedback submitFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        return quality.submitFeedback(context(jwt, organizationId), request.runId(), request.rating(), request.comment());
    }

    @GetMapping(path = "/feedback")
    List<RagFeedback> listFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId) {
        return quality.listFeedback(context(jwt, organizationId));
    }

    @DeleteMapping(path = "/feedback/{feedbackId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID feedbackId) {
        quality.deleteFeedback(context(jwt, organizationId), feedbackId);
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    record SubmitFeedbackRequest(
            @NotNull UUID runId,
            @NotBlank @Pattern(regexp = "^(?i)(UP|DOWN)$") String rating,
            @Size(max = 2000) String comment) {
    }
}
