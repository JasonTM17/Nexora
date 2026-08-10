# Route and state inventory

## Status key

- **Implemented foundation:** observable at the pinned baseline only.
- **Planned:** product architecture; no route, API or browser behavior is
  implemented at the pinned baseline.
- **Future goal:** outside the v0.1 product route commitment and must not be
  surfaced as available work.

## Current foundation

| Status | Path | Contract and UI treatment |
| --- | --- | --- |
| Implemented foundation | `GET /api/v1/platform` | Developer/platform baseline only; do not position as an end-user dashboard. |
| Implemented foundation | `POST /api/v1/platform/echo` | Validation probe only. `400` returns `{ code, message, details, traceId }`; the `message` field is required and limited to 140 characters. |
| Implemented foundation | `/actuator/*`, `/v3/api-docs` | Operational/developer endpoints outside the planned product navigation. |

## Planned browser routes

| Planned route pattern | Audience and purpose | Required route states |
| --- | --- | --- |
| `/` and `/{published-path}` | Visitor reads a tenant/site-resolved published page | Loading skeleton; not found; access-limited; degraded/refetch; published content. Never draft content. |
| `/auth/sign-in`, `/auth/callback`, `/auth/recover` | Anonymous user establishes/recover session | Initial/loading; validation; provider/transport failure; expired/invalid session; return-safe success. |
| `/select-organization` | Authenticated user chooses an allowed membership | Loading; one membership auto-continue; chooser; empty membership; removed-membership/denied; retry. |
| `/studio` | Authorized member overview | Loading; first-use empty; partial/degraded data; denied; error/retry. No invented analytics. |
| `/studio/content/pages` | Creator/reviewer lists tenant pages | Loading; empty; pagination/end; filter no-results; error; denied. |
| `/studio/content/pages/new` | Creator starts typed metadata/draft | Form initial; field validation; slug conflict; save failure; denied; cancellation confirmation. |
| `/studio/content/pages/{pageId}` | Authorized member views page details | Loading; missing/archived-safe result; forbidden; stale/degraded; error. |
| `/studio/content/pages/{pageId}/builder` | Creator composes a draft | Unsaved/saving/saved; failed/offline/reconnecting/conflict; empty canvas; schema validation; denied; not-found; destructive/undo. |
| `/studio/content/pages/{pageId}/preview` | Authorized member reviews planned preview | Loading; validation-blocked; preview unavailable; denied; error. Always labelled preview. |
| `/studio/content/pages/{pageId}/review` | Reviewer/publisher makes workflow decision | Loading; no candidate; in-review; approved/rejected/published history; stale transition conflict; denied; error. |
| `/studio/content/pages/{pageId}/history` | Publisher inspects immutable versions / planned rollback | Loading; empty history; selection; rollback confirmation; duplicate/unknown status; denied; error. |
| `/studio/experience/themes` | Authorized owner edits constrained tenant/site theme tokens | Loading; empty/default; token validation and contrast feedback; preview-versus-published; saving/conflict; publish/rollback confirmation; denied; error. |
| `/studio/knowledge` | Knowledge manager lists knowledge bases | Loading; empty; no-results; error; denied. |
| `/studio/knowledge/{knowledgeBaseId}` | Knowledge manager supervises source list | Loading; empty; pagination; error; denied; degraded refresh. |
| `/studio/knowledge/{knowledgeBaseId}/documents/{documentId}` | Authorized manager sees document/job lifecycle | Queued/running/completed/failed/cancelled/retry; unknown after reconnect; deleted/missing-safe; denied. |
| `/studio/knowledge/chat` | Permitted member opens tenant-scoped chat | First-use empty; history loading/pagination; queued/streaming/cancelled/failed/completed/no-answer; citation unavailable; offline; denied. |
| `/studio/knowledge/quality` | Authorized administrator inspects safe retrieval traces and feedback | Loading; empty; filter no-results; redacted trace detail; error; denied. No fabricated quality metric or raw prompt/source text. |
| `/studio/organization/profile` | Member changes allowlisted preferences | Loading; form validation; saving/saved; stale-write conflict; error; denied. |
| `/studio/organization/members` | Authorized administrator manages memberships | Loading; empty; pagination/no-results; invite/change/remove confirmation; last-owner/invariant failure; denied; error. |
| `/studio/organization/roles` | Authorized administrator uses role matrix | Loading; matrix; loading permissions; save/conflict; insufficient grant authority; last-owner/invariant failure; denied; error. |
| `/studio/organization/settings` | Authorized owner manages permitted tenant settings | Loading; initial/empty; field validation; saving/saved; error; denied. |
| `/not-found`, `/access-denied`, `/offline` | Shared safe recovery destinations | Clear page heading, safe next action and no leaked resource metadata. |

## Required state behavior

| State | Visual/semantic contract | Recovery/action |
| --- | --- | --- |
| Loading | Layout-stable skeleton with a route-level `aria-busy` region; do not present fake content or metric values. | Allow safe navigation away; announce completion once. |
| Empty | Explain what is absent, why it might be absent and the permitted next action. | Offer create/upload only when allowed; otherwise offer refresh/back/help. |
| Denied | Neutral 401/403-safe messaging with no object existence disclosure. | Sign in, choose another allowed organization or return to a safe route. |
| Not found | Distinguish only when policy permits; default to a safe generic not-found result. | Return to list/home; never guess a private ID. |
| Validation | Inline errors, summary and focus management; preserve non-sensitive entries. | Correct and resubmit; server remains authoritative. |
| Conflict | Name the concurrency/status ambiguity and stop auto-overwrite/retry. | Refetch current truth; provide compare/copy/retry only under future contract. |
| Offline/degraded | State what is unavailable and what remains locally visible; do not imply changes will sync. | Retry/refetch; queue no sensitive mutation unless a later contract explicitly supports it. |
| Error | Give a stable, human-readable failure with trace ID only if returned; never surface stack traces, secrets or raw provider content. | Retry a safe idempotent read; contact support with safe trace ID. |
| Destructive confirmation | State object/effect and consequences in text; focus starts on dialog heading or least destructive control. | Explicit confirm/cancel; success refetches durable truth. |

## API-to-state boundary

The implemented Spring error envelope supplies `code`, human-readable `message`,
field `details` and `traceId`. Planned domain APIs should map their stable
authorization, validation, conflict and availability results into the route
states above. Until those API contracts exist, no frontend may infer a success,
membership, role, document status, source citation or publication result from a
client-side transition alone.
