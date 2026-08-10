import assert from "node:assert/strict";
import test from "node:test";
import {
  NexoraApiError,
  PlatformApiClient,
} from "../src/generated/platform-api.ts";

test("keeps bearer credentials off an explicitly public operation while sending trace context", async () => {
  let observedUrl;
  let observedInit;
  let tokenReads = 0;
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test/",
    accessToken: async () => {
      tokenReads += 1;
      return "short-lived-token";
    },
    fetch: async (url, init) => {
      observedUrl = url;
      observedInit = init;
      return new Response(JSON.stringify({ apiVersion: "v1", migrationBaseline: "001", schemas: ["core"] }), {
        status: 200,
        headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-client-1" },
      });
    },
  });

  const response = await client.getPlatform({ traceId: "trace-client-1" });

  assert.equal(observedUrl, "https://nexora.test/api/v1/platform");
  assert.equal(observedInit.method, "GET");
  assert.equal(tokenReads, 0);
  assert.equal(observedInit.headers.get("Authorization"), null);
  assert.equal(observedInit.headers.get("X-Trace-Id"), "trace-client-1");
  assert.equal(response.traceId, "trace-client-1");
  assert.equal(response.data.apiVersion, "v1");
});

test("projects protected identity, tenant selection, and profile calls with the generated shared types", async () => {
  const requests = [];
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test/",
    accessToken: "short-lived-token",
    fetch: async (url, init) => {
      requests.push({ url, init });
      const path = new URL(url).pathname;
      const body = path === "/api/v1/identity/access-context"
        ? {
            subjectId: "10000000-0000-4000-8000-000000000001",
            sessionId: "20000000-0000-4000-8000-000000000001",
            assuranceLevel: "aal2",
            memberships: [],
            tenantSelectionRequired: false,
          }
        : path === "/api/v1/authorization/permission-matrix"
          ? {
              context: {
                organizationId: "30000000-0000-4000-8000-000000000001",
                membershipId: "40000000-0000-4000-8000-000000000001",
                membershipVersion: 1,
                role: "USER",
              },
              permissions: ["organization.read"],
            }
          : path === "/api/v1/profile"
          ? {
              subjectId: "10000000-0000-4000-8000-000000000001",
              displayName: "Ada",
              locale: "en-US",
              reducedMotion: false,
              highContrast: false,
              version: 1,
            }
          : {
              organizationId: "30000000-0000-4000-8000-000000000001",
              membershipId: "40000000-0000-4000-8000-000000000001",
              membershipVersion: 1,
              role: "USER",
            };
      return new Response(JSON.stringify(body), {
        status: 200,
        headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-m2-projection" },
      });
    },
  });

  await client.getAccessContext();
  await client.resolveTenantContext({ organizationId: "30000000-0000-4000-8000-000000000001" });
  await client.getTenantContext({ organizationId: "30000000-0000-4000-8000-000000000001" });
  await client.getPermissionMatrix({ organizationId: "30000000-0000-4000-8000-000000000001" });
  await client.getProfile();
  await client.updateProfile({
    displayName: "Ada",
    locale: "en-US",
    reducedMotion: false,
    highContrast: false,
    expectedVersion: 1,
  });

  assert.equal(requests.length, 6);
  for (const { init } of requests) assert.equal(init.headers.get("Authorization"), "Bearer short-lived-token");
  assert.equal(requests[2].init.headers.get("X-Nexora-Organization-Id"), "30000000-0000-4000-8000-000000000001");
  assert.equal(requests[3].init.headers.get("X-Nexora-Organization-Id"), "30000000-0000-4000-8000-000000000001");
  assert.equal(requests[1].init.body, JSON.stringify({ organizationId: "30000000-0000-4000-8000-000000000001" }));
  assert.equal(requests[5].init.method, "PUT");
  assert.equal(requests[5].init.headers.get("Content-Type"), "application/json");
});

