package com.nexora.platform.events.admission;

import com.nexora.platform.identity.IdentityPrincipal;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

/**
 * Non-browser internal projection for the frozen Go ingress boundary. It is
 * intentionally hidden from generated OpenAPI and has no generated client.
 */
@Hidden
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/internal/event-admission", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventAdmissionController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final EventAdmissionService admissions;

    public EventAdmissionController(EventAdmissionService admissions) {
        this.admissions = admissions;
    }

    @PostMapping(path = "/publication-invalidated", consumes = MediaType.APPLICATION_JSON_VALUE)
    EventAdmissionService.AdmissionDecision authorizePublicationInvalidation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationCandidate,
            @Valid @RequestBody PublicationInvalidationCandidate candidate) {
        return admissions.authorizePublicationInvalidation(
                IdentityPrincipal.from(jwt).subjectId(), jwt.getExpiresAt(), organizationCandidate,
                new EventAdmissionService.CandidateEnvelope(
                        candidate.eventType(), candidate.resourceType(), candidate.resourceId(), candidate.eventVersion(),
                        candidate.schemaVersion()));
    }

    record PublicationInvalidationCandidate(
            @NotBlank @Pattern(regexp = "^PUBLICATION_INVALIDATED$") String eventType,
            @NotBlank @Pattern(regexp = "^page$") String resourceType,
            @NotNull UUID resourceId,
            @Min(1) @Max(9_007_199_254_740_991L) long eventVersion,
            @NotBlank @Pattern(regexp = "^1\\.1\\.0$") String schemaVersion) { }
}
