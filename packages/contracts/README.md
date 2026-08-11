# Nexora platform contracts

`openapi/v1/openapi.json` is the versioned OpenAPI 3.1 authority for the
browser-facing `/api/v1` platform surface. `src/generated/platform-api.ts` is a
deterministic TypeScript client generated from that source; do not edit it by
hand.

The v1 contract freezes the safe API error body, bearer authentication default,
standard 401/403 responses and `X-Trace-Id` correlation header. Each operation
must either use bearer authentication and reference those responses, or declare
an explicit reviewed public exception. The two current loopback bootstrap
operations are explicit public exceptions, so the generated client does not
resolve or send an access token for them. Authentication does not imply
organization authorization. The generated client API exposes a bearer
access-token input and only the reviewed `X-Nexora-Organization-Id` selection
header on `getTenantContext`; that header is a selection candidate, never
tenant authority. It exposes no provider-credential or arbitrary-header input.

The protected M2 projection supplies generated request/response types for the
identity access context, explicit tenant-context resolution/selection, bounded
membership mutation, and allowlisted profile read/update routes. Membership
mutation requires a target path identifier, selected-organization header and
`expectedVersion`; the server still derives the acting membership and assignment
authority from current tenant state. Role/status input never grants authority,
and the client exposes no arbitrary-header input. Profile updates also carry
`expectedVersion` for optimistic conflict handling; profile fields never
authorize tenant access.

The membership-directory projection returns only the bounded authoritative rows
for one selected organization. It has no browser-controlled tenant, permission,
or filter input: the required organization header is a selection candidate, and
the server must re-resolve the acting ACTIVE membership and require both
`user.manage` and `role.manage` before returning the array. The contract does
not claim pagination, cross-tenant lookup, or a runtime authorization result.

The protected CMS projection freezes page drafting, immutable-version publication
or rollback receipts, workflow transitions, typed theme snapshots, and typed SEO
metadata. Every CMS request carries the reviewed organization-selection header;
the server must still derive fresh acting authority. Publication additionally
requires a bounded `Idempotency-Key`: identical same-scope retries reuse the
receipt, while a changed request fingerprint under the same key fails. Content
is represented only by a schema version and digest—never arbitrary executable
markup, provider data, or a client-provided audit record. These are contract
shapes only; CMS persistence, rendering, workflow execution, and public delivery
remain downstream implementation and evidence work.
Safe problem codes may use the established lowercase validation form or the
uppercase identity/domain form, while the envelope and detail-value guards stay
unchanged.

The M3 event contract freezes the versioned event envelope, private topic
vocabulary, outbox state semantics, and safe payload allowlist. Topics follow
the server-derived `scope:tenant-or-resource-id:purpose` rule, where the
identifier alone never grants access, and the event-type routing matrix ties
each published type to one allowed scope/purpose pair. Payloads carry only
allowlisted IDs, versions, job state, and fixed-shape safe display metadata;
safeDisplay is a fixed event-catalog label/status/variant tuple, never a
free-form bag or user-authored display copy. Event digests use the exact
`sha256:<64 lowercase hex>` wire format: `payloadDigest` is SHA-256 of the
domain-separated RFC 8785 canonical safe payload, while `idempotencyKeyDigest`
binds the server-derived routing operation and resource scope to the opaque key
without emitting that raw key. The contract fixture contains known-answer
vectors for both digests. All allowed scalar metadata is grammar-bounded:
UUID identity fields, lowercase resource types, fixed-width opaque trace and
correlation identifiers, route-specific finite resource types and job states,
and integer progress only;
free-form identifiers and invalid I-JSON text are rejected before digesting.
New producers emit schema `1.1.0`; ingress and
consumers reject legacy `1.0.0` free-form-display envelopes fail closed until a
later persistence owner records an explicit quarantine or conversion path. No
bodies, prompts, tokens, secrets, raw provider output, unowned tenant data, or
nested unsafe display content may appear. Outbox rows move through a bounded claim/failure/
dead-letter lifecycle with explicit lease and retry semantics. This contract is
a vocabulary only; M3-DB01 owns the migration DDL and M3-T02/T03/T04/T05 own
the runtime implementations and consumers.

The companion [Realtime channel contract](./realtime/v1/channel-contract.json)
freezes private-only, server-issued subscription descriptors consumed by
M3-T03. A normal browser session token is not a channel credential: the server
must issue a short-lived scoped Realtime JWT that binds the exact topic, event
route and current authorization epoch. Its `tenant`/`resource` wire topics are
the integrated M3 event-routing matrix; legacy `org/page/job` shapes are logical
server references, never browser-constructed Realtime topics. Browser code has
no direct `realtime.messages` write authority, including Presence; its bounded
presence intent goes to the same-origin M3-T03 adapter for server validation and
canonical delivery. The contract also specifies bounded reconnect,
token/membership invalidation, durable refetch and minimal presence behavior.

Problem `details` are limited to short printable validation messages or codes.
Detail keys are normalized from camelCase and `.`, `_`, `-` separators into
lowercase segments. The generated client rejects the entire problem envelope
when any key segment or value looks like authorization, credential,
provider/source, stack/exception, secret or opaque-token material; it does not
retain or echo the unsafe value. Benign validation keys such as `field` and
`message` remain valid.

From the repository root:

```powershell
node packages/contracts/scripts/generate-client.mjs
node packages/contracts/scripts/check-generated.mjs
node --test packages/contracts/tests/*.test.mjs
corepack pnpm@11.0.9 exec tsc -p packages/contracts/tsconfig.json
```

The generator intentionally uses only Node.js built-ins. This keeps generation
inside the frozen dependency window and makes source/client drift independently
checkable without changing package manifests or the workspace lockfile.