test("projects the protected optimistic membership mutation with a generated path and selected-tenant header", async () => {
  let observedUrl;
  let observedInit;
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test/",
    accessToken: "short-lived-token",
    fetch: async (url, init) => {
      observedUrl = url;
      observedInit = init;
      return new Response(JSON.stringify({
        membershipId: "40000000-0000-4000-8000-000000000001",
        organizationId: "30000000-0000-4000-8000-000000000001",
        subjectId: "10000000-0000-4000-8000-000000000001",
        status: "ACTIVE",
        role: "REVIEWER",
        version: 2,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-membership-mutation" },
      });
    },
  });

  const response = await client.updateMembership(
    { membershipId: "40000000-0000-4000-8000-000000000001" },
    { expectedVersion: 1, role: "REVIEWER" },
    { organizationId: "30000000-0000-4000-8000-000000000001" },
    { traceId: "trace-membership-mutation" },
  );

  assert.equal(observedUrl, "https://nexora.test/api/v1/authorization/memberships/40000000-0000-4000-8000-000000000001");
  assert.equal(observedInit.method, "PATCH");
  assert.equal(observedInit.headers.get("Authorization"), "Bearer short-lived-token");
  assert.equal(observedInit.headers.get("X-Nexora-Organization-Id"), "30000000-0000-4000-8000-000000000001");
  assert.equal(observedInit.headers.get("X-Trace-Id"), "trace-membership-mutation");
  assert.equal(observedInit.headers.get("Content-Type"), "application/json");
  assert.equal(observedInit.body, JSON.stringify({ expectedVersion: 1, role: "REVIEWER" }));
  assert.equal(response.data.role, "REVIEWER");
  assert.equal(response.data.version, 2);
  assert.equal(response.traceId, "trace-membership-mutation");
});

test("projects CMS publication with generated component parameters and idempotency", async () => {
  let observedUrl;
  let observedInit;
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test/",
    accessToken: "short-lived-token",
    fetch: async (url, init) => {
      observedUrl = url;
      observedInit = init;
      return new Response(JSON.stringify({
        receiptId: "70000000-0000-4000-8000-000000000001",
        operation: "PUBLISH",
        pageId: "50000000-0000-4000-8000-000000000001",
        publishedVersionId: "60000000-0000-4000-8000-000000000001",
        sourceDraftVersion: 3,
        schemaVersion: "1.0.0",
        themeVersionId: "80000000-0000-4000-8000-000000000001",
        seoSnapshotDigest: "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        createdAt: "2026-08-10T00:00:00Z",
      }), {
        status: 201,
        headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-cms-publish" },
      });
    },
  });

  const response = await client.publishCmsPage(
    { pageId: "50000000-0000-4000-8000-000000000001" },
    { operation: "PUBLISH", expectedDraftVersion: 3 },
    {
      organizationId: "30000000-0000-4000-8000-000000000001",
      idempotencyKey: "cms-publish-tenant-a-page-500-v3",
    },
    { traceId: "trace-cms-publish" },
  );

  assert.equal(observedUrl, "https://nexora.test/api/v1/cms/pages/50000000-0000-4000-8000-000000000001/publication");
  assert.equal(observedInit.method, "POST");
  assert.equal(observedInit.headers.get("Authorization"), "Bearer short-lived-token");
  assert.equal(observedInit.headers.get("X-Nexora-Organization-Id"), "30000000-0000-4000-8000-000000000001");
  assert.equal(observedInit.headers.get("Idempotency-Key"), "cms-publish-tenant-a-page-500-v3");
  assert.equal(observedInit.headers.get("X-Trace-Id"), "trace-cms-publish");
  assert.equal(observedInit.body, JSON.stringify({ operation: "PUBLISH", expectedDraftVersion: 3 }));
  assert.equal(response.data.operation, "PUBLISH");
  assert.equal(response.data.sourceDraftVersion, 3);
  assert.equal(response.traceId, "trace-cms-publish");
});

test("serializes typed CMS page cursor and limit query parameters", async () => {
  let observedUrl;
  let observedInit;
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test/",
    accessToken: "short-lived-token",
    fetch: async (url, init) => {
      observedUrl = url;
      observedInit = init;
      return new Response(JSON.stringify({ items: [], nextCursor: null }), {
        status: 200,
        headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-cms-list" },
      });
    },
  });

  const response = await client.listCmsPages(
    { organizationId: "30000000-0000-4000-8000-000000000001" },
    { cursor: "cursor:page-2", limit: 10 },
    { traceId: "trace-cms-list" },
  );

  assert.equal(observedUrl, "https://nexora.test/api/v1/cms/pages?cursor=cursor%3Apage-2&limit=10");
  assert.equal(observedInit.method, "GET");
  assert.equal(observedInit.headers.get("Authorization"), "Bearer short-lived-token");
  assert.equal(observedInit.headers.get("X-Nexora-Organization-Id"), "30000000-0000-4000-8000-000000000001");
  assert.equal(observedInit.headers.get("X-Trace-Id"), "trace-cms-list");
  assert.equal(response.data.nextCursor, null);
});

