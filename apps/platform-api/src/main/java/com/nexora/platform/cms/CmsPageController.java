package com.nexora.platform.cms;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
import com.nexora.platform.observability.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP projection for the C02 tenant-scoped mutable page contract. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/cms/pages", produces = MediaType.APPLICATION_JSON_VALUE)
public class CmsPageController {
    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final CmsPageService pages;

    public CmsPageController(TenantContextService tenantContexts, CmsPageService pages) {
        this.tenantContexts = tenantContexts;
        this.pages = pages;
    }

    @GetMapping
    CmsPageService.PageList list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return pages.list(context(jwt, organizationId), cursor, limit);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CmsPageService.PageView create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @Valid @RequestBody CreatePageRequest request,
            HttpServletRequest servletRequest) {
        return pages.create(context(jwt, organizationId), request.toCommand(), traceId(servletRequest));
    }

    @GetMapping(path = "/{pageId}")
    CmsPageService.PageView get(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID pageId) {
        return pages.get(context(jwt, organizationId), pageId);
    }

    @PatchMapping(path = "/{pageId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    CmsPageService.PageView update(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID pageId,
            @Valid @RequestBody UpdatePageRequest request,
            HttpServletRequest servletRequest) {
        return pages.update(context(jwt, organizationId), pageId, request.toCommand(), traceId(servletRequest));
    }

    @DeleteMapping(path = "/{pageId}")
    CmsPageService.ArchiveResult archive(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @PathVariable UUID pageId,
            @RequestParam @Min(1) long expectedDraftVersion,
            HttpServletRequest servletRequest) {
        return pages.archive(context(jwt, organizationId), pageId, expectedDraftVersion, traceId(servletRequest));
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.ATTRIBUTE);
    }

    record CreatePageRequest(
            @NotNull UUID siteId,
            @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Pattern(regexp = "^[1-9][0-9]*\\.[0-9]+\\.[0-9]+$") String schemaVersion,
            @NotBlank @Pattern(regexp = "^sha256:[a-f0-9]{64}$") String contentDigest,
            @NotNull UUID themeVersionId,
            @NotNull @Valid SeoRequest seo) {
        CmsPageService.CreateCommand toCommand() {
            return new CmsPageService.CreateCommand(siteId, slug, title, schemaVersion, contentDigest, themeVersionId,
                    seo.toSnapshot());
        }
    }

    record UpdatePageRequest(
            @Min(1) long expectedDraftVersion,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Pattern(regexp = "^[1-9][0-9]*\\.[0-9]+\\.[0-9]+$") String schemaVersion,
            @NotBlank @Pattern(regexp = "^sha256:[a-f0-9]{64}$") String contentDigest,
            @NotNull UUID themeVersionId,
            @NotNull @Valid SeoRequest seo) {
        CmsPageService.UpdateCommand toCommand() {
            return new CmsPageService.UpdateCommand(expectedDraftVersion, title, schemaVersion, contentDigest,
                    themeVersionId, seo.toSnapshot());
        }
    }

    record SeoRequest(
            @NotBlank @Size(max = 70) String title,
            @NotBlank @Size(max = 160) String description,
            @NotBlank @Pattern(regexp = "^[a-z]{2,3}(?:-[A-Z]{2})?$") String locale,
            @NotBlank @Size(max = 240) @Pattern(regexp = "^/[a-z0-9]+(?:[/-][a-z0-9]+)*$") String canonicalPath,
            @NotNull @Valid OpenGraphRequest openGraph,
            @NotNull @Valid TwitterRequest twitter,
            @NotBlank @Pattern(regexp = "^(WebPage|Article)$") String jsonLdType) {
        CmsPageService.SeoSnapshot toSnapshot() {
            return new CmsPageService.SeoSnapshot(title, description, locale, canonicalPath,
                    openGraph.title(), openGraph.description(), openGraph.imageAssetId(), openGraph.type(),
                    twitter.card(), twitter.title(), twitter.description(), twitter.imageAssetId(), jsonLdType);
        }
    }

    record OpenGraphRequest(
            @NotBlank @Size(max = 70) String title,
            @NotBlank @Size(max = 160) String description,
            UUID imageAssetId,
            @NotBlank @Pattern(regexp = "^(website|article)$") String type) { }

    record TwitterRequest(
            @NotBlank @Pattern(regexp = "^(summary|summary_large_image)$") String card,
            @NotBlank @Size(max = 70) String title,
            @NotBlank @Size(max = 160) String description,
            UUID imageAssetId) { }
}
