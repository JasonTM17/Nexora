package com.nexora.platform.rag.conversation;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

/** HTTP projection for the tenant plus subject scoped conversation contract. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/rag/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConversationController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final ConversationService conversations;

    public ConversationController(TenantContextService tenantContexts, ConversationService conversations) {
        this.tenantContexts = tenantContexts;
        this.conversations = conversations;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ConversationService.SessionView create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody CreateSessionRequest request) {
        return conversations.createSession(context(jwt, organizationId), request.title());
    }

    @PostMapping(path = "/{sessionId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ConversationService.MessageView send(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        return conversations.send(context(jwt, organizationId), sessionId, request.clientMessageId(), request.content());
    }

    @GetMapping(path = "/{sessionId}/messages")
    java.util.List<ConversationService.MessageView> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return conversations.history(context(jwt, organizationId), sessionId, cursor, limit);
    }

    @DeleteMapping(path = "/{sessionId}")
    ConversationService.SessionView delete(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID sessionId) {
        return conversations.deleteSession(context(jwt, organizationId), sessionId);
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    record CreateSessionRequest(@NotBlank @Size(max = 200) String title) {
    }

    record SendMessageRequest(
            @NotBlank @Size(max = 128) String clientMessageId,
            @NotBlank @Size(max = 100000) String content) {
    }
}
