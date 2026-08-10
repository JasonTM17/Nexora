import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";

const packageRoot = fileURLToPath(new URL("../", import.meta.url));
export const specPath = fileURLToPath(new URL("../openapi/v1/openapi.json", import.meta.url));
export const outputPath = fileURLToPath(new URL("../src/generated/platform-api.ts", import.meta.url));

const HTTP_METHODS = new Set(["get", "post", "put", "patch", "delete"]);

function refName(ref) {
  return ref.slice(ref.lastIndexOf("/") + 1);
}

function typeFor(schema) {
  if (!schema) return "unknown";
  if (schema.$ref) return refName(schema.$ref);
  if (schema.enum) return schema.enum.map((value) => JSON.stringify(value)).join(" | ");
  if (schema.oneOf) return schema.oneOf.map(typeFor).join(" | ");
  if (schema.type === "array") return `ReadonlyArray<${typeFor(schema.items)}>`;
  if (schema.type === "integer" || schema.type === "number") return "number";
  if (schema.type === "boolean") return "boolean";
  if (schema.type === "object") {
    const required = new Set(schema.required ?? []);
    const properties = Object.entries(schema.properties ?? {}).map(
      ([name, property]) => `readonly ${JSON.stringify(name)}${required.has(name) ? "" : "?"}: ${typeFor(property)};`,
    );
    if (schema.additionalProperties && typeof schema.additionalProperties === "object") {
      properties.push(`readonly [key: string]: ${typeFor(schema.additionalProperties)};`);
    }
    return `{ ${properties.join(" ")} }`;
  }
  if (schema.type === "string") return "string";
  return "unknown";
}

function renderSchemas(spec) {
  return Object.entries(spec.components?.schemas ?? {})
    .map(([name, schema]) => `export type ${name} = ${typeFor(schema)};`)
    .join("\n\n");
}

function jsonSchema(response) {
  return response?.content?.["application/json"]?.schema;
}

function resolveLocalRef(spec, value) {
  if (!value?.$ref) return value;
  const parts = value.$ref.replace(/^#\//, "").split("/");
  return parts.reduce((current, part) => current?.[part], spec);
}

function typeName(operationId, suffix) {
  return `${operationId.charAt(0).toUpperCase()}${operationId.slice(1)}${suffix}`;
}

function headerParametersFor(operation) {
  return (operation.parameters ?? [])
    .filter((parameter) => parameter?.in === "header")
    .map((parameter) => {
      if (typeof parameter.name !== "string" || !parameter.name) {
        throw new Error("Header parameters must have a name");
      }
      if (!parameter.schema) throw new Error(`Header parameter ${parameter.name} has no schema`);
      return {
        name: parameter.name,
        propertyName: parameter["x-nexora-client-name"] ?? parameter.name,
        required: parameter.required === true,
        type: typeFor(parameter.schema),
      };
    });
}

function operationsFor(spec) {
  const operations = [];
  for (const [path, pathItem] of Object.entries(spec.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem)) {
      if (!HTTP_METHODS.has(method)) continue;
      if (!operation.operationId) throw new Error(`${method.toUpperCase()} ${path} has no operationId`);
      if (path.includes("{")) throw new Error(`Path parameters are not supported yet: ${path}`);

      const requestSchema = operation.requestBody?.content?.["application/json"]?.schema;
      const successEntry = Object.entries(operation.responses ?? {}).find(([status]) => /^2\d\d$/.test(status));
      if (!successEntry) throw new Error(`${operation.operationId} has no explicit 2xx response`);
      const success = resolveLocalRef(spec, successEntry[1]);
      const responseSchema = jsonSchema(success);
      if (!responseSchema) throw new Error(`${operation.operationId} has no application/json success schema`);
      const effectiveSecurity = operation.security ?? spec.security ?? [];

      operations.push({
        operationId: operation.operationId,
        method: method.toUpperCase(),
        path,
        requestType: requestSchema ? typeFor(requestSchema) : null,
        responseType: typeFor(responseSchema),
        requiresAuth: effectiveSecurity.some((requirement) => Object.hasOwn(requirement, "bearerAuth")),
        headerParameters: headerParametersFor(operation),
      });
    }
  }
  return operations;
}

