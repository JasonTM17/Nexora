import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import test from "node:test";

const contractPath = fileURLToPath(new URL("../domain/v1/identity-tenant-permission.json", import.meta.url));
const fixturePath = fileURLToPath(new URL("../fixtures/v1/two-tenant-access.json", import.meta.url));

const contract = JSON.parse(await readFile(contractPath, "utf8"));
const fixture = JSON.parse(await readFile(fixturePath, "utf8"));

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function activeMemberships(subjectId) {
  return fixture.memberships.filter((membership) => membership.subjectId === subjectId && membership.status === "ACTIVE");
}

function resolveTenant(testCase) {
  if (!testCase.authenticated || testCase.anonymous || !testCase.subjectId) return "AUTHENTICATION_REQUIRED";
  const active = activeMemberships(testCase.subjectId);
  if (testCase.selectedOrganizationId) {
    return active.some((membership) => membership.organizationId === testCase.selectedOrganizationId)
      ? "RESOLVED"
      : "PERMISSION_DENIED";
  }
  if (active.length === 1) return "RESOLVED";
  if (active.length > 1) return "TENANT_SELECTION_REQUIRED";
  return "MEMBERSHIP_REQUIRED";
}

function evaluatePermission(testCase) {
  if (testCase.resolvedOrganizationId !== testCase.resourceOrganizationId) return "DENY";
  const membership = activeMemberships(testCase.subjectId)
    .find((candidate) => candidate.organizationId === testCase.resolvedOrganizationId);
  if (!membership) return "DENY";
  return contract.permissionMatrix[membership.role]?.includes(testCase.permission) ? "ALLOW" : "DENY";
}

test("validates the canonical contract and fixture shape without dependencies", () => {
  assert.equal(fixture.contractVersion, contract.contractVersion);
  assert.equal(fixture.fixtureVersion, "1.0.0");
  for (const collection of ["organizations", "subjects", "memberships", "tenantResolutionCases", "permissionCases"]) {
    assert.ok(Array.isArray(fixture[collection]) && fixture[collection].length > 0, `${collection} must be populated`);
  }
  for (const organization of fixture.organizations) {
    assert.match(organization.id, UUID_PATTERN);
    assert.match(organization.slug, /^[a-z][a-z0-9-]{2,62}$/);
    assert.equal(organization.status, "ACTIVE");
    assert.ok(Number.isInteger(organization.version) && organization.version > 0);
  }
  for (const membership of fixture.memberships) {
    for (const id of [membership.id, membership.organizationId, membership.subjectId]) assert.match(id, UUID_PATTERN);
    assert.ok(contract.membership.statuses.includes(membership.status));
    assert.ok(contract.roles.tenant.includes(membership.role));
    assert.ok(Number.isInteger(membership.version) && membership.version > 0);
  }
});

test("freezes distinct platform and tenant roles with a complete deny-by-default permission matrix", () => {
  assert.deepEqual(contract.roles.platform.map(({ role }) => role), ["SUPER_ADMIN"]);
  assert.deepEqual(contract.roles.platform[0].tenantPermissions, []);
  assert.equal(contract.roles.platform[0].tenantAssignable, false);
  assert.equal(contract.roles.viewerEquivalent, "USER");

  const roles = contract.roles.tenant;
  assert.deepEqual(Object.keys(contract.permissionMatrix), roles);
  assert.equal(new Set(roles).size, roles.length);
  assert.equal(new Set(contract.permissions).size, contract.permissions.length);
  for (const [role, permissions] of Object.entries(contract.permissionMatrix)) {
    assert.equal(new Set(permissions).size, permissions.length, `${role} permissions must be unique`);
    for (const permission of permissions) assert.ok(contract.permissions.includes(permission), `${role} uses declared permissions only`);
  }
  assert.deepEqual(new Set(contract.permissionMatrix.OWNER), new Set(contract.permissions));
  assert.ok(contract.permissionMatrix.USER.every((permission) => contract.permissionMatrix.OWNER.includes(permission)));
  assert.equal(contract.membership.customRoles, "not-supported-in-v1");
  assert.match(contract.membership.ownerInvariant, /last owner cannot/i);
});

