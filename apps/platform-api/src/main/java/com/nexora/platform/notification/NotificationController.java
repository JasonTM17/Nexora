package com.nexora.platform.notification;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.notification.NotificationService.CreateNotificationCommand;
import com.nexora.platform.notification.NotificationService.Notification;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP projection for user-scoped notification management. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationController {

    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final NotificationService service;

    public NotificationController(TenantContextService tenantContexts, NotificationService service) {
        this.tenantContexts = tenantContexts;
        this.service = service;
    }

    /** List notifications for the authenticated user. */
    @GetMapping
    public List<Notification> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) UUID cursor) {
        UUID userId = IdentityPrincipal.from(jwt).subjectId();
        return service.listForUser(context(jwt, organizationId), userId, limit, cursor);
    }

    /** Count unread notifications. */
    @GetMapping(path = "/unread-count")
    public UnreadCount unreadCount(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId) {
        UUID userId = IdentityPrincipal.from(jwt).subjectId();
        return new UnreadCount(service.countUnread(context(jwt, organizationId), userId));
    }

    /** Mark a notification as read. */
    @PatchMapping(path = "/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID notificationId) {
        UUID userId = IdentityPrincipal.from(jwt).subjectId();
        service.markRead(context(jwt, organizationId), notificationId, userId);
    }

    /**
     * Create a notification (admin/system use). In production this is typically
     * called by internal services, not directly by users.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody CreateNotificationRequest request) {
        var command = new CreateNotificationCommand(
                request.userId(),
                request.notificationType(),
                request.priority() != null ? request.priority() : "normal",
                request.title(),
                request.body(),
                request.actionUrl(),
                request.metadata(),
                request.ttl());
        return new CreateResponse(service.notify(context(jwt, organizationId), command));
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    public record CreateNotificationRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 64) String notificationType,
            @Size(max = 16) String priority,
            @NotBlank @Size(max = 256) String title,
            @Size(max = 2000) String body,
            String actionUrl,
            Map<String, Object> metadata,
            OffsetDateTime ttl) {
    }

    public record UnreadCount(long count) {
    }

    public record CreateResponse(UUID id) {
    }
}