function renderOperation(operation) {
  const bodyParameter = operation.requestType ? `body: ${operation.requestType}, ` : "";
  const bodyOption = operation.requestType ? ", body" : "";
  const headerType = typeName(operation.operationId, "Headers");
  const headerParameter = operation.headerParameters.length
    ? `headers: ${headerType}${operation.headerParameters.some((parameter) => parameter.required) ? "" : " = {}"}, `
    : "";
  const headerOption = operation.headerParameters.length
    ? `, headers: { ${operation.headerParameters.map((parameter) => `${JSON.stringify(parameter.name)}: headers.${parameter.propertyName}`).join(", ")} }`
    : "";
  return `  async ${operation.operationId}(${bodyParameter}${headerParameter}options: RequestOptions = {}): Promise<ApiResponse<${operation.responseType}>> {
    return this.request<${operation.responseType}>(${JSON.stringify(operation.path)}, { method: ${JSON.stringify(operation.method)}${bodyOption}${headerOption} }, options, ${operation.requiresAuth});
  }`;
}

function renderHeaderTypes(spec) {
  return operationsFor(spec)
    .filter((operation) => operation.headerParameters.length > 0)
    .map((operation) => `export interface ${typeName(operation.operationId, "Headers")} {\n${operation.headerParameters
      .map((parameter) => `  readonly ${JSON.stringify(parameter.propertyName)}${parameter.required ? "" : "?"}: ${parameter.type};`)
      .join("\n")}\n}`)
    .join("\n\n");
}

