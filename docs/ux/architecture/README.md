# M1-D00 UX architecture

## Purpose and status

This is the pre-design information architecture for the Nexora v0.1 product
slice. It locks the journeys, route/state inventory and wireflows that later
design-direction and frontend work consume. It is **not** a screen design,
implementation contract, API specification or evidence that a planned feature
works.

The baseline observed for this document is `main` at
`2119e2b5ac2c0ffdc83de4a903f091ddba706951` (2026-08-10). At that revision the
only implemented browser-relevant Spring API surface is the platform foundation:

| Current endpoint | What a UI may truthfully show today |
| --- | --- |
| `GET /api/v1/platform` | API version, migration baseline and schema names. |
| `POST /api/v1/platform/echo` | A bounded (`1..140` characters) validation-contract probe. |
| `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/metrics`, `/v3/api-docs` | Operational/developer endpoints, not a product UI. |

There is no implemented auth, organization, CMS, publishing, knowledge or chat
endpoint at this baseline. Every product route and interaction in this directory
is therefore labelled **Planned**. Later workers must replace a planned label
only with exact-head implementation and validation evidence.

## Documents

| Document | Consumer decision it freezes |
| --- | --- |
| [Product journeys](./product-journeys.md) | Persona goals, authority boundaries and success/failure outcomes. |
| [Information architecture](./information-architecture.md) | Route families, navigation hierarchy and semantic landmarks. |
| [Route and state inventory](./route-state-inventory.md) | Required routes, API truth boundary and complete UI states. |
| [Wireflows](./wireflows.md) | Concrete task sequences, recovery paths and responsive behavior. |

## Non-negotiable UX rules

1. **Server authority wins.** A selected organization, visible action or route
   parameter is never proof of membership or permission. The planned Spring
   boundary reauthorizes each protected request; UI denial is explanatory only.
2. **Truth before liveness.** Durable API data is the source of truth. Planned
   Realtime, cache and streaming signals can improve freshness, but on a gap,
   reconnect or ambiguity the UI refetches durable state and says so.
3. **No false completion.** Saving, publishing, processing and AI answers have
   explicit lifecycle states. A queued, partial, failed or fixture result is
   never styled as completed/live.
4. **Accessible by construction.** Every route has a named main landmark, a
   skip link, visible keyboard focus, semantic headings and a current-page
   announcement. Pointer-only canvas actions always have a keyboard command
   equivalent.
5. **Responsive is capability-aware.** At 375px public, authentication,
   profile, review, knowledge and chat tasks remain usable. The planned desktop
   builder does not compress its canvas into an unusable mobile replica: it
   provides outline/property editing and preview, then clearly directs the user
   to a larger viewport for precision composition.

## Source boundary

This architecture derives from the approved product outcome and the planned
identity, RBAC, CMS, builder, publishing, workflow, knowledge and secure-RAG
phase contracts. It does not introduce roles, endpoint shapes, permissions,
provider behaviour, analytics claims, a visual direction or a new product
scope. The exact path owner for this work is `docs/ux/architecture/**`.
