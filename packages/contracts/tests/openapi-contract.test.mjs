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
  const forbiddenDetailKeys = problem.properties.details.propertyNames.allOf[1].not.enum;
  for (const forbidden of ["stack", "exception", "credential", "password", "token", "provider_response"]) {
    assert.ok(forbiddenDetailKeys.includes(forbidden), `details must reject ${forbidden}`);
  }

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

test("requires bearer auth with 401/403 responses or an explicit public exception per operation", () => {
  assert.deepEqual(spec.security, [{ bearerAuth: [] }]);

  for (const { path, method, operation } of operations()) {
    const label = `${method.toUpperCase()} ${path}`;
    const effectiveSecurity = operation.security ?? spec.security;
    const bearerProtected = effectiveSecurity.some((requirement) =>
      Object.hasOwn(requirement, "bearerAuth"),
    );

    if (bearerProtected) {
      assert.equal(operation.responses["401"]?.$ref, "#/components/responses/AuthenticationRequired", `${label} must declare 401`);
      assert.equal(operation.responses["403"]?.$ref, "#/components/responses/PermissionDenied", `${label} must declare 403`);
      continue;
    }

    assert.deepEqual(operation.security, [], `${label} public access must be explicit`);
    assert.equal(operation["x-nexora-public"]?.access, "public", `${label} must identify the public exception`);
    assert.match(operation["x-nexora-public"]?.reason ?? "", /unauthenticated/i, `${label} must explain why it is public`);
  }
});

test("generates bearer handling when an operation inherits the protected default", () => {
  const protectedSpec = structuredClone(spec);
  const operation = protectedSpec.paths["/api/v1/platform"].get;
  delete operation.security;
  delete operation["x-nexora-public"];
  operation.responses["401"] = { "$ref": "#/components/responses/AuthenticationRequired" };
  operation.responses["403"] = { "$ref": "#/components/responses/PermissionDenied" };

  const generated = renderClient(protectedSpec);
  assert.match(
    generated,
    /getPlatform[\s\S]*?this\.request<PlatformResponse>\([\s\S]*?options, true\);/,
  );
  assert.match(generated, /if \(requiresAuth\)[\s\S]*?headers\.set\("Authorization"/);
});

test("has no generated-client drift", async () => {
  const generated = await readFile(outputPath, "utf8");
  assert.equal(generated, renderClient(spec));
});
