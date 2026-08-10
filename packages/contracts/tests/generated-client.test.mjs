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

test("surfaces only a valid safe problem envelope", async () => {
  const problem = {
    code: "authentication_required",
    message: "Authentication is required.",
    details: {},
    traceId: "trace-auth-1",
  };
  const client = new PlatformApiClient({
    baseUrl: "https://nexora.test",
    fetch: async () => new Response(JSON.stringify(problem), {
      status: 401,
      headers: { "Content-Type": "application/json", "X-Trace-Id": "trace-header-1" },
    }),
  });

  await assert.rejects(
    client.getPlatform(),
    (error) => {
      assert.ok(error instanceof NexoraApiError);
      assert.equal(error.status, 401);
      assert.deepEqual(error.problem, problem);
      assert.equal(error.traceId, "trace-auth-1");
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
