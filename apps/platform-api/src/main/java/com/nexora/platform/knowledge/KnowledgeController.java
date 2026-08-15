package com.nexora.platform.knowledge;

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

/** HTTP projection for the tenant-scoped knowledge base and document lifecycle. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/knowledge", produces = MediaType.APPLICATION_JSON_VALUE)
public class KnowledgeController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final KnowledgeService knowledge;

    public KnowledgeController(TenantContextService tenantContexts, KnowledgeService knowledge) {
        this.tenantContexts = tenantContexts;
        this.knowledge = knowledge;
    }

    @GetMapping(path = "/knowledge-bases")
    KnowledgeService.KnowledgeBaseList listKnowledgeBases(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return knowledge.listKnowledgeBases(context(jwt, organizationId), cursor, limit);
    }

    @PostMapping(path = "/knowledge-bases", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    KnowledgeService.KnowledgeBaseView createKnowledgeBase(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return knowledge.createKnowledgeBase(context(jwt, organizationId), request.toCommand());
    }

    @DeleteMapping(path = "/knowledge-bases/{knowledgeBaseId}")
    KnowledgeService.KnowledgeBaseView deleteKnowledgeBase(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID knowledgeBaseId) {
        return knowledge.deleteKnowledgeBase(context(jwt, organizationId), knowledgeBaseId);
    }

    @GetMapping(path = "/knowledge-bases/{knowledgeBaseId}/documents")
    KnowledgeService.DocumentList listDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID knowledgeBaseId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return knowledge.listDocuments(context(jwt, organizationId), knowledgeBaseId, cursor, limit);
    }

    @PostMapping(path = "/knowledge-bases/{knowledgeBaseId}/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    KnowledgeService.DocumentView registerDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID knowledgeBaseId,
            @Valid @RequestBody RegisterDocumentRequest request) {
        return knowledge.registerDocument(context(jwt, organizationId), request.toCommand(knowledgeBaseId));
    }

    @PostMapping(path = "/documents/{documentId}/queue")
    KnowledgeService.DocumentView queueDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID documentId) {
        return knowledge.queueDocument(context(jwt, organizationId), documentId);
    }

    @DeleteMapping(path = "/documents/{documentId}")
    KnowledgeService.DocumentView deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID documentId) {
        return knowledge.deleteDocument(context(jwt, organizationId), documentId);
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    record CreateKnowledgeBaseRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description) {
        KnowledgeService.CreateKnowledgeBaseCommand toCommand() {
            return new KnowledgeService.CreateKnowledgeBaseCommand(name, description);
        }
    }

    record RegisterDocumentRequest(
            @NotBlank @Size(max = 255) String originalName,
            @NotBlank @Pattern(regexp = "^(application/pdf|text/markdown|text/plain)$") String contentType,
            @Min(0) @Max(52428800) long byteSize,
            @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String sha256) {
        KnowledgeService.RegisterDocumentCommand toCommand(UUID knowledgeBaseId) {
            return new KnowledgeService.RegisterDocumentCommand(knowledgeBaseId, originalName, contentType, byteSize, sha256);
        }
    }
}
