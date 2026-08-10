# Nexora platform contracts

`openapi/v1/openapi.json` is the versioned OpenAPI 3.1 authority for the
browser-facing `/api/v1` platform surface. `src/generated/platform-api.ts` is a
deterministic TypeScript client generated from that source; do not edit it by
hand.

The v1 contract freezes the safe API error body, bearer authentication scheme,
standard 401/403 responses and `X-Trace-Id` correlation header. Authentication
does not imply organization authorization. The generated client API exposes a
bearer access-token input, but no provider-credential or arbitrary-header input.

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
