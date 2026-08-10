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
