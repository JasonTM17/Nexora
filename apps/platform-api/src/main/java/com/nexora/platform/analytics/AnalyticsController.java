package com.nexora.platform.analytics;

import com.nexora.platform.analytics.AnalyticsService.AnalyticsEvent;
import com.nexora.platform.analytics.AnalyticsService.EventCount;
import com.nexora.platform.analytics.AnalyticsService.RecordEventCommand;
import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP projection for product analytics event collection and aggregation. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
public class AnalyticsController {

    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final AnalyticsService service;

    public AnalyticsController(TenantContextService tenantContexts, AnalyticsService service) {
        this.tenantContexts = tenantContexts;
        this.service = service;
    }

    /** Record an analytics event. */
    @PostMapping(path = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void recordEvent(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody RecordEventRequest request) {
        var command = new RecordEventCommand(
                request.eventType(),
                request.resourceType(),
                request.resourceId(),
                request.properties() != null ? request.properties() : Map.of(),
                request.clientContext() != null ? request.clientContext() : Map.of(),
                request.idempotencyKey());
        service.recordEvent(context(jwt, organizationId), command);
    }

    /** Aggregate event counts by type since a timestamp. */
    @GetMapping(path = "/aggregate")
    public List<EventCount> aggregate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        return service.aggregateByType(context(jwt, organizationId), since);
    }

    /** Recent events, cursor-paginated. */
    @GetMapping(path = "/events")
    public List<AnalyticsEvent> recentEvents(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) UUID cursor) {
        return service.recentEvents(context(jwt, organizationId), limit, cursor);
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    public record RecordEventRequest(
            @NotBlank @Size(max = 128) String eventType,
            @Size(max = 64) String resourceType,
            UUID resourceId,
            Map<String, Object> properties,
            Map<String, Object> clientContext,
            @Size(max = 256) String idempotencyKey) {
    }
}
