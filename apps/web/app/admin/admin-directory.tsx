"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { AccessContextResponse, MembershipDirectoryEntry, MembershipMutationResponse, TenantRole } from "../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";

type Mode = "users" | "roles";
type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error" | "version-conflict";

const roles: ReadonlyArray<TenantRole> = ["OWNER", "ADMIN", "EDITOR", "REVIEWER", "USER"];

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not complete that member change." };
}

export function AdminDirectory({ mode }: { mode: Mode }) {
  const [context, setContext] = useState<AccessContextResponse | null>(null);
  const [organizationId, setOrganizationId] = useState("");
  const [memberships, setMemberships] = useState<ReadonlyArray<MembershipDirectoryEntry>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [saving, setSaving] = useState<string | null>(null);
  const [removing, setRemoving] = useState<MembershipDirectoryEntry | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = asProblem(error);
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : next.code === "VERSION_CONFLICT" ? "version-conflict" : "error");
  }, []);

  const load = useCallback(async () => {
    setState("loading"); setProblem(null);
    try {
      const access = await readJson<AccessContextResponse>("/api/bff/access-context");
      setContext(access);
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) { setMemberships([]); setState("empty"); return; }
      const directory = await readJson<ReadonlyArray<MembershipDirectoryEntry>>(`/api/bff/memberships?organizationId=${encodeURIComponent(selected)}`);
      setMemberships(directory); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  async function changeOrganization(next: string) {
    setOrganizationId(next); setState("loading"); setProblem(null);
    try {
      const directory = await readJson<ReadonlyArray<MembershipDirectoryEntry>>(`/api/bff/memberships?organizationId=${encodeURIComponent(next)}`);
      setMemberships(directory); setState("ready");
    } catch (error) { setFailure(error); }
  }

  async function update(member: MembershipDirectoryEntry, change: { role: TenantRole } | { status: "REMOVED" }) {
    setSaving(member.membershipId); setProblem(null);
    try {
      const updated = await readJson<MembershipMutationResponse>(`/api/bff/memberships/${encodeURIComponent(member.membershipId)}`, {
        method: "PATCH", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ organizationId, expectedVersion: member.version, ...change }),
      });
      setMemberships((current) => current.map((item) => item.membershipId === updated.membershipId ? updated : item));
      setRemoving(null); setState("ready");
    } catch (error) { setFailure(error); }
    finally { setSaving(null); }
  }

  const title = mode === "users" ? "Manage users" : "Manage roles";
  const lead = mode === "users" ? "Review and remove tenant memberships. The server independently authorizes each change." : "Assign a membership role. The browser does not grant permissions; the server validates every requested role.";
  const selectedMembership = context?.memberships.find((item) => item.organizationId === organizationId);

  return <AppShell><PageGrid><header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label="Administration"><Link aria-current={mode === "users" ? "page" : undefined} href="/admin/users">Users</Link><Link aria-current={mode === "roles" ? "page" : undefined} href="/admin/roles">Roles</Link><Link href="/account">Account</Link></nav></header><main id="main-content" className="nx-admin-page">
    <p className="nx-eyebrow">Tenant administration</p><h1 ref={heading} tabIndex={-1}>{state === "denied" ? "Access denied" : state === "version-conflict" ? "Member changed elsewhere" : title}</h1><p className="nx-lede" aria-live="polite">{state === "denied" ? "You do not have current permission to manage memberships in this organization." : state === "version-conflict" ? "The member record is stale. Reload the directory before attempting another change." : lead}</p>
    {context && context.memberships.length > 1 && <label className="nx-admin-organization" htmlFor="admin-organization">Organization<select id="admin-organization" value={organizationId} disabled={state === "loading" || saving !== null} onChange={(event) => void changeOrganization(event.target.value)}>{context.memberships.map((item) => <option key={item.membershipId} value={item.organizationId}>Organization {item.organizationId.slice(0, 8)} ({item.role})</option>)}</select></label>}
    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> Loading the server-authorized membership directory…</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>Choose an organization</h2><p>Choose an active organization in your account before accessing its administration directory.</p><Link className="nx-action-button nx-action-button--secondary" href="/account">Go to account</Link></section>}
    {(state === "denied" || state === "error" || state === "version-conflict") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p>{problem?.traceId && <p className="nx-field-help">Reference: {problem.traceId}</p>}<ActionButton tone="secondary" onClick={() => void load()}>{state === "version-conflict" ? "Reload directory" : "Retry"}</ActionButton></section>}
    {state === "ready" && <section className="nx-access-card nx-admin-directory" aria-labelledby="directory-title"><div className="nx-card-heading"><div><h2 id="directory-title">{title}</h2><p className="nx-field-help">Selected organization · your current role: {selectedMembership?.role ?? "server-confirmed"}</p></div><StatusLabel kind="fixture" /></div>{memberships.length === 0 ? <p className="nx-empty-copy">No memberships are available in this organization.</p> : <div className="nx-table-scroll"><table><caption className="nx-visually-hidden">Membership directory</caption><thead><tr><th scope="col">Subject</th><th scope="col">Status</th><th scope="col">Role</th><th scope="col"><span className="nx-visually-hidden">Action</span></th></tr></thead><tbody>{memberships.map((member) => <tr key={member.membershipId}><td><code>{member.subjectId}</code><small>Version {member.version}</small></td><td>{member.status}</td><td>{mode === "roles" ? <select aria-label={`Role for ${member.subjectId}`} defaultValue={member.role} disabled={saving !== null} onChange={(event) => { if (event.target.value !== member.role) void update(member, { role: event.target.value as TenantRole }); }}>{roles.map((role) => <option key={role} value={role}>{role}</option>)}</select> : member.role}</td><td>{mode === "users" && <ActionButton tone="secondary" disabled={saving !== null || member.status === "REMOVED"} onClick={() => setRemoving(member)}>{saving === member.membershipId ? "Saving…" : member.status === "REMOVED" ? "Removed" : "Remove"}</ActionButton>}</td></tr>)}</tbody></table></div>}</section>}
    {removing && <div className="nx-dialog-backdrop" role="presentation"><section className="nx-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="remove-title"><h2 id="remove-title">Remove this member?</h2><p>This permanently removes the active membership for <code>{removing.subjectId}</code>. The server confirms both the current tenant and your permission before applying it.</p><div className="nx-hero-actions"><ActionButton tone="secondary" disabled={saving !== null} onClick={() => setRemoving(null)}>Cancel</ActionButton><ActionButton disabled={saving !== null} onClick={() => void update(removing, { status: "REMOVED" })}>{saving === removing.membershipId ? "Removing…" : "Remove member"}</ActionButton></div></section></div>}
  </main></PageGrid></AppShell>;
}