test("keeps membership and organization fixtures referentially sound with an active owner per tenant", () => {
  assert.equal(fixture.organizations.length, 2, "the canonical security fixture must contain exactly two tenants");
  const organizationIds = new Set(fixture.organizations.map(({ id }) => id));
  const subjectIds = new Set(fixture.subjects.map(({ id }) => id));
  const membershipKeys = new Set();

  for (const membership of fixture.memberships) {
    assert.ok(organizationIds.has(membership.organizationId));
    assert.ok(subjectIds.has(membership.subjectId));
    const key = `${membership.organizationId}:${membership.subjectId}`;
    assert.ok(!membershipKeys.has(key), `duplicate membership authority ${key}`);
    membershipKeys.add(key);
  }

  for (const organizationId of organizationIds) {
    assert.ok(
      fixture.memberships.some((membership) =>
        membership.organizationId === organizationId && membership.status === "ACTIVE" && membership.role === "OWNER"),
      `${organizationId} requires an active owner`,
    );
  }
});

test("executes every tenant-resolution fixture including selection, removed-member, and no-membership denials", () => {
  for (const testCase of fixture.tenantResolutionCases) {
    assert.equal(resolveTenant(testCase), testCase.expected, testCase.id);
  }
  for (const requiredCase of [
    "multiple-memberships-require-selection",
    "forged-cross-tenant-selection-denied",
    "removed-membership-denied",
  ]) {
    assert.ok(fixture.tenantResolutionCases.some(({ id }) => id === requiredCase));
  }
});

test("executes the tenant and permission matrix with negative cross-tenant cases", () => {
  for (const testCase of fixture.permissionCases) {
    assert.equal(evaluatePermission(testCase), testCase.expected, testCase.id);
  }
  const crossTenantCases = fixture.permissionCases.filter((testCase) =>
    testCase.resolvedOrganizationId !== testCase.resourceOrganizationId);
  assert.ok(crossTenantCases.length >= 2);
  assert.ok(crossTenantCases.every(({ expected }) => expected === "DENY"));
});

test("freezes the non-owner forced-RLS and transaction-local pool contract", () => {
  assert.match(contract.database.roles.runtime, /non-owner/i);
  assert.match(contract.database.roles.runtime, /no BYPASSRLS/i);
  assert.equal(contract.database.rls.mode, "enabled-and-forced-on-every-tenant-owned-relation");
  assert.equal(contract.database.rls.default, "deny");
  assert.equal(contract.database.transactionContext.scope, "transaction-local-only");
  assert.deepEqual(contract.database.transactionContext.settings, [
    "nexora.subject_id",
    "nexora.organization_id",
    "nexora.membership_id",
  ]);
  assert.match(contract.database.transactionContext.sequence.at(-1), /no retained settings/i);
});

test("freezes a compact API handoff with unique operations and explicit tenant selection", () => {
  const ids = contract.api.operations.map(({ operationId }) => operationId);
  assert.equal(new Set(ids).size, ids.length);
  for (const path of [
    "/api/v1/identity/access-context",
    "/api/v1/tenant-context/resolve",
    "/api/v1/tenant-context",
    "/api/v1/authorization/permission-matrix",
  ]) assert.ok(contract.api.operations.some((operation) => operation.path === path), `${path} must be frozen`);
  assert.equal(contract.api.organizationSelectionHeader, contract.tenantResolution.selectionHeader);
});

test("records bounded downstream ownership and no runtime or provider proof", () => {
  assert.deepEqual(Object.keys(contract.handoffs), ["M2-DB01", "M2-T01", "M2-T02", "M2-U01"]);
  assert.ok(contract.limitations.some((limitation) => /no migration, runtime enforcement, UI, hosted configuration, provider call/i.test(limitation)));
  assert.ok(contract.limitations.some((limitation) => /not PostgreSQL RLS/i.test(limitation)));
});

test("keeps canonical fixtures synthetic and free of credential-shaped or direct-contact fields", () => {
  const serialized = JSON.stringify(fixture);
  assert.doesNotMatch(serialized, /(?:password|secret|credential|accessToken|refreshToken|apiKey|authorization|cookie|provider)/i);
  assert.doesNotMatch(serialized, /(?:email|phone|@)/i);
});
