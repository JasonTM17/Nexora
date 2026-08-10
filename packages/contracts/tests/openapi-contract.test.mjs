import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { outputPath, renderClient, specPath } from "../scripts/generate-client.mjs";

const spec = JSON.parse(await readFile(specPath, "utf8"));
const methods = new Set(["get", "post", "put", "patch", "delete"]);

function resolve(value) {
  if (!value?.$ref) return value;
  return value.$ref
    .replace(/^#\//, "")
    .split("/")
    .reduce((current, part) => current?.[part], spec);
}

function operations() {
  return Object.entries(spec.paths).flatMap(([path, pathItem]) =>
    Object.entries(pathItem)
      .filter(([method]) => methods.has(method))
      .map(([method, operation]) => ({ path, method, operation })),
  );
}

test("publishes a versioned OpenAPI 3.1 authority under /api/v1", () => {
  assert.equal(spec.openapi, "3.1.0");
  assert.match(spec.info.version, /^1\.\d+\.\d+$/);
  assert.equal(spec["x-nexora-contract"].stability, "stable-v1");
  assert.ok(operations().length > 0);
  for (const { path } of operations()) assert.match(path, /^\/api\/v1(?:\/|$)/);
});

test("keeps operation identifiers unique and every response traceable", () => {
  const ids = operations().map(({ operation }) => operation.operationId);
  assert.equal(new Set(ids).size, ids.length);
  assert.ok(ids.every(Boolean));

  for (const { path, method, operation } of operations()) {
    for (const [status, declaredResponse] of Object.entries(operation.responses)) {
      const response = resolve(declaredResponse);
      assert.ok(response, `${method.toUpperCase()} ${path} ${status} must resolve`);
      assert.ok(response.headers?.["X-Trace-Id"], `${method.toUpperCase()} ${path} ${status} must return X-Trace-Id`);
    }
  }
});

test("freezes the safe error, authentication, authorization, and trace contracts", () => {
  const problem = spec.components.schemas.ApiProblem;
  assert.equal(problem.additionalProperties, false);
  assert.deepEqual(problem.required, ["code", "message", "details", "traceId"]);
  assert.deepEqual(Object.keys(problem.properties), ["code", "message", "details", "traceId"]);
  assert.equal(problem.properties.traceId.$ref, "#/components/schemas/TraceId");
  assert.equal(spec.components.schemas.TraceId.pattern, "^[A-Za-z0-9._-]{1,128}$");

  const bearer = spec.components.securitySchemes.bearerAuth;
  assert.deepEqual({ type: bearer.type, scheme: bearer.scheme }, { type: "http", scheme: "bearer" });
  for (const name of ["AuthenticationRequired", "PermissionDenied"]) {
    const response = spec.components.responses[name];
    assert.equal(response.content["application/json"].schema.$ref, "#/components/schemas/ApiProblem");
    assert.ok(response.headers["X-Trace-Id"]);
  }

  const serializedProblem = JSON.stringify(problem).toLowerCase();
  for (const forbidden of ["stack", "exception", "credential", "providerresponse", "requestsource"]) {
    assert.doesNotMatch(serializedProblem, new RegExp(`"${forbidden}"\\s*:`));
  }
});

test("has no generated-client drift", async () => {
  const generated = await readFile(outputPath, "utf8");
  assert.equal(generated, renderClient(spec));
});