test("serializes the required CMS archive concurrency query parameter", async () => {
  let observedUrl;
  let observedInit;
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test/",
    accessToken: "short-lived-token",
    fetch: async (url, init) => {
      observedUrl = url;
      observedInit = init;
      return new Response(JSON.stringify({
        pageId: "50000000-0000-4000-8000-000000000001",
        state: "ARCHIVED",
        archivedAt: "2026-08-10T00:00:00Z",
      }), {
        status: 200,
        headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-cms-archive" },
      });
    },
  });

  const response = await client.archiveCmsPage(
    { pageId: "50000000-0000-4000-8000-000000000001" },
    { organizationId: "30000000-0000-4000-8000-000000000001" },
    { expectedDraftVersion: 3 },
    { traceId: "trace-cms-archive" },
  );

  assert.equal(observedUrl, "https://nexora.test/api/v1/cms/pages/50000000-0000-4000-8000-000000000001?expectedDraftVersion=3");
  assert.equal(observedInit.method, "DELETE");
  assert.equal(observedInit.headers.get("Authorization"), "Bearer short-lived-token");
  assert.equal(observedInit.headers.get("X-Nexora-Organization-Id"), "30000000-0000-4000-8000-000000000001");
  assert.equal(observedInit.headers.get("X-Trace-Id"), "trace-cms-archive");
  assert.equal(response.data.state, "ARCHIVED");
});

test("surfaces only a valid safe problem envelope", async () => {
  const problem = {
    code: "validation_failed",
    message: "Request validation failed.",
    details: { field: "must not be blank", message: "invalid value" },
    traceId: "trace-validation-1",
  };
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test",
    fetch: async () => new Response(JSON.stringify(problem), {
      status: 400,
      headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-header-1" },
    }),
  });

  await assert.rejects(
    client.getPlatform(),
    (error) => {
      assert.ok(error instanceof NexoraApiError);
      assert.equal(error.status, 400);
      assert.deepEqual(error.problem, problem);
      assert.equal(error.traceId, "trace-validation-1");
      return true;
    },
  );
});

test("rejects an unsafe error payload instead of retaining extra fields", async () => {
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test",
    fetch: async () => new Response(JSON.stringify({
      code: "internal_error",
      message: "An unexpected error occurred.",
      details: {},
      traceId: "trace-safe-1",
      stack: "must not escape",
    }), { status: 500, headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-safe-1" } }),
  });

  await assert.rejects(
    client.getPlatform(),
    (error) => {
      assert.ok(error instanceof NexoraApiError);
      assert.equal(error.problem, null);
      assert.equal(error.traceId, "trace-safe-1");
      assert.doesNotMatch(error.message, /must not escape/);
      return true;
    },
  );
});

test("rejects sensitive-looking detail keys without exposing their values", async () => {
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test",
    fetch: async () => new Response(JSON.stringify({
      code: "internal_error",
      message: "An unexpected error occurred.",
      details: { stack: "sensitive nested diagnostic" },
      traceId: "trace-detail-1",
    }), { status: 500, headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-detail-1" } }),
  });

  await assert.rejects(
    client.getPlatform(),
    (error) => {
      assert.ok(error instanceof NexoraApiError);
      assert.equal(error.problem, null);
      assert.equal(error.traceId, "trace-detail-1");
      assert.doesNotMatch(error.message, /sensitive nested diagnostic/);
      return true;
    },
  );
});

test("rejects forbidden normalized detail-key segments", async () => {
  for (const [index, unsafeKey] of ["credential_value", "stack_trace_line", "provider.response", "CredentialValue"].entries()) {
    const traceId = `trace-segment-${index}`;
    const client = new PlatformApiClient({
      baseUrl: "https://nexora.test",
      fetch: async () => new Response(JSON.stringify({
        code: "validation_failed",
        message: "Request validation failed.",
        details: { [unsafeKey]: "invalid value" },
        traceId,
      }), { status: 400, headers: { "Content-Type": "application/json", "X-Trace-Id": traceId } }),
    });

    await assert.rejects(
      client.getPlatform(),
      (error) => {
        assert.ok(error instanceof NexoraApiError, unsafeKey);
        assert.equal(error.problem, null, unsafeKey);
        assert.equal(error.traceId, traceId, unsafeKey);
        return true;
      },
    );
  }
});

test("rejects a bearer credential hidden behind a benign detail key", async () => {
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test",
    fetch: async () => new Response(JSON.stringify({
      code: "validation_failed",
      message: "Request validation failed.",
      details: { message: "Authorization: Bearer redacted" },
      traceId: "trace-detail-2",
    }), { status: 400, headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-detail-2" } }),
  });

  await assert.rejects(
    client.echoPlatform({ message: "invalid" }),
    (error) => {
      assert.ok(error instanceof NexoraApiError);
      assert.equal(error.problem, null);
      assert.equal(error.traceId, "trace-detail-2");
      assert.doesNotMatch(error.message, /redacted/);
      return true;
    },
  );
});
