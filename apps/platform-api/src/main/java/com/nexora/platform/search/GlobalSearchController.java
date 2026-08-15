package com.nexora.platform.search;

import com.nexora.platform.identity.IdentityPrincipal;
import com.nexora.platform.search.GlobalSearchService.SearchResult;
import com.nexora.platform.search.GlobalSearchService.SearchResults;
import com.nexora.platform.tenant.TenantContext;
import com.nexora.platform.tenant.TenantContextService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP projection for authorized hybrid global search. */
@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class GlobalSearchController {

    private static final String ORGANIZATION_HEADER = "X-Nexora-Organization-Id";
    private final TenantContextService tenantContexts;
    private final GlobalSearchService service;

    public GlobalSearchController(TenantContextService tenantContexts, GlobalSearchService service) {
        this.tenantContexts = tenantContexts;
        this.service = service;
    }

    /** Search across authorized page and knowledge sources. */
    @GetMapping
    public SearchResponse search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(ORGANIZATION_HEADER) UUID organizationId,
            @RequestParam @NotBlank @Size(max = 200) String query,
            @RequestParam(defaultValue = "25") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) String cursor) {
        SearchResults results = service.search(context(jwt, organizationId), query, limit, cursor);
        return new SearchResponse(
                results.items().stream().map(SearchItemView::from).toList(),
                results.nextCursor(),
                results.totalCount());
    }

    private TenantContext context(Jwt jwt, UUID organizationId) {
        return tenantContexts.resolve(IdentityPrincipal.from(jwt).subjectId(), organizationId);
    }

    public record SearchResponse(List<SearchItemView> items, String nextCursor, int totalCount) {
    }

    public record SearchItemView(UUID id, String sourceType, String title, String snippet, double score) {
        static SearchItemView from(SearchResult result) {
            return new SearchItemView(result.id(), result.sourceType(), result.title(),
                    result.snippet(), result.score());
        }
    }
}