export function renderClient(spec) {
  const schemas = renderSchemas(spec);
  const headerTypes = renderHeaderTypes(spec);
  const operations = operationsFor(spec).map(renderOperation).join("\n\n");
  const detailPropertyRules = spec.components?.schemas?.ApiProblem?.properties?.details?.propertyNames?.allOf ?? [];
  const forbiddenDetailKeys = detailPropertyRules.find((rule) => Array.isArray(rule.not?.enum))?.not.enum;
  if (!forbiddenDetailKeys) throw new Error("ApiProblem details must define forbidden property names");
  const forbiddenDetailSegments = spec.components?.schemas?.ApiProblem?.properties?.details?.propertyNames?.["x-nexora-forbidden-segments"];
  if (!Array.isArray(forbiddenDetailSegments) || forbiddenDetailSegments.length === 0) {
    throw new Error("ApiProblem details must define forbidden normalized key segments");
  }
  const detailValueSchema = spec.components?.schemas?.ApiProblem?.properties?.details?.additionalProperties;
  const problemCodePattern = spec.components?.schemas?.ApiProblem?.properties?.code?.pattern;
  const detailValueMaxLength = detailValueSchema?.maxLength;
  const safeDetailValuePattern = detailValueSchema?.pattern;
  const forbiddenDetailValuePattern = detailValueSchema?.not?.pattern;
  if (!detailValueMaxLength || !safeDetailValuePattern || !forbiddenDetailValuePattern) {
    throw new Error("ApiProblem details must define bounded safe value rules");
  }
  if (!problemCodePattern) throw new Error("ApiProblem code must define a pattern");
  const hasBearerAuth = Object.values(spec.components?.securitySchemes ?? {}).some(
    (scheme) => scheme.type === "http" && scheme.scheme === "bearer",
  );
  if (!hasBearerAuth) throw new Error("The contract must define an HTTP bearer security scheme");

  return `// Generated from openapi/v1/openapi.json by scripts/generate-client.mjs.
// Do not edit this file directly.

export const API_CONTRACT_VERSION = ${JSON.stringify(spec.info.version)} as const;

${schemas}

${headerTypes}

export interface ApiResponse<T> {
  readonly data: T;
  readonly traceId: string | null;
}

export interface RequestOptions {
  readonly traceId?: string;
  readonly signal?: AbortSignal;
}

export interface PlatformApiClientOptions {
  readonly baseUrl?: string;
  readonly accessToken?: string | (() => string | null | undefined | Promise<string | null | undefined>);
  readonly fetch?: typeof globalThis.fetch;
}

export class NexoraApiError extends Error {
  readonly status: number;
  readonly problem: ApiProblem | null;
  readonly traceId: string | null;

  constructor(status: number, problem: ApiProblem | null, traceId: string | null) {
    super(problem?.message ?? \`Nexora API request failed with status \${status}.\`);
    this.name = "NexoraApiError";
    this.status = status;
    this.problem = problem;
    this.traceId = problem?.traceId ?? traceId;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

const FORBIDDEN_DETAIL_KEYS = new Set(${JSON.stringify(forbiddenDetailKeys)});
const FORBIDDEN_DETAIL_KEY_SEGMENTS = new Set(${JSON.stringify(forbiddenDetailSegments)});
const API_PROBLEM_CODE_PATTERN = new RegExp(${JSON.stringify(problemCodePattern)});
const SAFE_DETAIL_VALUE_PATTERN = new RegExp(${JSON.stringify(safeDetailValuePattern)});
const FORBIDDEN_DETAIL_VALUE_PATTERN = new RegExp(${JSON.stringify(forbiddenDetailValuePattern)});

function isSafeDetailKey(key: string): boolean {
  if (!/^[A-Za-z][A-Za-z0-9_.-]{0,63}$/.test(key)) return false;
  const segments = key
    .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1_$2")
    .toLowerCase()
    .split(/[._-]+/)
    .filter(Boolean);
  return !FORBIDDEN_DETAIL_KEYS.has(segments.join("_"))
    && !segments.some((segment) => FORBIDDEN_DETAIL_KEY_SEGMENTS.has(segment));
}

function asProblem(value: unknown): ApiProblem | null {
  if (!isRecord(value) || typeof value.code !== "string" || typeof value.message !== "string") return null;
  if (!isRecord(value.details) || typeof value.traceId !== "string") return null;
  const keys = Object.keys(value);
  if (keys.length !== 4 || !keys.every((key) => ["code", "message", "details", "traceId"].includes(key))) return null;
  if (!API_PROBLEM_CODE_PATTERN.test(value.code) || value.message.length < 1 || value.message.length > 512) return null;
  if (!/^[A-Za-z0-9._-]{1,128}$/.test(value.traceId)) return null;
  const details = Object.entries(value.details);
  if (details.length > 50 || !details.every(([key, detail]) =>
    isSafeDetailKey(key)
      && typeof detail === "string"
      && detail.length <= ${detailValueMaxLength}
      && SAFE_DETAIL_VALUE_PATTERN.test(detail)
      && !FORBIDDEN_DETAIL_VALUE_PATTERN.test(detail)
  )) return null;
  return value as ApiProblem;
}

export class PlatformApiClient {
  private readonly baseUrl: string;
  private readonly accessToken: PlatformApiClientOptions["accessToken"];
  private readonly fetchImplementation: typeof globalThis.fetch;

  constructor(options: PlatformApiClientOptions = {}) {
    this.baseUrl = (options.baseUrl ?? "").replace(/\\/$/, "");
    this.accessToken = options.accessToken;
    this.fetchImplementation = options.fetch ?? globalThis.fetch;
    if (!this.fetchImplementation) throw new Error("A Fetch API implementation is required.");
  }

${operations}

  private async request<T>(
    path: string,
    request: { readonly method: string; readonly body?: unknown; readonly headers?: Readonly<Record<string, string | undefined>> },
    options: RequestOptions,
    requiresAuth: boolean,
  ): Promise<ApiResponse<T>> {
    const headers = new Headers({ Accept: "application/json" });
    for (const [name, value] of Object.entries(request.headers ?? {})) {
      if (value !== undefined) headers.set(name, value);
    }
    if (request.body !== undefined) headers.set("Content-Type", "application/json");
    if (options.traceId) headers.set("X-Trace-Id", options.traceId);

    if (requiresAuth) {
      const token = typeof this.accessToken === "function" ? await this.accessToken() : this.accessToken;
      if (token) headers.set("Authorization", \`Bearer \${token}\`);
    }

    const init: RequestInit = { method: request.method, headers };
    if (request.body !== undefined) init.body = JSON.stringify(request.body);
    if (options.signal) init.signal = options.signal;
    const response = await this.fetchImplementation(\`\${this.baseUrl}\${path}\`, init);
    const traceId = response.headers.get("X-Trace-Id");
    const payload: unknown = await response.json();
    if (!response.ok) throw new NexoraApiError(response.status, asProblem(payload), traceId);
    return { data: payload as T, traceId };
  }
}
`;
}

export async function generateClient() {
  const spec = JSON.parse(await readFile(specPath, "utf8"));
  const output = renderClient(spec);
  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, output, "utf8");
  return output;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  await generateClient();
  process.stdout.write(`Generated ${outputPath.slice(packageRoot.length)}\n`);
}
